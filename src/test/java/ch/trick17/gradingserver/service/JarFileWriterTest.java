package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.JarFile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static java.nio.file.Files.readAllBytes;
import static org.junit.jupiter.api.Assertions.*;

public class JarFileWriterTest {

    @Test
    void write() throws IOException {
        var writer = new JarFileWriter();
        var files = List.of(
                new JarFile("foo.jar", new byte[]{0, 1}),
                new JarFile("bar.jar", new byte[]{2, 3, 4, 5}));
        var paths = writer.write(files);
        assertEquals(2, paths.size());
        assertEquals("foo.jar", paths.get(0).getFileName().toString());
        assertEquals("bar.jar", paths.get(1).getFileName().toString());
        assertArrayEquals(new byte[] {0, 1}, readAllBytes(paths.get(0)));
        assertArrayEquals(new byte[] {2, 3, 4, 5}, readAllBytes(paths.get(1)));

        var moreFiles = List.of(
                new JarFile("foo.jar", new byte[]{0, 1}),       // same as above
                new JarFile("bar.jar", new byte[]{2, 3, 4}),    // different contents
                new JarFile("baz.jar", new byte[]{6, 7, 8}));   // new file
        var morePaths = writer.write(moreFiles);
        assertEquals(3, morePaths.size());
        assertEquals("foo.jar", morePaths.get(0).getFileName().toString());
        assertEquals("bar.jar", morePaths.get(1).getFileName().toString());
        assertEquals("baz.jar", morePaths.get(2).getFileName().toString());
        assertEquals(paths.get(0), morePaths.get(0));
        assertNotEquals(paths.get(1), morePaths.get(1));
        assertArrayEquals(new byte[] {2, 3, 4}, readAllBytes(morePaths.get(1)));
        assertArrayEquals(new byte[] {6, 7, 8}, readAllBytes(morePaths.get(2)));
    }
}
