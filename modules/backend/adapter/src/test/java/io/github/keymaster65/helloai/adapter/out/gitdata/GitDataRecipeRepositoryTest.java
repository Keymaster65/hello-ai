package io.github.keymaster65.helloai.adapter.out.gitdata;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.keymaster65.helloai.domain.model.Difficulty;
import io.github.keymaster65.helloai.domain.model.Ingredient;
import io.github.keymaster65.helloai.domain.model.PreparationStep;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests the git-backed repository against a real repository in a temporary directory (ADR 0053).
 *
 * <p>No mock stands in for git here: what is worth testing about this adapter is exactly what JGit
 * does with it &ndash; which files end up in the tree, how many commits it takes and that the
 * working tree stays untouched. A mocked {@code Repository} would confirm the calls and prove
 * nothing about the result.
 */
class GitDataRecipeRepositoryTest {

    private static final String BRANCH = "data";

    @TempDir
    private Path directory;

    private Git git;
    private Repository repository;
    private GitDataRecipeRepository repositoryUnderTest;

    @BeforeEach
    void initRepository() throws GitAPIException {
        git = Git.init().setDirectory(directory.toFile()).setInitialBranch("main").call();
        repository = git.getRepository();
        repositoryUnderTest = new GitDataRecipeRepository(
                repository,
                BRANCH,
                // Fixed clock: the commits of a test should not differ from run to run.
                Clock.fixed(Instant.parse("2026-08-17T10:15:30Z"), ZoneOffset.UTC),
                JsonMapper.builder().build());
    }

    @AfterEach
    void closeRepository() {
        git.close();
    }

    @Test
    void findAllIsEmptyWhileTheBranchDoesNotExist() {
        assertThat(repositoryUnderTest.findAll()).isEmpty();
        assertThat(repositoryUnderTest.findById(1L)).isEmpty();
        assertThat(branchExists()).isFalse();
    }

    @Test
    void saveAssignsIdentifiersAndReturnsThePersistedRecipe() {
        Recipe saved = repositoryUnderTest.save(fastingSoup());

        assertThat(saved.id()).isEqualTo(1L);
        assertThat(saved.title()).isEqualTo("Fastensuppe");
        assertThat(saved.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(saved.ingredients()).containsExactly(
                new Ingredient("Karotten", BigDecimal.valueOf(150), "g"),
                new Ingredient("Fenchel", null, null));
        assertThat(saved.steps()).containsExactly(
                new PreparationStep(1, "Gemüse putzen"),
                new PreparationStep(2, "Passieren"));
    }

    @Test
    void saveWritesOneFilePerRowIntoTheEntityDirectories() throws IOException {
        repositoryUnderTest.save(fastingSoup());

        assertThat(pathsOnBranch()).containsExactly(
                "database/entities/ingredients/1.json",
                "database/entities/ingredients/2.json",
                "database/entities/preparation_steps/1.json",
                "database/entities/preparation_steps/2.json",
                "database/entities/recipes/1.json");
    }

    @Test
    void everyWriteIsExactlyOneCommitAndTheFirstOneHasNoParent() throws IOException {
        repositoryUnderTest.save(fastingSoup());
        repositoryUnderTest.save(fastingTea());
        repositoryUnderTest.deleteById(1L);

        List<RevCommit> commits = commitsOnBranch();
        assertThat(commits).extracting(RevCommit::getFullMessage)
                .containsExactly("delete recipe 1", "insert recipe 2", "insert recipe 1");
        assertThat(commits.getLast().getParentCount())
                .as("die Wurzel des Branches data hängt nicht an der Historie des Codes")
                .isZero();
        assertThat(commits.getFirst().getParentCount()).isEqualTo(1);
    }

    @Test
    void theWorkingTreeStaysUntouched() throws IOException {
        repositoryUnderTest.save(fastingSoup());

        assertThat(directory.resolve("database")).doesNotExist();
        assertThat(repository.getBranch())
                .as("der ausgecheckte Branch bleibt, wo er war")
                .isEqualTo("main");
    }

    @Test
    void findAllReturnsTheRecipesOrderedByIdentifier() {
        repositoryUnderTest.save(fastingSoup());
        repositoryUnderTest.save(fastingTea());

        assertThat(repositoryUnderTest.findAll())
                .extracting(Recipe::id, Recipe::title)
                .containsExactly(Tuple.tuple(1L, "Fastensuppe"), Tuple.tuple(2L, "Fastentee"));
    }

    @Test
    void findByIdReadsTheChildrenOfThatRecipeOnly() {
        repositoryUnderTest.save(fastingSoup());
        repositoryUnderTest.save(fastingTea());

        Recipe tea = repositoryUnderTest.findById(2L).orElseThrow();

        assertThat(tea.ingredients()).extracting(Ingredient::name).containsExactly("Fenchelsamen");
        assertThat(tea.steps()).extracting(PreparationStep::instruction).containsExactly("Aufgießen");
    }

    @Test
    void updateReplacesTheChildrenAndKeepsTheIdentifier() throws IOException {
        repositoryUnderTest.save(fastingSoup());

        Optional<Recipe> updated = repositoryUnderTest.update(1L, fastingTea());

        assertThat(updated).isPresent();
        assertThat(updated.orElseThrow().id()).isEqualTo(1L);
        assertThat(updated.orElseThrow().title()).isEqualTo("Fastentee");
        assertThat(updated.orElseThrow().ingredients()).hasSize(1);
        assertThat(pathsOnBranch())
                .as("die alten Zeilen des Rezepts sind fort, nicht überzählig")
                .containsExactly(
                        "database/entities/ingredients/3.json",
                        "database/entities/preparation_steps/3.json",
                        "database/entities/recipes/1.json");
    }

    @Test
    void updateOfAnUnknownRecipeChangesNothing() throws IOException {
        repositoryUnderTest.save(fastingSoup());

        assertThat(repositoryUnderTest.update(42L, fastingTea())).isEmpty();
        assertThat(commitsOnBranch()).hasSize(1);
    }

    @Test
    void deleteRemovesTheRecipeWithItsChildren() throws IOException {
        repositoryUnderTest.save(fastingSoup());
        repositoryUnderTest.save(fastingTea());

        assertThat(repositoryUnderTest.deleteById(1L)).isTrue();

        assertThat(repositoryUnderTest.findById(1L)).isEmpty();
        assertThat(pathsOnBranch()).allSatisfy(path ->
                assertThat(path).doesNotContain("recipes/1.json"));
        assertThat(repositoryUnderTest.findAll()).extracting(Recipe::id).containsExactly(2L);
    }

    @Test
    void deleteOfAnUnknownRecipeChangesNothing() throws IOException {
        repositoryUnderTest.save(fastingSoup());

        assertThat(repositoryUnderTest.deleteById(42L)).isFalse();
        assertThat(commitsOnBranch()).hasSize(1);
    }

    @Test
    void theHighestIdentifierIsHandedOnAgainAfterItsRowIsDeleted() {
        repositoryUnderTest.save(fastingSoup());
        repositoryUnderTest.deleteById(1L);

        // Deliberate, and the price of deriving the next identifier from the stored rows instead of
        // keeping a counter: a deleted *highest* number comes back (ADR 0053, „Nachteile").
        assertThat(repositoryUnderTest.save(fastingTea()).id()).isEqualTo(1L);
    }

    @Test
    void storedFilesAreIndentedJsonEndingWithANewline() throws IOException {
        repositoryUnderTest.save(fastingSoup());

        String content = contentOnBranch("database/entities/recipes/1.json");

        assertThat(content).startsWith("{\n").endsWith("}\n").contains("\"title\" : \"Fastensuppe\"");
    }

    @Test
    void aFreshHandleReadsWhatAnotherOneWrote() {
        repositoryUnderTest.save(fastingSoup());

        GitDataRecipeRepository second = new GitDataRecipeRepository(
                repository, BRANCH, Clock.systemUTC(), JsonMapper.builder().build());

        assertThat(second.findAll()).extracting(Recipe::title).containsExactly("Fastensuppe");
        assertThat(Files.exists(directory.resolve(".git"))).isTrue();
    }

    private Recipe fastingSoup() {
        return Recipe.curried()
                .id(null)
                .title("Fastensuppe")
                .description("Passiertes Gemüse")
                .servings(2)
                .prepTimeMinutes(30)
                .difficulty(Difficulty.EASY)
                .ingredients(List.of(
                        new Ingredient("Karotten", BigDecimal.valueOf(150), "g"),
                        new Ingredient("Fenchel", null, null)))
                .steps(List.of(
                        new PreparationStep(1, "Gemüse putzen"),
                        new PreparationStep(2, "Passieren")));
    }

    private Recipe fastingTea() {
        return Recipe.curried()
                .id(null)
                .title("Fastentee")
                .description(null)
                .servings(1)
                .prepTimeMinutes(5)
                .difficulty(Difficulty.EASY)
                .ingredients(List.of(new Ingredient("Fenchelsamen", BigDecimal.ONE, "TL")))
                .steps(List.of(new PreparationStep(1, "Aufgießen")));
    }

    private boolean branchExists() {
        try {
            return repository.resolve("refs/heads/" + BRANCH) != null;
        } catch (IOException cause) {
            throw new IllegalStateException(cause);
        }
    }

    private List<String> pathsOnBranch() throws IOException {
        List<String> paths = new ArrayList<>();
        try (TreeWalk walk = new TreeWalk(repository)) {
            walk.addTree(commitsOnBranch().getFirst().getTree());
            walk.setRecursive(true);
            while (walk.next()) {
                paths.add(walk.getPathString());
            }
        }
        return paths;
    }

    private String contentOnBranch(String path) throws IOException {
        try (TreeWalk walk =
                TreeWalk.forPath(repository, path, commitsOnBranch().getFirst().getTree())) {
            byte[] content = repository.open(walk.getObjectId(0)).getBytes();
            return new String(content, StandardCharsets.UTF_8);
        }
    }

    /** The commits of the branch, newest first. */
    private List<RevCommit> commitsOnBranch() throws IOException {
        List<RevCommit> commits = new ArrayList<>();
        try (RevWalk walk = new RevWalk(repository)) {
            walk.markStart(walk.parseCommit(repository.resolve("refs/heads/" + BRANCH)));
            walk.forEach(commits::add);
        }
        return commits;
    }
}
