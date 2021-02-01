package ch.trick17.gradingserver.gradingservice.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradingJobRepository extends CrudRepository<GradingJob, String> {}
