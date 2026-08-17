package io.github.keymaster65.helloai.adapter.out.gitdata;

/**
 * One row of the {@code preparation_steps} entity as it is stored in a single JSON file
 * (ADR 0053).
 *
 * <p>The components mirror the columns of the {@code preparation_step} table. The position comes
 * from the domain here, it is not derived from an order: a step carries its number itself
 * (see {@code docs/system/datenmodell.adoc}, the asymmetry to the ingredients).
 *
 * @param id          identifier, unique within the entity
 * @param recipeId    identifier of the recipe this step belongs to
 * @param position    1-based order within that recipe
 * @param instruction textual instruction
 */
record PreparationStepDocument(Long id, Long recipeId, Integer position, String instruction) {

    /**
     * Starts the curried construction of a {@link PreparationStepDocument} (see ADR 0021).
     *
     * @return the first step of the curried factory
     */
    public static IdStep curried() {
        return id -> recipeId -> position -> instruction ->
                new PreparationStepDocument(id, recipeId, position, instruction);
    }

    /** Step 1 of {@link #curried()}: the identifier. */
    @FunctionalInterface
    public interface IdStep {

        /**
         * @param id identifier, unique within the entity
         * @return the next step
         */
        RecipeIdStep id(Long id);
    }

    /** Step 2 of {@link #curried()}: the foreign key. */
    @FunctionalInterface
    public interface RecipeIdStep {

        /**
         * @param recipeId identifier of the recipe this step belongs to
         * @return the next step
         */
        PositionStep recipeId(Long recipeId);
    }

    /** Step 3 of {@link #curried()}: the position. */
    @FunctionalInterface
    public interface PositionStep {

        /**
         * @param position 1-based order within that recipe
         * @return the next step
         */
        InstructionStep position(Integer position);
    }

    /** Step 4 of {@link #curried()}: the instruction, completing the document. */
    @FunctionalInterface
    public interface InstructionStep {

        /**
         * @param instruction textual instruction
         * @return the finished {@link PreparationStepDocument}
         */
        PreparationStepDocument instruction(String instruction);
    }
}
