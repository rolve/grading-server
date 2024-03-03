package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.GradingResult;
import ch.trick17.gradingserver.model.Submission;
import ch.trick17.gradingserver.model.SubmissionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {

    private final SubmissionRepository repo;
    private final GradingService gradingService;

    public SubmissionService(SubmissionRepository repo, GradingService gradingService) {
        this.repo = repo;
        this.gradingService = gradingService;
    }

    @Transactional
    public void setGradingStarted(Submission submission, boolean gradingStarted) {
        submission.setGradingStarted(gradingStarted);
        repo.save(submission);
    }

    @Transactional
    public void setResult(Submission submission, GradingResult result) {
        submission.setResult(result);
        repo.save(submission);
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void requeueUngradedSubmissions() {
        var ungraded = repo.findByGradingStartedIsFalse();
        // some of these may already be queued and ignored by GradingService
        for (var submission : ungraded) {
            gradingService.grade(submission); // async
        }
    }
}
