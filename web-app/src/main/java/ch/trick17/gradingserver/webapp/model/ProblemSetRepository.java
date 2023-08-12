package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.JarFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSetRepository extends JpaRepository<ProblemSet, Integer> {
    int countByGradingConfigDependenciesContaining(JarFile file);
}
