package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.Credentials;
import ch.trick17.gradingserver.GradingResult;
import ch.trick17.gradingserver.webapp.model.HostCredentialsRepository;
import ch.trick17.gradingserver.webapp.model.SolutionRepository;
import ch.trick17.gradingserver.webapp.model.Submission;
import ch.trick17.gradingserver.webapp.model.SubmissionRepository;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class SubmissionService {

    private final SubmissionRepository repo;
    private final HostCredentialsRepository credRepo;
    private final SolutionRepository solRepo;
    private final LatestCommitFetcher fetcher;
    private final GradingService gradingService;

    public SubmissionService(SubmissionRepository repo, HostCredentialsRepository credRepo,
                             SolutionRepository solRepo, LatestCommitFetcher fetcher,
                             GradingService gradingService) {
        this.repo = repo;
        this.credRepo = credRepo;
        this.solRepo = solRepo;
        this.fetcher = fetcher;
        this.gradingService = gradingService;
    }

    @Async
    @Transactional
    public void fetchSubmission(int solId) throws GitAPIException {
        var sol = solRepo.findById(solId).get();
        try {
            var token = credRepo.findLatestForUrl(sol.getRepoUrl())
                    .map(Credentials::getPassword).orElse(null);
            fetcher.fetchLatestCommit(sol.getRepoUrl(), token).ifPresent(commitHash -> {
                if (!repo.existsBySolutionAndCommitHash(sol, commitHash)) {
                    var submission = new Submission(sol, commitHash);
                    repo.save(submission);
                    gradingService.grade(submission);
                }
            });
        } finally {
            sol.setFetchingSubmission(false);
            solRepo.save(sol);
        }
    }

    @Transactional
    public void setGradingStarted(Submission submission) {
        submission.setGradingStarted(true);
        repo.save(submission);
    }

    @Transactional
    public void setResult(Submission submission, GradingResult result) {
        submission.setResult(result);
        repo.save(submission);
    }
}
