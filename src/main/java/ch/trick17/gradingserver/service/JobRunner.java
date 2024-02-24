package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.*;
import ch.trick17.jtt.grader.Grader;
import ch.trick17.jtt.grader.Property;
import ch.trick17.jtt.memcompile.Compiler;
import ch.trick17.jtt.sandbox.Whitelist;
import ch.trick17.jtt.testrunner.TestMethod;
import ch.trick17.jtt.testsuitegrader.TestSuiteGrader;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static ch.trick17.gradingserver.model.GradingResult.formatTestMethods;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.write;
import static java.util.Collections.emptyList;
import static org.eclipse.jgit.util.FileUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Runs {@link GradingJob}s, i.e., downloads a submission from the specified
 * code location, runs the {@link Grader} on it, and persists the result.
 */
@Service
public class JobRunner {

    private static final Path JOBS_ROOT = Path.of("grading-jobs").toAbsolutePath();

    private static final Logger logger = getLogger(JobRunner.class);

    private final AtomicLong idCounter = new AtomicLong(0);
    private final CodeDownloader downloader;
    private final Grader grader = new Grader();
    private final TestSuiteGrader testSuiteGrader = new TestSuiteGrader();

    public JobRunner(CodeDownloader downloader) {
        this.downloader = downloader;
    }

    public GradingResult run(GradingJob job) {
        try {
            return tryRun(job);
        } catch (Throwable e) {
            logger.error("Error while running grading job for " + job.submission(), e);
            return new ErrorResult(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private GradingResult tryRun(GradingJob job) throws IOException {
        var id = idCounter.getAndIncrement() + "";
        logger.info("Running grading job {} for {}", id, job.submission());
        var codeDir = JOBS_ROOT.resolve(id + "-code");
        var depsDir = JOBS_ROOT.resolve(id + "-deps");
        try {
            downloader.downloadCode(job.submission(), codeDir, job.username(), job.password());
            var dependencies = writeDependencies(job.projectConfig().getDependencies(), depsDir);

            // TODO: handle case when project root is missing
            var srcDir = codeDir
                    .resolve(job.projectConfig().getProjectRoot())
                    .resolve(job.projectConfig().getStructure().srcDirPath);

            if (job.gradingConfig() instanceof ImplGradingConfig gradingConfig) {
                var submission = new Grader.Submission(id, srcDir);
                var options = gradingConfig.options();
                var task = Grader.Task.fromString(gradingConfig.testClass())
                        .compiler(Compiler.valueOf(options.compiler().name()))
                        .repetitions(options.repetitions())
                        .timeouts(options.repTimeout(), options.testTimeout())
                        .permittedCalls(options.permRestrictions()
                                ? Whitelist.DEFAULT_WHITELIST_DEF
                                : null)
                        .dependencies(dependencies);

                var result = grader.grade(task, submission);
                return convert(result);
            } else if (job.gradingConfig() instanceof TestSuiteGradingConfig gradingConfig) {
                var testDir = codeDir
                        .resolve(job.projectConfig().getProjectRoot())
                        .resolve(job.projectConfig().getStructure().testDirPath);
                var testSuiteSubmission = TestSuiteGrader.Submission.loadFrom(testDir);

                var testSuiteResult = testSuiteGrader.grade(gradingConfig.task(), testSuiteSubmission, dependencies);
                if (!testSuiteResult.compiled() || testSuiteResult.emptyTestSuite()) {
                    return new TestSuiteGradingResult(testSuiteResult, null);
                }

                var task = new Grader.Task(testSuiteSubmission.testSuite(), emptyList())
                        .dependencies(dependencies);
                var implSubmission = new Grader.Submission(id, srcDir);
                var implResult = grader.grade(task, implSubmission);

                return new TestSuiteGradingResult(testSuiteResult, convert(implResult));
            } else {
                throw new AssertionError("Unknown grading config type: " + job.gradingConfig());
            }
        } finally {
            logger.info("Job {} finished", id);
            try {
                delete(codeDir.toFile(), RECURSIVE | RETRY);
                delete(depsDir.toFile(), RECURSIVE | RETRY);
            } catch (IOException e) {
                logger.warn("Could not delete " + codeDir, e);
            }
        }
    }

    private ImplGradingResult convert(Grader.Result result) {
        var props = result.properties().stream()
                .map(Property::prettyName)
                .toList();
        return new ImplGradingResult(props, formatTestMethods(result.passedTests(), result.allTests()),
                formatTestMethods(result.failedTests(), result.allTests()));
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
