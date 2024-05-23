package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.CodeLocation;
import ch.trick17.jtt.memcompile.InMemSource;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNullElse;
import static org.eclipse.jgit.lib.ObjectId.fromString;
import static org.eclipse.jgit.treewalk.filter.PathFilterGroup.createFromStrings;

public class CodeDownloader {

    private final CodeLocation source;
    private final String username;
    private final String password;

    public CodeDownloader(CodeLocation source, String username, String password) {
        this.source = source;
        this.username = username;
        this.password = password;
    }

    /**
     * @param srcDirPath Relative path to the source directory, e.g. "foo/src"
     * @param packageFilter Filter for the package, e.g. "foo.bar". May be null.
     */
    public List<InMemSource> downloadCode(Path srcDirPath,
                                          String packageFilter) throws IOException {
        if (srcDirPath.isAbsolute()) {
            throw new IllegalArgumentException("srcDirPath must be relative");
        }
        int attempts = 3;
        while (true) {
            try (var repo = new InMemoryRepository(new DfsRepositoryDescription())) {
                var credentials = new UsernamePasswordCredentialsProvider(
                        requireNonNullElse(username, ""), requireNonNullElse(password, ""));
                var git = new Git(repo);
                git.fetch()
                        .setRemote(source.getRepoUrl())
                        .setRefSpecs(source.getCommitHash())
                        .setCredentialsProvider(credentials)
                        .call();

                var commit = new RevWalk(repo).parseCommit(fromString(source.getCommitHash()));
                var treeWalk = new TreeWalk(repo);
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);
                var packagePath = requireNonNullElse(packageFilter, "")
                        .replace('.', '/');
                var fullPath = srcDirPath.resolve(packagePath).toString()
                        .replace('\\', '/');
                treeWalk.setFilter(createFromStrings(fullPath));
                var result = new ArrayList<InMemSource>();
                while (treeWalk.next()) {
                    var path = srcDirPath.relativize(Path.of(treeWalk.getPathString()))
                            .toString().replace('\\', '/');
                    var loader = repo.open(treeWalk.getObjectId(0));
                    var content = new String(loader.getBytes(), UTF_8);
                    result.add(new InMemSource(path, content));
                }
                return result;
            } catch (GitAPIException e) {
                if (--attempts == 0) {
                    throw new IOException(e);
                }
            }
        }
    }
}
