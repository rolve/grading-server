package ch.trick17.gradingserver.webapp.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

import static java.net.http.HttpClient.newHttpClient;
import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpResponse.BodyHandlers.discarding;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;

@Service
public class DependencyResolver {

    private static final String MVN_CENTRAL_BASE = "https://repo1.maven.org/maven2/";

    private final HttpClient http = newHttpClient();

    public List<URI> resolveDependencyUrls(String rawDependencies)
            throws InvalidURLException, NoJarFileFoundException, IOException {
        if (rawDependencies.isBlank()) {
            return emptyList();
        }
        var urls = new ArrayList<URI>();
        for (var entry : rawDependencies.split("\\s+")) {
            var parts = entry.split(":");
            if (parts.length == 3 && stream(parts).allMatch(p -> p.matches("[A-Za-z0-9_\\-.]+"))) {
                // entry corresponds to artifact coordinates
                var groupId = parts[0];
                var artifactId = parts[1];
                var version = parts[2];
                var url = URI.create(MVN_CENTRAL_BASE + groupId.replaceAll("\\.", "/")
                        + "/" + artifactId + "/" + version
                        + "/" + artifactId + "-" + version + ".jar");
                if (!jarFileExists(url)) {
                    throw new NoJarFileFoundException(entry + " (" + url + ")");
                }
                urls.add(url);
            } else {
                // otherwise, entry has to be a valid URL
                try {
                    var url = new URI(entry);
                    if (url.getScheme() == null || !url.getScheme().matches("https?")) {
                        throw new InvalidURLException(entry);
                    }
                    if (!jarFileExists(url)) {
                        throw new NoJarFileFoundException(entry);
                    }
                    urls.add(url);
                } catch (URISyntaxException e) {
                    throw new InvalidURLException(entry);
                }
            }
        }
        return urls;
    }

    private boolean jarFileExists(URI url) throws IOException {
        var request = HttpRequest.newBuilder(url)
                .method("HEAD", noBody())
                .build();
        try {
            return http.send(request, discarding()).statusCode() == 200;
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    public static class InvalidURLException extends Exception {
        private final String url;

        public InvalidURLException(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    public static class NoJarFileFoundException extends Exception {
        private final String coordinates;

        public NoJarFileFoundException(String coordinates) {
            this.coordinates = coordinates;
        }

        public String getCoordinates() {
            return coordinates;
        }
    }
}
