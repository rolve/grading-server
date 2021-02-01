package ch.trick17.gradingserver.gradingservice.model;

import ch.trick17.jtt.grader.Grader;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * Runs {@link GradingJob}s, i.e., downloads a submission from
 * the specified code location, runs the {@link Grader} on it,
 * and persists the result.
 */
@Service
public class JobRunner {

    private final Grader grader;
    private final GradingJobRepository jobRepo;
    private final ExecutorService threadPool = newFixedThreadPool(getRuntime().availableProcessors());

    public JobRunner(Grader grader, GradingJobRepository jobRepo) {
        this.grader = grader;
        this.jobRepo = jobRepo;
    }

    public void submit(GradingJob job) {
        threadPool.submit(() -> run(job));
    }

    private void run(GradingJob job) {
        // TODO
    }
}
