package io.github.keymaster65.helloai.adapter.out.gitdata;

import java.time.Instant;

/**
 * One row of the {@code recipes} entity as it is stored in a single JSON file (ADR 0053).
 *
 * <p>The components mirror the columns of the {@code recipe} table, ingredients and steps
 * excluded: they are rows of their own, in files of their own. {@code difficulty} is a
 * {@code String} for the same reason the column is a {@code VARCHAR} &ndash; the stored format does
 * not change when the enum gains a constant.
 *
 * <p>Since ADR 0055 the two timestamps of the table are here as well. They come from the store's
 * clock, not from the domain: the domain does not carry them, and the database fills them itself
 * (default and {@code UPDATE}). Without them the two stores differed in two columns, which made
 * „the same data" a claim nobody could check.
 *
 * @param id              identifier, unique within the entity
 * @param title           title of the recipe
 * @param description     free-text description, may be {@code null}
 * @param servings        number of servings, may be {@code null}
 * @param prepTimeMinutes preparation time in minutes, may be {@code null}
 * @param difficulty      name of the difficulty constant
 * @param createdAt       when the row was written, set once
 * @param updatedAt       when the row was last replaced; equal to {@code createdAt} until then
 */
record RecipeDocument(
        Long id,
        String title,
        String description,
        Integer servings,
        Integer prepTimeMinutes,
        String difficulty,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Starts the curried construction of a {@link RecipeDocument} (see ADR 0021).
     *
     * @return the first step of the curried factory
     */
    public static IdStep curried() {
        return id -> title -> description -> servings -> prepTimeMinutes -> difficulty ->
                createdAt -> updatedAt -> new RecipeDocument(
                        id, title, description, servings, prepTimeMinutes, difficulty,
                        createdAt, updatedAt);
    }

    /** Step 1 of {@link #curried()}: the identifier. */
    @FunctionalInterface
    public interface IdStep {

        /**
         * @param id identifier, unique within the entity
         * @return the next step
         */
        TitleStep id(Long id);
    }

    /** Step 2 of {@link #curried()}: the title. */
    @FunctionalInterface
    public interface TitleStep {

        /**
         * @param title title of the recipe
         * @return the next step
         */
        DescriptionStep title(String title);
    }

    /** Step 3 of {@link #curried()}: the description. */
    @FunctionalInterface
    public interface DescriptionStep {

        /**
         * @param description free-text description, may be {@code null}
         * @return the next step
         */
        ServingsStep description(String description);
    }

    /** Step 4 of {@link #curried()}: the number of servings. */
    @FunctionalInterface
    public interface ServingsStep {

        /**
         * @param servings number of servings, may be {@code null}
         * @return the next step
         */
        PrepTimeMinutesStep servings(Integer servings);
    }

    /** Step 5 of {@link #curried()}: the preparation time. */
    @FunctionalInterface
    public interface PrepTimeMinutesStep {

        /**
         * @param prepTimeMinutes preparation time in minutes, may be {@code null}
         * @return the next step
         */
        DifficultyStep prepTimeMinutes(Integer prepTimeMinutes);
    }

    /** Step 6 of {@link #curried()}: the difficulty. */
    @FunctionalInterface
    public interface DifficultyStep {

        /**
         * @param difficulty name of the difficulty constant
         * @return the next step
         */
        CreatedAtStep difficulty(String difficulty);
    }

    /** Step 7 of {@link #curried()}: when the row was written. */
    @FunctionalInterface
    public interface CreatedAtStep {

        /**
         * @param createdAt when the row was written, set once
         * @return the next step
         */
        UpdatedAtStep createdAt(Instant createdAt);
    }

    /** Step 8 of {@link #curried()}: when it was last replaced, completing the document. */
    @FunctionalInterface
    public interface UpdatedAtStep {

        /**
         * @param updatedAt when the row was last replaced
         * @return the finished {@link RecipeDocument}
         */
        RecipeDocument updatedAt(Instant updatedAt);
    }
}
