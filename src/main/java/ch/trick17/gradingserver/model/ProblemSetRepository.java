package ch.trick17.gradingserver.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemSetRepository extends JpaRepository<ProblemSet, Integer> {
    int countByProjectConfigDependenciesContaining(JarFile file);
}
