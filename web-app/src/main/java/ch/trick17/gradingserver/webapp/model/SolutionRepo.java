package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface SolutionRepo extends PagingAndSortingRepository<Solution, Integer> {}
