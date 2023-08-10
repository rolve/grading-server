package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.JarFile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import static ch.trick17.gradingserver.webapp.service.JarDownloader.JarDownloadFailedException.Reason.*;
import static java.net.http.HttpClient.Redirect.NORMAL;
import static java.util.Arrays.stream;
import static org.springframework.http.ContentDisposition.parse;

@Service
public class JarDownloader {

    private static final String MVN_CENTRAL_BASE = "https://repo1.maven.org/maven2/";

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(NORMAL)
            .build();

    /**
     * Downloads the JAR file specified by the given "identifier", which may be
     * an HTTP(S) URL or an artifact coordinate triplet for Maven Central
     * (groupId:artifactId:version).
     */
    public JarFile downloadAndCheckJar(String identifier)
            throws JarDownloadFailedException {
        var url = toUrl(identifier);
        var jar = downloadJar(url);
        var in = new ZipInputStream(new ByteArrayInputStream(jar.getContent()));
        try {
            in.getNextEntry();
        } catch (ZipException e) {
            throw new JarDownloadFailedException(INVALID_JAR, url.toString());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return jar;
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
