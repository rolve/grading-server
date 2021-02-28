package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface SolutionRepository extends PagingAndSortingRepository<Solution, Integer> {
    List<Solution> findByRepoUrl(String repoUrl);
}
