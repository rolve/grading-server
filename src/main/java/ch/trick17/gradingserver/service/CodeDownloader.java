package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.CodeLocation;
import ch.trick17.jtt.memcompile.InMemSource;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.exists;
import static org.eclipse.jgit.util.FileUtils.*;

public class CodeDownloader {

    private final CodeLocation source;
    private final String username;
    private final String password;

    public CodeDownloader(CodeLocation source, String username, String password) {
        this.source = source;
        this.username = username;
        this.password = password;
    }

    public List<InMemSource> downloadCode(Path srcDirPath,
                                          String packageFilter) throws IOException {
        if (srcDirPath.isAbsolute()) {
            throw new IllegalArgumentException("srcDirPath must be relative");
        }
        int attempts = 3;
        while (true) {
            var temp = createTempDirectory("grading-server-");
            try {
                var clone = Git.cloneRepository()
                        .setURI(source.getRepoUrl())
                        .setDirectory(temp.toFile());
                if (username != null) {
                    clone.setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(username, password));
                }
                try (var git = clone.call()) {
                    git.checkout().setName(source.getCommitHash()).call();
                }
                var srcDir = temp.resolve(srcDirPath);
                return InMemSource.fromDirectory(srcDir, packageFilter);
            } catch (GitAPIException e) {
                if (--attempts == 0) {
                    throw new IOException(e);
                }
            } finally {
                if (exists(temp)) {
                    delete(temp.toFile(), RECURSIVE | RETRY);
                }
            }
        }
    }
}
