-- Initial schema for the recipe management backend.
-- The very same script is used by Flyway at runtime and by jOOQ's DDLDatabase for code generation.

CREATE TABLE recipe (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    servings          INTEGER,
    prep_time_minutes INTEGER,
    difficulty        VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ingredient (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id BIGINT        NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    position  INTEGER       NOT NULL,
    name      VARCHAR(200)  NOT NULL,
    quantity  NUMERIC(12, 3),
    unit      VARCHAR(50)
);

CREATE TABLE preparation_step (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id   BIGINT  NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    instruction TEXT    NOT NULL
);

CREATE INDEX idx_ingredient_recipe ON ingredient (recipe_id);
CREATE INDEX idx_preparation_step_recipe ON preparation_step (recipe_id);
