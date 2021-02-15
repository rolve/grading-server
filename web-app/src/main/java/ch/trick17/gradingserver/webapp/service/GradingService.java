package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.GradingJob;
import ch.trick17.gradingserver.webapp.WebAppProperties;
import ch.trick17.gradingserver.webapp.model.HostCredentialsRepository;
import ch.trick17.gradingserver.webapp.model.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
public class GradingService {

    private static final Logger logger = LoggerFactory.getLogger(GradingService.class);

    private final HostCredentialsRepository credentialsRepo;
    private final SubmissionService submissionService;
    private final WebClient client;

    public GradingService(HostCredentialsRepository credentialsRepo,
                          @Lazy SubmissionService submissionService,
                          WebAppProperties props, WebClient.Builder clientBuilder) {
        this.credentialsRepo = credentialsRepo;
        this.submissionService = submissionService;

        var baseUrl = props.getGradingServiceBaseUrl();
        logger.info("Using grading service base URL: {}\n", baseUrl);
        client = clientBuilder.baseUrl(baseUrl).build();
    }

    @Bean
    Executor gradingExecutor() {
        return newFixedThreadPool(4); // allow 4 grading jobs at once
    }

    @Async("gradingExecutor")
    public Future<Void> grade(Submission submission) {
        var code = submission.getCodeLocation();
        var credentials = credentialsRepo.findLatestForUrl(code.getRepoUrl()).orElse(null);
        var config = submission.getSolution().getProblemSet().getGradingConfig();
        var job = new GradingJob(code, credentials, config);

        var response = client.post()
                .uri("/api/v1/grading-jobs?waitUntilDone=true")
                .accept(APPLICATION_JSON)
                .bodyValue(job)
                .retrieve().bodyToMono(GradingJob.class);

        submissionService.setGradingStarted(submission);
        var result = response.block().getResult();
        // set result in separate, @Transactional method:
        submissionService.setResult(submission, result);
        return completedFuture(null);
    }
}
