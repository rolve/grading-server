package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.CodeLocation;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

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

    public void downloadCode(Path to) throws IOException {
        int attempts = 3;
        while (attempts-- > 0) {
            try {
                if (exists(to)) {
                    delete(to.toFile(), RECURSIVE | RETRY);
                }

                var clone = Git.cloneRepository()
                        .setURI(source.getRepoUrl())
                        .setDirectory(to.toFile());
                if (username != null) {
                    clone.setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(username, password));
                }
                try (var git = clone.call()) {
                    git.checkout().setName(source.getCommitHash()).call();
                }
                break; // done
            } catch (GitAPIException e) {
                if (attempts == 0) {
                    throw new IOException(e);
                }
            }
        }
    }
}
