package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.GradingServerProperties;
import ch.trick17.gradingserver.model.*;
import ch.trick17.jtt.grader.Grader;
import ch.trick17.jtt.grader.Property;
import ch.trick17.jtt.memcompile.Compiler;
import ch.trick17.jtt.sandbox.Whitelist;
import ch.trick17.jtt.testrunner.TestRunner;
import ch.trick17.jtt.testsuitegrader.TestSuiteGrader;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static ch.trick17.gradingserver.model.GradingResult.formatTestMethods;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.write;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.eclipse.jgit.util.FileUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class GradingService {

    private static final Path JOBS_ROOT = Path.of("grading-jobs").toAbsolutePath();

    private static final Logger logger = getLogger(GradingService.class);

    private final SubmissionRepository submissionRepo;
    private final SubmissionService submissionService;
    private final CodeDownloader downloader;

    private final Grader grader;
    private final TestSuiteGrader testSuiteGrader;
    @Lazy
    @Autowired
    private GradingService proxy;

    public GradingService(SubmissionRepository submissionRepo,
                          @Lazy SubmissionService submissionService,
                          CodeDownloader downloader, GradingServerProperties properties) {
        this.submissionRepo = submissionRepo;
        this.submissionService = submissionService;
        this.downloader = downloader;

        var testRunner = new TestRunner(properties.getTestRunnerVmArgs());
        grader = new Grader(testRunner);
        testSuiteGrader = new TestSuiteGrader(testRunner);
    }

    @Bean
    Executor gradingExecutor() {
        // TODO: Enable parallel grading again, after checking that the test runner is robust
        //  enough. Then, need to make sure that the naming of the grading directories is unique or
        //  prevent the same submission to be graded multiple times in parallel (which is useless
        //  anyway). Or, even better: implement full in-memory checkout and grading.
        return newFixedThreadPool(1);
    }

    public Future<Void> grade(Submission submission) {
        // remove previous result
        submission.clearResult();
        submission.setGradingStarted(false);
        submissionRepo.save(submission);
        // initialize lazy dependencies collection in this thread
        var ignored = submission.getSolution().getProblemSet()
                .getProjectConfig().getDependencies().size();
        // then call async method through proxy
        return proxy.doGrade(submission);
    }

    @Async("gradingExecutor")
    Future<Void> doGrade(Submission submission) {
        logger.info("Grading Submission {} ({})", submission.getId(), submission.getCodeLocation());
        submissionService.setGradingStarted(submission, true);

        GradingResult result;
        try {
            result = tryGrade(submission);
            logger.info("Finished grading Submission {}", submission.getId());
        } catch (Throwable e) {
            logger.error("Error while grading Submission " + submission.getId(), e);
            result = new ErrorResult(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // set result in separate, @Transactional method:
        submissionService.setResult(submission, result);
        return completedFuture(null);
    }

    private GradingResult tryGrade(Submission submission) throws IOException {
        var projectConfig = submission.getSolution().getProblemSet().getProjectConfig();

        var codeDir = JOBS_ROOT.resolve(submission.getId() + "-code");
        var depsDir = JOBS_ROOT.resolve(submission.getId() + "-deps");
        try {
            var token = submission.getSolution().getAccessToken();
            var username = token == null ? null : "";
            var password = token == null ? null : token.getToken();
            downloader.downloadCode(submission.getCodeLocation(), codeDir, username, password);

            var dependencies = writeDependencies(projectConfig.getDependencies(), depsDir);

            // TODO: handle case when project root is missing
            var srcDir = codeDir
                    .resolve(projectConfig.getProjectRoot())
                    .resolve(projectConfig.getStructure().srcDirPath);

            var gradingConfig = submission.getSolution().getProblemSet().getGradingConfig();
            if (gradingConfig instanceof ImplGradingConfig config) {
                // TODO: Refactor Grader to be able to apply package filter
                // TODO: Refactor Grader to not require a submission name
                var implSubmission = new Grader.Submission("", srcDir);
                var options = config.options();
                var task = Grader.Task.fromString(config.testClass())
                        .compiler(Compiler.valueOf(options.compiler().name()))
                        .repetitions(options.repetitions())
                        .timeouts(options.repTimeout(), options.testTimeout())
                        .permittedCalls(options.permRestrictions()
                                ? Whitelist.DEFAULT_WHITELIST_DEF
                                : null)
                        .dependencies(dependencies);

                var result = grader.grade(task, implSubmission);
                return convert(result);
            } else if (gradingConfig instanceof TestSuiteGradingConfig config) {
                var testDir = codeDir
                        .resolve(projectConfig.getProjectRoot())
                        .resolve(projectConfig.getStructure().testDirPath);
                var testSuiteSubmission = TestSuiteGrader.Submission.loadFrom(testDir, projectConfig.getPackageFilter());

                var testSuiteResult = testSuiteGrader.grade(config.task(), testSuiteSubmission, dependencies);
                if (testSuiteResult.emptyTestSuite() || testSuiteResult.compilationFailed()) {
                    return new TestSuiteGradingResult(testSuiteResult, null);
                }

                var task = new Grader.Task(testSuiteSubmission.testSuite(), emptyList())
                        .dependencies(dependencies);
                var implSubmission = new Grader.Submission("", srcDir);
                var implResult = grader.grade(task, implSubmission);

                return new TestSuiteGradingResult(testSuiteResult, convert(implResult));
            } else {
                throw new AssertionError("Unknown grading config type: " + gradingConfig);
            }
        } finally {
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
