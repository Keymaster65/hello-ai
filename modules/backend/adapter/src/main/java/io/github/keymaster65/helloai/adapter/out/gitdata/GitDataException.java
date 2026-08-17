package io.github.keymaster65.helloai.adapter.out.gitdata;

/**
 * Signals that the {@code data} branch could not be read or written.
 *
 * <p>The outbound port {@link io.github.keymaster65.helloai.application.port.out.RecipeRepository}
 * declares no checked exception &ndash; a storage failure must not become part of the contract the
 * application layer sees. The adapter therefore wraps the {@link java.io.IOException} of JGit here
 * (ADR 0053).
 *
 * <p>The message names the operation and the branch, never a file system path: a path would leak
 * the layout of the installation into a log line (Security skill, principle 2).
 */
public class GitDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what failed, without internal paths
     * @param cause   the underlying failure
     */
    public GitDataException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message what failed, without internal paths
     */
    public GitDataException(String message) {
        super(message);
    }
}
