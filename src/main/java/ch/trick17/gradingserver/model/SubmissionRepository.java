package ch.trick17.gradingserver.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.stream.Stream;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
    List<Submission> findByResultIsNull();
    Stream<Submission> findByResultNotNullOrderByIdDesc();
}
