package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface SolutionRepository extends PagingAndSortingRepository<Solution, Integer> {}
