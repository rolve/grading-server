package ch.trick17.gradingserver.gradingservice.service;

import ch.trick17.gradingserver.gradingservice.model.GradingJob;
import ch.trick17.gradingserver.gradingservice.model.GradingJobRepository;
import ch.trick17.gradingserver.model.GradingResult;
import ch.trick17.jtt.grader.Grader;
import ch.trick17.jtt.grader.SingleCodebase;
import ch.trick17.jtt.grader.Task;
import ch.trick17.jtt.grader.result.Property;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static ch.trick17.jtt.grader.ProjectStructure.valueOf;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toList;
import static org.eclipse.jgit.util.FileUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Runs {@link GradingJob}s, i.e., downloads a submission from
 * the specified code location, runs the {@link Grader} on it,
 * and persists the result.
 */
@Service
public class JobRunner {

    private static final Path CODE_ROOT = Path.of("grading-jobs-code").toAbsolutePath();

    private static final Logger logger = getLogger(JobRunner.class);

    private final CodeDownloader downloader;
    private final Grader grader;
    private final GradingJobRepository jobRepo;
    private final ExecutorService threadPool = newFixedThreadPool(getRuntime().availableProcessors());

    public JobRunner(CodeDownloader downloader, Grader grader, GradingJobRepository jobRepo) {
        this.downloader = downloader;
        this.grader = grader;
        this.jobRepo = jobRepo;
    }

    public void submit(GradingJob job) {
        submit(job, () -> {});
    }

    public void submit(GradingJob job, Runnable onCompletion) {
        threadPool.submit(() -> {
            run(job);
            onCompletion.run();
        });
    }

    private void run(GradingJob job) {
        GradingResult result;
        try {
            result = tryRun(job);
        } catch (Throwable e) {
            e.printStackTrace();
            var error = e.getClass().getSimpleName() + ": " + e.getMessage();
            result = new GradingResult(error, null, null, null, null);
        }
        job.setResult(result);
        jobRepo.save(job);
    }

    private GradingResult tryRun(GradingJob job) throws IOException {
        logger.info("Running grading job for {} (job id: {})",
                job.getSubmission(), job.getId());
        var repoDir = CODE_ROOT.resolve(job.getId());
        try {
            downloader.downloadCode(job.getSubmission(), repoDir, job.getUsername(), job.getPassword());
            var config = job.getConfig();
            var codebaseDir = repoDir.resolve(config.getProjectRoot());
            // TODO: handle case when project root is missing
            var codebase = new SingleCodebase(job.getId(), codebaseDir,
                    valueOf(config.getStructure().name()));
            var task = Task.fromString(config.getTestClass());

            var res = grader.run(codebase, List.of(task))
                    .get(0).submissionResults().get(0);

            var props = res.properties().stream()
                    .map(Property::prettyName).collect(toList());
            return new GradingResult(null, props, res.passedTests(), res.failedTests(), null);
        } finally {
            try {
                delete(repoDir.toFile(), RECURSIVE | RETRY);
            } catch (IOException e) {
                logger.warn("Could not delete " + repoDir, e);
            }
        }
    }
}
