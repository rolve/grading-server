package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.GradingResult;
import ch.trick17.gradingserver.model.Submission;
import ch.trick17.gradingserver.model.SubmissionRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.trick17.gradingserver.model.SubmissionState.OUTDATED;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class SubmissionService {

    private static final int OUTDATED_BATCH_SIZE = 25;

    private static final Logger logger = getLogger(SubmissionService.class);

    private final SubmissionRepository repo;
    private final GradingService gradingService;
    private final EntityManager entityManager;

    private volatile int minCheckedId = Integer.MAX_VALUE;

    public SubmissionService(SubmissionRepository repo, GradingService gradingService,
                             EntityManager entityManager) {
        this.repo = repo;
        this.gradingService = gradingService;
        this.entityManager = entityManager;
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
            gradingService.grade(submission);
        }
    }

    @Scheduled(fixedRate = 2 * 60 * 1000, initialDelay = 30 * 1000)
    @Transactional
    public void updateOutdatedResults() {
        if (minCheckedId > 0 && gradingService.isIdle()) {
            var outdated = findNextOutdatedResults();
            if (outdated.isEmpty()) {
                logger.info("All results up to date");
                minCheckedId = 0;
            } else {
                for (var submission : outdated) {
                    logger.info("Re-queuing submission {} due to outdated result", submission.getId());
                    gradingService.grade(submission);
                }
            }
        }
    }

    private List<Submission> findNextOutdatedResults() {
        // Enqueue outdated results few at a time, to avoid enqueuing a huge amount of
        // submissions at once after changing the result format. The grading service would
        // prioritize sensibly anyway, but this way, the more expressive "outdated" status is
        // kept for a longer time.
        var leftToCheck = new AtomicInteger(repo.countByIdLessThan(minCheckedId));
        try (var submissions = repo.findByIdLessThanOrderByIdDesc(minCheckedId)) {
            return submissions
                        .peek(s -> {
                            var left = leftToCheck.decrementAndGet();
                            if (left % 100 == 0) {
                                logger.info("Finding outdated results ({} left to check)", left);
                            }
                            minCheckedId = s.getId();
                        })
                        .filter(s -> {
                            var outdated = s.getStatus() == OUTDATED;
                            if (!outdated) {
                                entityManager.detach(s); // avoid out of memory
                            }
                            return outdated;
                        })
                        .limit(OUTDATED_BATCH_SIZE)
                        .toList();
        }
    }
}
