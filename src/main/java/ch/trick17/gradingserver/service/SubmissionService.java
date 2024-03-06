package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.GradingResult;
import ch.trick17.gradingserver.model.Submission;
import ch.trick17.gradingserver.model.SubmissionRepository;
import org.slf4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class SubmissionService {

    private static final Logger logger = getLogger(SubmissionService.class);

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

    @EventListener
    @Transactional
    public void gradeUngradedSubmissions(ContextRefreshedEvent ignored) {
        var ungraded = repo.findByResultIsNull();
        if (!ungraded.isEmpty()) {
            logger.info("Found {} ungraded submissions", ungraded.size());
        }
        for (var submission : ungraded) {
            gradingService.grade(submission); // async
        }
    }
}
