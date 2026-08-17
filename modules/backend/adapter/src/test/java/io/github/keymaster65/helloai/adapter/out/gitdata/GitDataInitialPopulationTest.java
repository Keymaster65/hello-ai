package io.github.keymaster65.helloai.adapter.out.gitdata;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/**
 * The initial population against a real repository (ADR 0055).
 *
 * <p>Two rules are worth a test each, because both are easy to break without noticing: that an
 * <em>unwritten</em> store gets the six recipes, and that an existing branch is left alone &ndash;
 * even an empty one.
 */
class GitDataInitialPopulationTest {

    private static final String BRANCH = "data";

    @TempDir
    private Path directory;

    private Git git;
    private GitDataRecipeRepository store;

    @BeforeEach
    void openStore() throws GitAPIException {
        git = Git.init().setDirectory(directory.toFile()).setInitialBranch("main").call();
        store = new GitDataRecipeRepository(
                git.getRepository(),
                BRANCH,
                Clock.fixed(Instant.parse("2026-08-17T12:00:00Z"), ZoneOffset.UTC),
                JsonMapper.builder().build());
    }

    @AfterEach
    void closeRepository() {
        git.close();
    }

    @Test
    void shouldWriteTheSeededRecipesIntoAnUnwrittenStore() {
        int written = new GitDataInitialPopulation(store).populateIfUnwritten();

        assertThat(written).isEqualTo(6);
        assertThat(store.findAll())
                .extracting(Recipe::title)
                .containsExactlyElementsOf(SeedChangelog.recipes().stream().map(Recipe::title).toList());
    }

    @Test
    void shouldWriteOneCommitPerRecipe() throws IOException {
        new GitDataInitialPopulation(store).populateIfUnwritten();

        assertThat(commitsOnBranch()).hasSize(6);
    }

    @Test
    void shouldGiveTheSeededRecipesTheIdentifiersOneToSix() {
        new GitDataInitialPopulation(store).populateIfUnwritten();

        assertThat(store.findAll()).extracting(Recipe::id).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    void shouldLeaveAnExistingBranchAloneEvenWhenItIsEmpty() throws IOException {
        // A store that was written to and emptied again: the branch exists, the rows are gone.
        Recipe saved = store.save(Recipe.curried()
                .id(null)
                .title("Von Hand")
                .description(null)
                .servings(null)
                .prepTimeMinutes(null)
                .difficulty(Difficulty.EASY)
                .ingredients(List.of())
                .steps(List.of()));
        store.deleteById(saved.id());
        int commitsBefore = commitsOnBranch().size();

        int written = new GitDataInitialPopulation(store).populateIfUnwritten();

        assertThat(written).isZero();
        assertThat(store.findAll()).isEmpty();
        assertThat(commitsOnBranch()).hasSize(commitsBefore);
    }

    @Test
    void shouldNotPopulateTwice() {
        GitDataInitialPopulation population = new GitDataInitialPopulation(store);
        population.populateIfUnwritten();

        assertThat(population.populateIfUnwritten()).isZero();
        assertThat(store.findAll()).hasSize(6);
    }

    private List<RevCommit> commitsOnBranch() throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            walk.markStart(walk.parseCommit(git.getRepository().resolve("refs/heads/" + BRANCH)));
            return java.util.stream.StreamSupport.stream(walk.spliterator(), false).toList();
        }
    }
}
