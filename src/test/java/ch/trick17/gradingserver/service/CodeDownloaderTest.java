package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.CodeLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.jgit.util.FileUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeDownloaderTest {

    Path dir = Path.of("test-temp");

    @Test
    void specificCommit() throws IOException {
        var downloader = new CodeDownloader();
        var location = new CodeLocation("https://github.com/rolve/java-teaching-tools.git",
                "6493a0fe36f3739929a981ce1440111d0071e08e");
        downloader.downloadCode(location, dir, null, null);
        var versionLine = Files.readAllLines(dir.resolve("pom.xml")).stream()
                .filter(l -> l.contains("<version>")).findFirst().orElseThrow().strip();
        assertEquals("<version>1.4.0-SNAPSHOT</version>", versionLine);
    }

    @Test
    void withCredentials() throws IOException {
        var username = "grading-server";
        var deployToken = "VBgo1xky7z87tKdzXacw"; // read-only deploy token
        var downloader = new CodeDownloader();
        var location = new CodeLocation("https://gitlab.com/rolve/some-private-repo.git",
                "5f5ffff42176fc05bd3947ad2971712fb409ae9b");
        downloader.downloadCode(location, dir, username, deployToken);
        assertTrue(Files.exists(dir.resolve("src/foo/Foo.java")));
    }

    @AfterEach
    void deleteDir() throws IOException {
        delete(dir.toFile(), RECURSIVE | RETRY);
    }
}
