package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

public interface CourseRepository extends PagingAndSortingRepository<Course, Integer> {}
