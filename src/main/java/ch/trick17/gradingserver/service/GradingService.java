package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.GradingJob;
import ch.trick17.gradingserver.model.Submission;
import ch.trick17.gradingserver.model.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static java.lang.Runtime.getRuntime;
import static java.util.Collections.newSetFromMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newFixedThreadPool;

@Service
public class GradingService {

    @Lazy
    @Autowired
    private GradingService proxy;
    private final SubmissionRepository submissionRepo;
    private final SubmissionService submissionService;
    private final JobRunner jobRunner;

    private final Set<Integer> queued = newSetFromMap(new ConcurrentHashMap<>());

    public GradingService(SubmissionRepository submissionRepo,
                          @Lazy SubmissionService submissionService,
                          JobRunner jobRunner) {
        this.submissionRepo = submissionRepo;
        this.submissionService = submissionService;
        this.jobRunner = jobRunner;
    }

    @Bean
    Executor gradingExecutor() {
        var processors = getRuntime().availableProcessors();
        return newFixedThreadPool(Math.max(1, processors - 1));
    }

    public Future<Void> grade(Submission submission) {
        // remove previous result
        submission.clearResult();
        submission.setGradingStarted(false);
        submissionRepo.save(submission);
        // initialize lazy dependencies collection
        var ignored = submission.getSolution().getProblemSet()
                .getProjectConfig().getDependencies().size();
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
        var projectConfig = submission.getSolution().getProblemSet().getProjectConfig();
        var gradingConfig = submission.getSolution().getProblemSet().getGradingConfig();
        var job = new GradingJob(code, username, password, projectConfig, gradingConfig);

        submissionService.setGradingStarted(submission, true);

        var result = jobRunner.run(job);
        // set result in separate, @Transactional method:
        submissionService.setResult(submission, result);

        queued.remove(submission.getId());
        return completedFuture(null);
    }
}
