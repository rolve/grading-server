package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.model.GradingJob;
import ch.trick17.gradingserver.webapp.model.GradingResult;
import ch.trick17.gradingserver.webapp.model.JarFile;
import ch.trick17.jtt.grader.Compiler;
import ch.trick17.jtt.grader.*;
import ch.trick17.jtt.grader.result.Property;
import ch.trick17.jtt.sandbox.Whitelist;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.write;
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

    private static final Path JOBS_ROOT = Path.of("grading-jobs").toAbsolutePath();

    private static final Logger logger = getLogger(JobRunner.class);

    private final AtomicLong idCounter = new AtomicLong(0);
    private final CodeDownloader downloader;
    private final Grader grader;

    public JobRunner(CodeDownloader downloader, Grader grader) {
        this.downloader = downloader;
        this.grader = grader;
    }

    public GradingResult run(GradingJob job) {
        try {
            return tryRun(job);
        } catch (Throwable e) {
            logger.error("Error while running grading job for " + job.submission(), e);
            var error = e.getClass().getSimpleName() + ": " + e.getMessage();
            return new GradingResult(error, null, null, null, null);
        }
    }

    private GradingResult tryRun(GradingJob job) throws IOException {
        var id = idCounter.getAndIncrement() + "";
        logger.info("Running grading job for {} (job id: {})",
                job.submission(), id);
        var codeDir = JOBS_ROOT.resolve(id + "-code");
        var depsDir = JOBS_ROOT.resolve(id + "-deps");
        try {
            var config = job.config();
            downloader.downloadCode(job.submission(), codeDir, job.username(), job.password());
            var dependencies = writeDependencies(config.getDependencies(), depsDir);

            var codebaseDir = codeDir.resolve(config.getProjectRoot());
            // TODO: handle case when project root is missing
            var codebase = new SingleCodebase(id, codebaseDir,
                    ProjectStructure.valueOf(config.getStructure().name()));

            var options = config.getOptions();
            var task = Task.fromString(config.getTestClass())
                    .compiler(Compiler.valueOf(options.getCompiler().name()))
                    .repetitions(options.getRepetitions())
                    .timeouts(options.getRepTimeout(), options.getTestTimeout())
                    .permittedCalls(options.getPermRestrictions()
                            ? Whitelist.DEFAULT_WHITELIST_DEF
                            : null)
                    .dependencies(dependencies);

            var res = grader.run(codebase, List.of(task))
                    .get(0).submissionResults().get(0);

            var props = res.properties().stream()
                    .map(Property::prettyName).collect(toList());
            return new GradingResult(null, props, res.passedTests(), res.failedTests(), null);
        } finally {
            try {
                delete(codeDir.toFile(), RECURSIVE | RETRY);
                delete(depsDir.toFile(), RECURSIVE | RETRY);
            } catch (IOException e) {
                logger.warn("Could not delete " + codeDir, e);
            }
        }
    }

    private List<Path> writeDependencies(List<JarFile> dependencies, Path dir)
            throws IOException {
        var paths = new ArrayList<Path>();
        createDirectories(dir);
        for (var dep : dependencies) {
            var path = dir.resolve(dep.getFilename());
            write(path, dep.getContent());
            paths.add(path);
        }
        return paths;
    }
}
