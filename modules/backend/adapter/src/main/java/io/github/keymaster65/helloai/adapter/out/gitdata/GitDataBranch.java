package io.github.keymaster65.helloai.adapter.out.gitdata;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * The {@code data} branch as a map of paths to file contents, plus a way to replace it in a single
 * commit (ADR 0053).
 *
 * <p>Nothing here touches a working tree. Reading walks the tree of the branch tip, writing builds
 * a fresh tree in the object database and moves the ref. The repository may therefore stay checked
 * out on any other branch while this class works &ndash; which it does, since the branch it writes
 * lives in the repository of the installation itself.
 *
 * <p>The branch is <em>root-less</em>: its first commit has no parent, so it shares no history with
 * the code. Every later commit carries the previous tip as its parent, and the ref is moved with
 * the old tip as the expected value &ndash; a concurrent writer loses the update instead of
 * overwriting it silently.
 */
final class GitDataBranch {

    /** Committer of every commit on the branch: the adapter, not the person running the process. */
    private static final String AUTHOR_NAME = "recipes gitdata adapter";

    /** No mailbox exists for the adapter; the address names the machine part, not a person. */
    private static final String AUTHOR_EMAIL = "gitdata@recipes.invalid";

    private final Repository repository;
    private final String branch;
    private final Clock clock;

    /**
     * @param repository the repository holding the branch; stays open and is not closed here
     * @param branch     short name of the branch, e.g. {@code data}
     * @param clock      source of the commit timestamps &ndash; injected so a test can fix them
     */
    GitDataBranch(Repository repository, String branch, Clock clock) {
        this.repository = repository;
        this.branch = branch;
        this.clock = clock;
    }

    /**
     * Reads every file of the branch tip.
     *
     * @return path to content, sorted by path; empty if the branch does not exist yet
     * @throws GitDataException if the repository cannot be read
     */
    Map<String, byte[]> readAll() {
        try {
            ObjectId tip = repository.resolve(reference());
            if (tip == null) {
                // Not an error: an empty branch is the state before the first write.
                return Map.of();
            }
            Map<String, byte[]> files = new TreeMap<>();
            try (RevWalk walk = new RevWalk(repository);
                    TreeWalk tree = new TreeWalk(repository)) {
                RevCommit commit = walk.parseCommit(tip);
                tree.addTree(commit.getTree());
                tree.setRecursive(true);
                while (tree.next()) {
                    files.put(tree.getPathString(), repository.open(tree.getObjectId(0)).getBytes());
                }
            }
            return files;
        } catch (IOException cause) {
            throw new GitDataException("cannot read branch " + branch, cause);
        }
    }

    /**
     * Replaces the content of the branch with {@code files} in one commit.
     *
     * <p>The whole set is written, not a difference: what is absent from the map is absent from the
     * commit. That is what makes a delete a delete.
     *
     * @param files   path to content, the complete new state of the branch
     * @param message commit message, one line
     * @throws GitDataException if the commit cannot be written or the ref cannot be moved
     */
    void write(Map<String, byte[]> files, String message) {
        try (ObjectInserter inserter = repository.newObjectInserter()) {
            ObjectId tree = insertTree(inserter, files);
            ObjectId tip = repository.resolve(reference());
            ObjectId commit = inserter.insert(commitFor(tree, tip, message));
            inserter.flush();
            moveReference(tip, commit);
        } catch (IOException cause) {
            throw new GitDataException("cannot write branch " + branch, cause);
        }
    }

    private ObjectId insertTree(ObjectInserter inserter, Map<String, byte[]> files)
            throws IOException {
        DirCache cache = DirCache.newInCore();
        DirCacheBuilder builder = cache.builder();
        // Sorted, because a DirCache is sorted by path; TreeMap spares the builder the resort.
        for (Map.Entry<String, byte[]> file : new TreeMap<>(files).entrySet()) {
            DirCacheEntry entry = new DirCacheEntry(file.getKey());
            entry.setFileMode(FileMode.REGULAR_FILE);
            entry.setObjectId(inserter.insert(Constants.OBJ_BLOB, file.getValue()));
            builder.add(entry);
        }
        builder.finish();
        return cache.writeTree(inserter);
    }

    private CommitBuilder commitFor(ObjectId tree, ObjectId tip, String message) {
        PersonIdent identity =
                new PersonIdent(AUTHOR_NAME, AUTHOR_EMAIL, clock.instant(), clock.getZone());
        CommitBuilder commit = new CommitBuilder();
        commit.setTreeId(tree);
        if (tip != null) {
            commit.setParentId(tip);
        }
        commit.setAuthor(identity);
        commit.setCommitter(identity);
        commit.setMessage(message);
        return commit;
    }

    private void moveReference(ObjectId expectedTip, ObjectId commit) throws IOException {
        RefUpdate update = repository.updateRef(reference());
        update.setExpectedOldObjectId(expectedTip == null ? ObjectId.zeroId() : expectedTip);
        update.setNewObjectId(commit);
        update.setRefLogMessage("gitdata adapter", false);
        RefUpdate.Result result = update.update();
        if (result != RefUpdate.Result.NEW && result != RefUpdate.Result.FAST_FORWARD) {
            throw new GitDataException(
                    "cannot move branch " + branch + ", git answered " + result);
        }
    }

    private String reference() {
        return Constants.R_HEADS + branch;
    }
}
