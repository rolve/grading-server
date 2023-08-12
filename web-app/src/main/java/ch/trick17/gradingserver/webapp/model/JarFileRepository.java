package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.JarFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JarFileRepository extends JpaRepository<JarFile, Integer> {
    Optional<JarFile> findByFilenameAndHash(String filename, byte[] hash);
}
