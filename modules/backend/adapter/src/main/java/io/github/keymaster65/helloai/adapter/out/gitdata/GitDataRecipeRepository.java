package io.github.keymaster65.helloai.adapter.out.gitdata;

import io.github.keymaster65.helloai.application.port.out.RecipeRepository;
import io.github.keymaster65.helloai.domain.model.Ingredient;
import io.github.keymaster65.helloai.domain.model.PreparationStep;
import io.github.keymaster65.helloai.domain.model.Recipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import tools.jackson.databind.ObjectMapper;

/**
 * Second implementation of the {@link RecipeRepository} outbound port: it stores every row as its
 * own JSON file on a git branch and turns every write into one commit (ADR 0053).
 *
 * <p>The layout mirrors the tables of the relational adapter, one directory per entity:
 *
 * <pre>
 * database/entities/recipes/&lt;id&gt;.json
 * database/entities/ingredients/&lt;id&gt;.json
 * database/entities/preparation_steps/&lt;id&gt;.json
 * </pre>
 *
 * <p>Every operation reads the whole branch, works on the result and &ndash; if it writes &ndash;
 * commits the whole new state. That is the deliberate trade of this adapter: a data set of this size
 * is one tree walk, and a commit per operation is only meaningful if it is complete. The commit is
 * what a transaction is in the other adapter: either the ref moves or nothing happened.
 *
 * <p>Identifiers are assigned like a sequence: the highest one stored plus one, per entity. They are
 * never reused &ndash; a deleted recipe does not hand its number to the next one.
 *
 * <p>Two ways in: {@link #openBare(Path, String, Clock, ObjectMapper)} opens (or creates) a
 * repository this instance then owns and closes, and the constructor takes a repository somebody
 * else opened and keeps owning. Both are package-private, like the jOOQ repository next door: what
 * leaves this package is the port, not the store (ADR 0054).
 */
class GitDataRecipeRepository implements RecipeRepository, AutoCloseable {

    private static final String RECIPES = "database/entities/recipes";
    private static final String INGREDIENTS = "database/entities/ingredients";
    private static final String PREPARATION_STEPS = "database/entities/preparation_steps";
    private static final String SUFFIX = ".json";

    private final GitDataBranch branch;
    private final GitDataMapper mapper = new GitDataMapper();
    private final ObjectMapper json;
    private final Repository owned;

    /**
     * @param repository the repository holding the branch; stays open and is not closed here
     * @param branch     short name of the branch to store in, e.g. {@code data}
     * @param clock      source of the commit timestamps
     * @param json       the JSON mapper; its configuration does not decide the file format
     */
    GitDataRecipeRepository(
            Repository repository, String branch, Clock clock, ObjectMapper json) {
        this(repository, branch, clock, json, null);
    }

    private GitDataRecipeRepository(
            Repository repository, String branch, Clock clock, ObjectMapper json, Repository owned) {
        this.branch = new GitDataBranch(repository, branch, clock);
        this.json = json;
        this.owned = owned;
    }

    /**
     * Opens the store in a <em>bare</em> repository, creating it if the directory holds none yet.
     *
     * <p>Bare on purpose: this adapter never uses a working tree, and a repository without one
     * cannot collide with anybody's checkout. The returned instance owns the repository and closes
     * it in {@link #close()}.
     *
     * @param directory directory of the repository, e.g. {@code /var/lib/recipes/data.git}
     * @param branch    short name of the branch to store in, e.g. {@code data}
     * @param clock     source of the commit timestamps
     * @param json      the JSON mapper
     * @return the store, ready to read and write
     * @throws GitDataException if the repository can neither be opened nor created
     */
    static GitDataRecipeRepository openBare(
            Path directory, String branch, Clock clock, ObjectMapper json) {
        try {
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(directory.toFile())
                    .setMustExist(false)
                    .build();
            if (!repository.getObjectDatabase().exists()) {
                repository.create(true);
            }
            return new GitDataRecipeRepository(repository, branch, clock, json, repository);
        } catch (IOException cause) {
            // The message names what failed, not the directory: it ends up in a log line.
            throw new GitDataException("cannot open the repository of the gitdata store", cause);
        }
    }

    /** Closes the repository if this instance opened it; otherwise nothing is ours to close. */
    @Override
    public void close() {
        if (owned != null) {
            owned.close();
        }
    }

    @Override
    public Recipe save(Recipe recipe) {
        Map<String, byte[]> files = new TreeMap<>(branch.readAll());
        long id = nextId(files, RECIPES);
        files.put(path(RECIPES, id), toJson(mapper.toDocument(id, recipe)));
        writeChildren(files, id, recipe, nextId(files, INGREDIENTS), nextId(files, PREPARATION_STEPS));
        branch.write(files, "insert recipe " + id);
        return read(branch.readAll(), id).orElseThrow(
                () -> new GitDataException("recipe " + id + " is missing right after its commit"));
    }

    @Override
    public Optional<Recipe> findById(long id) {
        return read(branch.readAll(), id);
    }

    @Override
    public List<Recipe> findAll() {
        Map<String, byte[]> files = branch.readAll();
        return recipeIds(files).stream().map(id -> read(files, id).orElseThrow()).toList();
    }

    @Override
    public Optional<Recipe> update(long id, Recipe recipe) {
        Map<String, byte[]> files = new TreeMap<>(branch.readAll());
        if (!files.containsKey(path(RECIPES, id))) {
            return Optional.empty();
        }
        files.put(path(RECIPES, id), toJson(mapper.toDocument(id, recipe)));
        // Children are replaced wholesale, as in the relational adapter: their positions follow the
        // incoming order, and a surviving old row would contradict it. The identifiers of the new
        // rows are taken *before* the old ones go, so they climb like a sequence instead of
        // inheriting the numbers just dropped – that is what the database does, too.
        long ingredientId = nextId(files, INGREDIENTS);
        long stepId = nextId(files, PREPARATION_STEPS);
        removeChildren(files, id);
        writeChildren(files, id, recipe, ingredientId, stepId);
        branch.write(files, "update recipe " + id);
        return read(branch.readAll(), id);
    }

    @Override
    public boolean deleteById(long id) {
        Map<String, byte[]> files = new TreeMap<>(branch.readAll());
        if (files.remove(path(RECIPES, id)) == null) {
            return false;
        }
        // What ON DELETE CASCADE does in the database happens here by hand.
        removeChildren(files, id);
        branch.write(files, "delete recipe " + id);
        return true;
    }

    private void writeChildren(
            Map<String, byte[]> files,
            long recipeId,
            Recipe recipe,
            long firstIngredientId,
            long firstStepId) {
        long ingredientId = firstIngredientId;
        List<Ingredient> ingredients = recipe.ingredients();
        for (int index = 0; index < ingredients.size(); index++) {
            // The position of an ingredient is its place in the list; the domain does not carry it.
            IngredientDocument document =
                    mapper.toDocument(ingredientId, recipeId, index + 1, ingredients.get(index));
            files.put(path(INGREDIENTS, ingredientId), toJson(document));
            ingredientId++;
        }
        long stepId = firstStepId;
        for (PreparationStep step : recipe.steps()) {
            files.put(path(PREPARATION_STEPS, stepId), toJson(mapper.toDocument(stepId, recipeId, step)));
            stepId++;
        }
    }

    private void removeChildren(Map<String, byte[]> files, long recipeId) {
        ingredientsOf(files, recipeId).forEach(child -> files.remove(path(INGREDIENTS, child.id())));
        stepsOf(files, recipeId).forEach(child -> files.remove(path(PREPARATION_STEPS, child.id())));
    }

    private Optional<Recipe> read(Map<String, byte[]> files, long id) {
        byte[] content = files.get(path(RECIPES, id));
        if (content == null) {
            return Optional.empty();
        }
        RecipeDocument recipe = fromJson(content, RecipeDocument.class);
        return Optional.of(mapper.toDomain(recipe, ingredientsOf(files, id), stepsOf(files, id)));
    }

    private List<IngredientDocument> ingredientsOf(Map<String, byte[]> files, long recipeId) {
        return documents(files, INGREDIENTS, IngredientDocument.class).stream()
                .filter(document -> recipeId == document.recipeId())
                .sorted(Comparator.comparing(IngredientDocument::position))
                .toList();
    }

    private List<PreparationStepDocument> stepsOf(Map<String, byte[]> files, long recipeId) {
        return documents(files, PREPARATION_STEPS, PreparationStepDocument.class).stream()
                .filter(document -> recipeId == document.recipeId())
                .sorted(Comparator.comparing(PreparationStepDocument::position))
                .toList();
    }

    private <T> List<T> documents(Map<String, byte[]> files, String directory, Class<T> type) {
        List<T> documents = new ArrayList<>();
        files.forEach((path, content) -> {
            if (isEntityFile(path, directory)) {
                documents.add(fromJson(content, type));
            }
        });
        return documents;
    }

    /** Recipe identifiers in ascending order &ndash; the order of {@code ORDER BY id}. */
    private List<Long> recipeIds(Map<String, byte[]> files) {
        return files.keySet().stream()
                .filter(path -> isEntityFile(path, RECIPES))
                .map(path -> identifier(path, RECIPES))
                .sorted()
                .toList();
    }

    /**
     * The next free identifier of an entity: the highest stored plus one, starting at one. Reading
     * the file names is enough &ndash; the name <em>is</em> the identifier.
     */
    private long nextId(Map<String, byte[]> files, String directory) {
        return files.keySet().stream()
                .filter(path -> isEntityFile(path, directory))
                .mapToLong(path -> identifier(path, directory))
                .max()
                .orElse(0L)
                + 1;
    }

    private boolean isEntityFile(String path, String directory) {
        return path.startsWith(directory + "/") && path.endsWith(SUFFIX);
    }

    private long identifier(String path, String directory) {
        String name = path.substring(directory.length() + 1, path.length() - SUFFIX.length());
        try {
            return Long.parseLong(name);
        } catch (NumberFormatException cause) {
            throw new GitDataException("file name of an entity is not an identifier", cause);
        }
    }

    private String path(String directory, long id) {
        return directory + "/" + id + SUFFIX;
    }

    /**
     * Indented, with a trailing newline: the stored state is meant to be read as a diff, and a file
     * without a final newline shows up as one changed line in every commit that touches it.
     */
    private byte[] toJson(Object document) {
        String text = json.writer().withDefaultPrettyPrinter().writeValueAsString(document);
        return (text + "\n").getBytes(StandardCharsets.UTF_8);
    }

    private <T> T fromJson(byte[] content, Class<T> type) {
        return json.readValue(content, type);
    }
}
