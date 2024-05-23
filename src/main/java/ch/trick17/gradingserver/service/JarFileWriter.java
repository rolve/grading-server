package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.JarFile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.lang.Runtime.getRuntime;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.eclipse.jgit.util.FileUtils.*;

@Service
public class JarFileWriter {

    private final Base64.Encoder encoder = Base64.getUrlEncoder();
    private final Path dir;

    public JarFileWriter() throws IOException {
        dir = Files.createTempDirectory("grading-server-jars-");
        getRuntime().addShutdownHook(new Thread(() -> {
            try {
                delete(dir.toFile(), RECURSIVE | RETRY);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));
    }

    public List<Path> write(List<JarFile> files)
            throws IOException {
        var paths = new ArrayList<Path>();
        for (var file : files) {
            var dirName = encoder.encodeToString(file.getHash());
            var path = dir.resolve(dirName).resolve(file.getFilename());
            writeIfNeeded(path, file.getContent());
            paths.add(path);
        }
        return paths;
    }

    private synchronized static void writeIfNeeded(Path path, byte[] content) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.write(path, content, CREATE_NEW, WRITE);
        } // else: already written
    }
}
