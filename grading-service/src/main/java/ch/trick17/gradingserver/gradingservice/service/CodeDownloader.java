package ch.trick17.gradingserver.gradingservice.service;

import ch.trick17.gradingserver.CodeLocation;
import ch.trick17.gradingserver.gradingservice.model.Credentials;
import ch.trick17.gradingserver.gradingservice.model.CredentialsRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.exists;
import static java.util.Comparator.reverseOrder;

@Service
public class CodeDownloader {

    private final CredentialsRepository credentialsRepo;

    public CodeDownloader(CredentialsRepository credentialsRepo) {
        this.credentialsRepo = credentialsRepo;
    }

    public void downloadCode(CodeLocation from, Path to) throws IOException {
        Credentials credentials;
        try {
            var host = new URI(from.getRepoUrl()).getHost();
            credentials = credentialsRepo.findByHost(host).findFirst().orElse(null);
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }

        int attempts = 3;
        while (attempts-- > 0) {
            try {
                deleteDir(to);
                
                var clone = Git.cloneRepository()
                        .setURI(from.getRepoUrl())
                        .setDirectory(to.toFile());
                if (credentials != null) {
                    clone.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                            credentials.getUsername(), credentials.getPassword()));
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

    static void deleteDir(Path dir) throws IOException {
        // note the difference to Files.delete() and the like:
        // https://stackoverflow.com/questions/12139482/
        // not sure what this means for non-Windows systems, but we'll see...
        if (exists(dir)) {
            try (var walk = Files.walk(dir)) {
                walk.map(Path::toFile).sorted(reverseOrder()).forEach(File::delete);
            }
        }
    }
}
