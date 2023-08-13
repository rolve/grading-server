package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.JarFile;
import ch.trick17.gradingserver.webapp.model.JarFileRepository;
import ch.trick17.gradingserver.webapp.model.ProblemSetRepository;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import static ch.trick17.gradingserver.webapp.service.JarFileService.JarDownloadFailedException.Reason.*;
import static java.lang.String.join;
import static java.net.http.HttpClient.Redirect.NORMAL;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.ContentDisposition.parse;

@Service
public class JarFileService {

    private static final String MVN_CENTRAL_BASE = "https://repo1.maven.org/maven2/";

    private static final Logger logger = getLogger(JarFileService.class);

    private final JarFileRepository jarFileRepo;
    private final ProblemSetRepository problemSetRepo;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(NORMAL)
            .build();

    public JarFileService(JarFileRepository jarFileRepo,
                          ProblemSetRepository problemSetRepo) {
        this.jarFileRepo = jarFileRepo;
        this.problemSetRepo = problemSetRepo;
    }

    /**
     * Downloads the JAR file specified by the given "identifier", which may be
     * an HTTP(S) URL or an artifact coordinate triplet for Maven Central
     * (groupId:artifactId:version).
     * <p>
     * The JAR file is stored in the DB, it doesn't already exist. If it does
     * exist (with the same content), a reference to the previously stored JAR
     * file is returned, avoiding unnecessary duplication of data in the DB.
     */
    public JarFile downloadAndStoreJarFile(String identifier)
            throws JarDownloadFailedException {
        var url = toUrl(identifier);
        var jar = downloadJar(url);
        checkJarFileValid(url, jar);
        return deduplicate(jar);
    }

    private static URI toUrl(String identifier) throws JarDownloadFailedException {
        URI url;
        var parts = identifier.split(":");
        if (parts.length == 3 && stream(parts).allMatch(p -> p.matches("[A-Za-z0-9_\\-.]+"))) {
            // identifier corresponds to artifact coordinates
            var groupId = parts[0];
            var artifactId = parts[1];
            var version = parts[2];
            url = URI.create(MVN_CENTRAL_BASE + groupId.replaceAll("\\.", "/")
                    + "/" + artifactId + "/" + version
                    + "/" + artifactId + "-" + version + ".jar");
        } else {
            // otherwise, identifier has to be a valid URL
            try {
                url = new URI(identifier);
                if (url.getScheme() == null || !url.getScheme().matches("https?")) {
                    throw new JarDownloadFailedException(INVALID_URL, identifier);
                }
            } catch (URISyntaxException e) {
                throw new JarDownloadFailedException(INVALID_URL, identifier);
            }
        }
        return url;
    }

    private JarFile downloadJar(URI url) throws JarDownloadFailedException {
        try {
            var request = HttpRequest.newBuilder(url).build();
            var response = http.send(request, BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                var filename = extractFilename(response);
                var content = response.body();
                return new JarFile(filename, content);
            } else {
                throw new JarDownloadFailedException(NOT_FOUND, url.toString());
            }
        } catch (IOException e) {
            throw new JarDownloadFailedException(DOWNLOAD_FAILED, url.toString());
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private String extractFilename(HttpResponse<?> response) {
        // first, try to derive from header
        var all = response.headers().allValues("Content-Disposition");
        if (!all.isEmpty()) {
            var contentDisposition = parse(all.get(0));
            if (contentDisposition.getFilename() != null) {
                return contentDisposition.getFilename();
            }
        }

        // second, try to derive from URL path
        var path = response.request().uri().getPath().replaceAll("/+$", "");
        if (!path.isEmpty()) {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        // if all else fails, use default name
        return "lib.jar";
    }

    private static void checkJarFileValid(URI url, JarFile jar) throws JarDownloadFailedException {
        var in = new ZipInputStream(new ByteArrayInputStream(jar.getContent()));
        try {
            in.getNextEntry();
        } catch (ZipException e) {
            throw new JarDownloadFailedException(INVALID_JAR, url.toString());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private JarFile deduplicate(JarFile jar) {
        var existing = jarFileRepo.findByFilenameAndHash(jar.getFilename(), jar.getHash());
        return existing.orElseGet(() -> jarFileRepo.save(jar));
    }

    public boolean deleteIfUnused(JarFile jarFile) {
        var uses = problemSetRepo.countByGradingConfigDependenciesContaining(jarFile);
        if (uses == 0) {
            jarFileRepo.delete(jarFile);
        }
        return uses == 0;
    }

    @Scheduled(fixedRate = 7 * 24 * 60 * 60 * 1000)
    @Transactional
    public void deleteUnused() {
        // TODO: not very efficient...
        var deleted = jarFileRepo.findAll().stream()
                .filter(this::deleteIfUnused)
                .map(JarFile::getFilename)
                .toList();
        if (!deleted.isEmpty()) {
            logger.info("Deleted {} unused JAR files: {}", deleted.size(),
                    join(", ", deleted));
        }
    }

    public static class JarDownloadFailedException extends Exception {
        private final Reason reason;
        private final String url;

        public JarDownloadFailedException(Reason reason, String url) {
            this.reason = reason;
            this.url = url;
        }

        public Reason getReason() {
            return reason;
        }

        public String getUrl() {
            return url;
        }

        public enum Reason {
            INVALID_URL, NOT_FOUND, INVALID_JAR, DOWNLOAD_FAILED
        }
    }
}
