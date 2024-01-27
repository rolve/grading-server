package ch.trick17.gradingserver.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.jgit.lib.ObjectId.fromString;

/**
 * Wrapper around a {@link org.eclipse.jgit.internal.storage.dfs.InMemoryRepository}
 * that provides a simplified interface to determine the paths that are affected by
 * the changes between two commits in a remote repository.
 */
public class GitRepoDiffFetcher implements Closeable {

    private final InMemoryRepository repo;
    private final Git git;

    public GitRepoDiffFetcher(String repoUrl, String branch, String user, String passwort)
            throws GitAPIException {
        repo = new InMemoryRepository(new DfsRepositoryDescription());
        git = new Git(repo);
        var credentials = new UsernamePasswordCredentialsProvider(user, passwort);
        try {
            git.fetch()
                    .setRemote(repoUrl)
                    .setCredentialsProvider(credentials)
                    .setRefSpecs("+refs/heads/" + branch + ":refs/heads/" + branch)
                    .call();
        } catch (TransportException e) {
            // ignore errors caused by empty repos
            if (!e.getMessage().contains("does not have refs/heads/" + branch + " available for fetch")) {
                throw e;
            }
        }
    }

    public Set<String> affectedPaths(String fromCommitHash, String toCommitHash) {
        try {
            var fromIterator = fromCommitHash == null
                    ? new CanonicalTreeParser()
                    : treeIterator(repo, fromCommitHash);
            var toIterator = treeIterator(repo, toCommitHash);
            return git.diff()
                    .setOldTree(fromIterator)
                    .setNewTree(toIterator)
                    .call().stream()
                    .flatMap(e -> Stream.of(e.getOldPath(), e.getNewPath()))
                    .filter(not("/dev/null"::equals))
                    .collect(toSet());
        } catch (IOException | GitAPIException e) {
            throw new AssertionError(e); // shouldn't happen, everything is in-memory
        }
    }

    private static AbstractTreeIterator treeIterator(Repository repo, String commitHash)
            throws IOException {
        try (var revWalk = new RevWalk(repo)) {
            var tree = revWalk.parseCommit(fromString(commitHash)).getTree();
            try (var reader = repo.newObjectReader()) {
                return new CanonicalTreeParser(null, reader, tree.getId());
            }
        }
    }

    @Override
    public void close() {
        repo.close();
    }
}
