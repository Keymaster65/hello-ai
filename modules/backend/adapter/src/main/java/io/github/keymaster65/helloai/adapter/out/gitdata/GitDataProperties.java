package io.github.keymaster65.helloai.adapter.out.gitdata;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where and whether the git-backed store writes (ADR 0054).
 *
 * <p>Configured under {@code recipes.gitdata}. The default is <em>off</em>: an application that
 * starts writing commits into a repository because someone deployed it would be a surprise, and the
 * REST front that comes with it is a second write path into the system (Security skill, „Neuer
 * offen erreichbarer Pfad").
 *
 * @param enabled    whether the store and its REST front exist at all
 * @param repository directory of the git repository to write into; created as a <em>bare</em>
 *                   repository if it holds none yet. Must be set when {@code enabled}
 * @param branch     branch the rows live on; created with its first commit
 */
@ConfigurationProperties(prefix = "recipes.gitdata")
record GitDataProperties(boolean enabled, String repository, String branch) {

    /** Branch of the entity files when none is configured. */
    static final String DEFAULT_BRANCH = "data";

    GitDataProperties {
        branch = branch == null || branch.isBlank() ? DEFAULT_BRANCH : branch;
        if (enabled) {
            Objects.requireNonNull(repository, "recipes.gitdata.repository must be set when enabled");
            if (repository.isBlank()) {
                // Without a directory the adapter would have to guess one, and a guess here is a
                // directory somebody else is working in.
                throw new IllegalArgumentException("recipes.gitdata.repository must not be blank");
            }
        }
    }

    /**
     * Starts the curried construction of {@link GitDataProperties} (see ADR 0021).
     *
     * @return the first step of the curried factory
     */
    public static EnabledStep curried() {
        return enabled -> repository -> branch -> new GitDataProperties(enabled, repository, branch);
    }

    /** Step 1 of {@link #curried()}: whether the store exists. */
    @FunctionalInterface
    public interface EnabledStep {

        /**
         * @param enabled whether the store and its REST front exist at all
         * @return the next step
         */
        RepositoryStep enabled(boolean enabled);
    }

    /** Step 2 of {@link #curried()}: the directory. */
    @FunctionalInterface
    public interface RepositoryStep {

        /**
         * @param repository directory of the git repository to write into
         * @return the next step
         */
        BranchStep repository(String repository);
    }

    /** Step 3 of {@link #curried()}: the branch, completing the properties. */
    @FunctionalInterface
    public interface BranchStep {

        /**
         * @param branch branch the rows live on; {@code null} falls back to {@value #DEFAULT_BRANCH}
         * @return the finished {@link GitDataProperties}
         */
        GitDataProperties branch(String branch);
    }
}
