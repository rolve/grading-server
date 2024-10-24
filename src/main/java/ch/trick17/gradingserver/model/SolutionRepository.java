package ch.trick17.gradingserver.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolutionRepository extends JpaRepository<Solution, Integer> {
    List<Solution> findByRepoUrl(String repoUrl);
    int countByAuthorsContains(Author author);
}
