package ch.trick17.gradingserver.gradingservice.service;

import ch.trick17.gradingserver.CodeLocation;
import ch.trick17.gradingserver.gradingservice.model.CredentialsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class CodeDownloaderTest {

    private Path dir = Path.of("test-temp");

    @Test
    void testDownloadCode() throws IOException {
        var downloader = new CodeDownloader(mock(CredentialsRepository.class));
        var location = new CodeLocation("https://github.com/rolve/java-teaching-tools.git",
                "6493a0fe36f3739929a981ce1440111d0071e08e");
        downloader.downloadCode(location, dir);
        var versionLine = Files.lines(dir.resolve("pom.xml"))
                .filter(l -> l.contains("<version>")).findFirst().get().strip();
        assertEquals("<version>1.4.0-SNAPSHOT</version>", versionLine);
    }

    @AfterEach
    void deleteDir() throws IOException {
        CodeDownloader.deleteDir(dir);
    }
}
