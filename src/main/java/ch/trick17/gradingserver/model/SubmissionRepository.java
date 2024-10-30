package ch.trick17.gradingserver.model;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.stream.Stream;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
    List<Submission> findByResultIsNull();
    @QueryHints({
            @QueryHint(name = HINT_FETCH_SIZE, value = "25"),
            @QueryHint(name = HINT_CACHEABLE, value = "false")})
    Stream<Submission> findByResultNotNullOrderByIdDesc();
}
