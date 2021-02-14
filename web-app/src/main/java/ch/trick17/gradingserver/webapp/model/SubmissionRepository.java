package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface SubmissionRepository extends PagingAndSortingRepository<Submission, Integer> {
    boolean existsBySolutionAndCommitHash(Solution sol, String commitHash);
}
