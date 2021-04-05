package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface SolutionRepository extends JpaRepository<Solution, Integer> {
    List<Solution> findByRepoUrl(String repoUrl);
}
