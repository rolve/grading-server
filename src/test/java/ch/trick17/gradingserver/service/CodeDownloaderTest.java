package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.CodeLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeDownloaderTest {

    @Test
    void specificCommit() throws IOException {
        var location = new CodeLocation("https://github.com/rolve/java-teaching-tools.git",
                "6493a0fe36f3739929a981ce1440111d0071e08e");
        var downloader = new CodeDownloader(location, null, null);
        var sources = downloader.checkoutCode(Path.of("grader/src/main/java"), "ch.trick17.jtt.grader");
        var grader = sources.stream()
                .filter(s -> s.getPath().endsWith("Grader.java"))
                .findFirst().orElseThrow();
        var firstLine = grader.getContent().lines().findFirst().orElseThrow();
        assertEquals("package ch.trick17.jtt.grader;", firstLine);
    }

    @Test
    void withCredentials() throws IOException {
        var username = "grading-server";
        var deployToken = "VBgo1xky7z87tKdzXacw"; // read-only deploy token
        var location = new CodeLocation("https://gitlab.com/rolve/some-private-repo.git",
                "5f5ffff42176fc05bd3947ad2971712fb409ae9b");
        var downloader = new CodeDownloader(location, username, deployToken);
        var sources = downloader.checkoutCode(Path.of("src"), "foo");
        var foo = sources.stream()
                .filter(s -> s.getPath().endsWith("Foo.java"))
                .findFirst().orElseThrow();
        var firstLine = foo.getContent().lines().findFirst().orElseThrow();
        assertEquals("package foo;", firstLine);
    }

    @Test
    void nonJavaFiles() throws IOException {
        var username = "grading-server";
        var deployToken = "VBgo1xky7z87tKdzXacw"; // read-only deploy token
        var location = new CodeLocation("https://gitlab.com/rolve/some-private-repo.git",
                "ef9341e0fe57dd3737f5187ef29318d15942796a");
        var downloader = new CodeDownloader(location, username, deployToken);
        var sources = downloader.checkoutCode(Path.of("src"), "foo");
        assertEquals(1, sources.size());
        assertEquals("foo/Foo.java", sources.get(0).getPath());
    }
}
