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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static ch.trick17.gradingserver.model.GradingResult.formatTestMethods;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class GradingService {

    private static final Logger logger = getLogger(GradingService.class);

    private final SubmissionRepository submissionRepo;
    private final SubmissionService submissionService;
    private final JarFileWriter jarFileWriter;

    private final Grader grader;
    private final TestSuiteGrader testSuiteGrader;
    private final ThreadPoolExecutor executor;

    public GradingService(SubmissionRepository submissionRepo,
                          @Lazy SubmissionService submissionService,
                          JarFileWriter jarFileWriter,
                          GradingServerProperties properties) {
        this.submissionRepo = submissionRepo;
        this.submissionService = submissionService;
        this.jarFileWriter = jarFileWriter;

        var args = properties.getTestRunnerVmArgs();
        var testRunner = new TestRunner(args.isEmpty() ? emptyList() : asList(args.split(" ")));
        grader = new Grader(testRunner);
        testSuiteGrader = new TestSuiteGrader(testRunner);

        // TODO: Enable parallel grading again, after checking that the test runner is robust
        //  enough. Then, need to make sure that the naming of the grading directories is unique or
        //  prevent the same submission to be graded multiple times in parallel (which is useless
        //  anyway). Or, even better: implement full in-memory checkout and grading.
        executor = new ThreadPoolExecutor(1, 1, 0, MILLISECONDS, new PriorityBlockingQueue<>());
    }

    public boolean isIdle() {
        return executor.getActiveCount() == 0;
    }

    @Transactional
    public Future<Void> grade(Submission submission) {
        // remove previous result
        submission.clearResult();
        submission.setGradingStarted(false);
        submissionRepo.save(submission);

        // initialize lazy collections needed for grading, in this thread
        var ignored1 = submission.getSolution().getProblemSet()
                .getProjectConfig().getDependencies().size();
        var ignored2 = submission.getSolution().getSubmissions().size();

        // then submit
        var task = new GradingTask(submission);
        executor.execute(task);
        return task;
    }

    private class GradingTask extends FutureTask<Void> implements Comparable<GradingTask> {
        final Submission submission;

        GradingTask(Submission submission) {
            super(() -> doGrade(submission), null);
            this.submission = submission;
        }

        public int compareTo(GradingTask other) {
            if (submission.isLatest() != other.submission.isLatest()) {
                // latest submissions always have higher priority, allowing to re-grade older
                // submissions without affecting latest ones
                return submission.isLatest() ? -1 : 1;
            } else if (submission.isLatest()) {
                // among latest submissions, it's first come, first served, to avoid starvation
                return Integer.compare(submission.getId(), other.submission.getId());
            } else {
                // among older submissions, prioritize newer ones again, as they are probably
                // more relevant
                return Integer.compare(submission.getId(), other.submission.getId());
            }
        }
    }

    private void doGrade(Submission submission) {
        logger.info("Grading Submission {} ({})", submission.getId(), submission.getCodeLocation());
        submissionService.setGradingStarted(submission, true);

        GradingResult result;
        try {
            result = tryGrade(submission);
            logger.info("Finished grading Submission {}", submission.getId());
        } catch (Throwable e) {
            logger.error("Error while grading Submission " + submission.getId(), e);
            result = new ErrorResult(humanFriendlyMsg(e));
        }

        // set result in separate, @Transactional method:
        submissionService.setResult(submission, result);
    }

    private static String humanFriendlyMsg(Throwable e) {
        var className = e.getClass().getSimpleName();
        var descriptionParts = className
                .replaceAll("(Exception|Error)$", "")
                .split("(?<=[a-z])(?=[A-Z])");
        String type;
        if (descriptionParts.length >= 3) {
            // for descriptive exception names like NoSuchFileException,
            // MalformedInputException, etc. use only the description
            var joined = join(" ", descriptionParts);
            type = joined.charAt(0) + joined.substring(1).toLowerCase(ROOT);
        } else {
            // for simple exception names like IOException, ArithmeticException,
            // etc. use the full name
            type = className;
        }
        return type + ": " + e.getMessage();
    }

    private GradingResult tryGrade(Submission submission) throws IOException {
        var projectConfig = submission.getSolution().getProblemSet().getProjectConfig();

        var token = submission.getSolution().getAccessToken();
        var username = token == null ? null : "";
        var password = token == null ? null : token.getToken();
        var downloader = new CodeDownloader(submission.getCodeLocation(), username, password);

        var sources = downloader.checkoutCode(
                projectConfig.getSrcDirPath(),
                projectConfig.getPackageFilter());
        var dependencies = jarFileWriter.write(projectConfig.getDependencies());

        var gradingConfig = submission.getSolution().getProblemSet().getGradingConfig();
        if (gradingConfig instanceof ImplGradingConfig config) {
            var options = config.options();
            var task = Grader.Task.fromString(config.testClass())
                    .compiler(Compiler.valueOf(options.compiler().name()))
                    .repetitions(options.repetitions())
                    .timeouts(options.repTimeout(), options.testTimeout())
                    .permittedCalls(options.permRestrictions()
                            ? Whitelist.DEFAULT_WHITELIST_DEF
                            : null)
                    .dependencies(dependencies);

            var result = grader.grade(task, sources);
            return convert(result);
        } else if (gradingConfig instanceof TestSuiteGradingConfig config) {
            var testSuite = downloader.checkoutCode(
                    projectConfig.getTestDirPath(),
                    projectConfig.getPackageFilter());

            var testSuiteResult = testSuiteGrader.grade(config.task(), testSuite, dependencies);
            if (testSuiteResult.emptyTestSuite() || testSuiteResult.compilationFailed()) {
                return new TestSuiteGradingResult(testSuiteResult, null);
            }

            var task = new Grader.Task(testSuite, emptyList())
                    .permittedCalls(TestSuiteGrader.WHITELIST)
                    .restrictTests(true)
                    .dependencies(dependencies);
            var implResult = grader.grade(task, sources);

            return new TestSuiteGradingResult(testSuiteResult, convert(implResult));
        } else {
            throw new AssertionError("Unknown grading config type: " + gradingConfig);
        }
    }

    private ImplGradingResult convert(Grader.Result result) {
        var props = result.properties().stream()
                .map(Property::prettyName)
                .toList();
        return new ImplGradingResult(props, formatTestMethods(result.passedTests(), result.allTests()),
                formatTestMethods(result.failedTests(), result.allTests()));
    }
}
