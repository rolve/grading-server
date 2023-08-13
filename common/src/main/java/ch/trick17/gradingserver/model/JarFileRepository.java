package ch.trick17.gradingserver.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JarFileRepository extends JpaRepository<JarFile, Integer> {
    Optional<JarFile> findByFilenameAndHash(String filename, byte[] hash);

    default JarFile deduplicate(JarFile jar) {
        var existing = findByFilenameAndHash(jar.getFilename(), jar.getHash());
        return existing.orElseGet(() -> save(jar));
    }
}
