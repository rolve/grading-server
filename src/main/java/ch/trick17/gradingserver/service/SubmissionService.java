package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.GradingResult;
import ch.trick17.gradingserver.model.Submission;
import ch.trick17.gradingserver.model.SubmissionRepository;
import org.slf4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ch.trick17.gradingserver.model.SubmissionState.OUTDATED;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class SubmissionService {

    private static final long OUTDATED_BATCH_SIZE = 5;

    private static final Logger logger = getLogger(SubmissionService.class);

    private final SubmissionRepository repo;
    private final GradingService gradingService;

    private volatile boolean outdatedResultsLeft = true;

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

    @Scheduled(fixedRate = 2 * 60 * 1000, initialDelay = 30 * 1000)
    @Transactional
    public void updateOutdatedResults() {
        if (outdatedResultsLeft && gradingService.isIdle()) {
            // Enqueue outdated results few at a time, to avoid enqueuing a huge amount of
            // submissions at once after changing the result format. The grading service would
            // prioritize sensibly anyway, but this way, the more expressive "outdated" status is
            // kept for a longer time.
            var outdated = repo.findByResultNotNullOrderByIdDesc()
                    .filter(s -> s.getStatus() == OUTDATED)
                    .limit(OUTDATED_BATCH_SIZE)
                    .toList();
            if (outdated.isEmpty()) {
                outdatedResultsLeft = false;
            } else {
                for (var submission : outdated) {
                    logger.info("Re-queuing submission {} due to outdated result", submission.getId());
                    gradingService.grade(submission); // async
                }
            }
        }
    }
}
