package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ProblemSetRepository extends JpaRepository<ProblemSet, Integer> {}
