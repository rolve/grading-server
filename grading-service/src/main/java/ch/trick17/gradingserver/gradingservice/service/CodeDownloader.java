package ch.trick17.gradingserver.gradingservice.service;

import ch.trick17.gradingserver.model.CodeLocation;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

import static java.nio.file.Files.exists;
import static org.eclipse.jgit.util.FileUtils.*;

@Service
public class CodeDownloader {

    public void downloadCode(CodeLocation from, Path to, String username, String password) throws IOException {
        int attempts = 3;
        while (attempts-- > 0) {
            try {
                if (exists(to)) {
                    delete(to.toFile(), RECURSIVE | RETRY);
                }

                var clone = Git.cloneRepository()
                        .setURI(from.getRepoUrl())
                        .setDirectory(to.toFile());
                if (username != null) {
                    clone.setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(username, password));
                }
                try (var git = clone.call()) {
                    git.checkout().setName(from.getCommitHash()).call();
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
