package io.github.keymaster65.helloai.adapter.out.gitdata;

/**
 * One row of the {@code recipes} entity as it is stored in a single JSON file (ADR 0053).
 *
 * <p>The components mirror the columns of the {@code recipe} table, ingredients and steps
 * excluded: they are rows of their own, in files of their own. {@code difficulty} is a
 * {@code String} for the same reason the column is a {@code VARCHAR} &ndash; the stored format does
 * not change when the enum gains a constant.
 *
 * @param id              identifier, unique within the entity
 * @param title           title of the recipe
 * @param description     free-text description, may be {@code null}
 * @param servings        number of servings, may be {@code null}
 * @param prepTimeMinutes preparation time in minutes, may be {@code null}
 * @param difficulty      name of the difficulty constant
 */
record RecipeDocument(
        Long id,
        String title,
        String description,
        Integer servings,
        Integer prepTimeMinutes,
        String difficulty) {

    /**
     * Starts the curried construction of a {@link RecipeDocument} (see ADR 0021).
     *
     * @return the first step of the curried factory
     */
    public static IdStep curried() {
        return id -> title -> description -> servings -> prepTimeMinutes -> difficulty ->
                new RecipeDocument(id, title, description, servings, prepTimeMinutes, difficulty);
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

    /** Step 6 of {@link #curried()}: the difficulty, completing the document. */
    @FunctionalInterface
    public interface DifficultyStep {

        /**
         * @param difficulty name of the difficulty constant
         * @return the finished {@link RecipeDocument}
         */
        RecipeDocument difficulty(String difficulty);
    }
}
