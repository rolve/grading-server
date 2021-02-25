package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface SubmissionRepository extends PagingAndSortingRepository<Submission, Integer> {
    boolean existsBySolutionAndCommitHash(Solution sol, String commitHash);
    List<Submission> findByGradingStartedIsFalse();
}
