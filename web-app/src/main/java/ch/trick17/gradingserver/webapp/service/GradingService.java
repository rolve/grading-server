package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.GradingJob;
import ch.trick17.gradingserver.webapp.WebAppProperties;
import ch.trick17.gradingserver.webapp.model.Submission;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static ch.trick17.gradingserver.webapp.service.GradingService.Status.DOWN;
import static ch.trick17.gradingserver.webapp.service.GradingService.Status.UP;
import static java.time.Instant.now;
import static java.util.Collections.newSetFromMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
public class GradingService {

    private static final Logger logger = getLogger(GradingService.class);

    @Lazy
    @Autowired
    private GradingService proxy;
    private final SubmissionService submissionService;
    private final WebClient client;

    private final Set<Integer> queued = newSetFromMap(new ConcurrentHashMap<>());

    public enum Status { UP, DOWN }
    private final AtomicReference<Status> status = new AtomicReference<>();
    private final AtomicReference<Instant> lastStatusCheck = new AtomicReference<>();

    public GradingService(@Lazy SubmissionService submissionService,
                          WebAppProperties props, WebClient.Builder clientBuilder) {
        this.submissionService = submissionService;

        var baseUrl = props.getGradingServiceBaseUrl();
        logger.info("Using grading service base URL: {}\n", baseUrl);
        client = clientBuilder.baseUrl(baseUrl).build();
        checkStatus();
    }

    @Bean
    Executor gradingExecutor() {
        // TODO: adjust to remote grading service
        return newFixedThreadPool(4); // allow 4 grading jobs at once
    }

    public Future<Void> grade(Submission submission) {
        // initialize lazy dependencies collection
        submission.getSolution().getProblemSet().getGradingConfig()
                .getDependencies().size();
        // then call async method through proxy
        return proxy.doGrade(submission);
    }

    @Async("gradingExecutor")
    Future<Void> doGrade(Submission submission) {
        var added = queued.add(submission.getId());
        if (!added) {
            return completedFuture(null);
        }

        var code = submission.getCodeLocation();
        var token = submission.getSolution().getAccessToken();
        var username = token == null ? null : "";
        var password = token == null ? null : token.getToken();
        var config = submission.getSolution().getProblemSet().getGradingConfig();
        var job = new GradingJob(code, username, password, config);

        var response = client.post()
                .uri("/api/v1/grading-jobs?waitUntilDone=true")
                .accept(APPLICATION_JSON)
                .bodyValue(job)
                .retrieve().bodyToMono(GradingJob.class);

        logger.info("Start grading submission {} (id {})",
                submission.shortCommitHash(), submission.getId());
        submissionService.setGradingStarted(submission, true);
        try {
            var result = response.block().getResult();
            // set result in separate, @Transactional method:
            submissionService.setResult(submission, result);
        } catch (RuntimeException e) {
            logger.warn("Exception while waiting for grading result of submission " +
                    submission.shortCommitHash() + " (id " + submission.getId() + ")", e);
            submissionService.setGradingStarted(submission, false);
        } finally {
            queued.remove(submission.getId());
        }
        return completedFuture(null);
    }

    public Status status() {
        var timeSinceCheck = Duration.between(lastStatusCheck.get(), now());
        if (timeSinceCheck.toSeconds() > 30) {
            checkStatus();
        }
        return status.get();
    }

    private void checkStatus() {
        lastStatusCheck.set(now());
        var request = client.get().uri("/api/v1/status").retrieve().toBodilessEntity();
        try {
            var response = request.blockOptional(Duration.ofSeconds(5));
            if (response.isPresent() && response.get().getStatusCode().is2xxSuccessful()) {
                status.set(UP);
                return;
            }
        } catch (Exception ignored) {}
        status.set(DOWN);
    }
}
