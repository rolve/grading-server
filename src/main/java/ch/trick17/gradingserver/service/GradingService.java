package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.GradingServerProperties;
import ch.trick17.gradingserver.model.*;
import ch.trick17.jtt.grader.Grader;
import ch.trick17.jtt.memcompile.Compiler;
import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.sandbox.Whitelist;
import ch.trick17.jtt.testrunner.ExceptionDescription;
import ch.trick17.jtt.testrunner.TestResult;
import ch.trick17.jtt.testrunner.TestRunner;
import ch.trick17.jtt.testsuitegrader.TestSuiteGrader;
import org.slf4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class GradingService {

    private static final Logger logger = getLogger(GradingService.class);

    private final SubmissionRepository submissionRepo;
    private final SubmissionService submissionService;
    private final ProblemSetService problemSetService;
    private final JarFileWriter jarFileWriter;

    private final Grader grader;
    private final TestSuiteGrader testSuiteGrader;
    private final ThreadPoolExecutor executor;

    public GradingService(SubmissionRepository submissionRepo,
                          @Lazy SubmissionService submissionService,
                          @Lazy ProblemSetService problemSetService,
                          JarFileWriter jarFileWriter,
                          GradingServerProperties properties) {
        this.submissionRepo = submissionRepo;
        this.submissionService = submissionService;
        this.jarFileWriter = jarFileWriter;

        var args = properties.getTestRunnerVmArgs();
        var testRunner = new TestRunner(args.isEmpty() ? emptyList() : asList(args.split(" ")));
        grader = new Grader(testRunner);
        testSuiteGrader = new TestSuiteGrader(testRunner);

        var parallelism = max(2, getRuntime().availableProcessors() - 1);
        executor = new ThreadPoolExecutor(parallelism, parallelism, 30, SECONDS,
                new PriorityBlockingQueue<>());
        executor.allowCoreThreadTimeOut(true);
        this.problemSetService = problemSetService;
    }

    private sealed abstract static class Task extends FutureTask<Void> implements Comparable<Task> {
        public Task(Runnable runnable, Void result) {
            super(runnable, result);
        }
    }

    public boolean isIdle() {
        return executor.getActiveCount() == 0;
    }

    public Future<Void> prepare(ProblemSet problemSet, List<String> refTestSuite,
                                List<String> refImplementation) {
        problemSet.setGradingConfig(null);
        problemSetService.save(problemSet);
        var task = new PrepareTask(problemSet, refTestSuite, refImplementation);
        executor.execute(task);
        return task;
    }

    private final class PrepareTask extends Task {
        final ProblemSet problemSet;

        public PrepareTask(ProblemSet problemSet, List<String> refTestSuite,
                           List<String> refImplementation) {
            super(() -> doPrepare(problemSet, refTestSuite, refImplementation), null);
            this.problemSet = problemSet;
        }

        public int compareTo(Task task) {
            if (!(task instanceof PrepareTask other)) {
                // prepare tasks first
                return -1;
            } else {
                return Integer.compare(problemSet.getId(), other.problemSet.getId());
            }
        }
    }

    private void doPrepare(ProblemSet problemSet, List<String> refTestSuiteSources,
                           List<String> refImplementationSources) {
        logger.info("Preparing Problem Set {}", problemSet.getId());
        try {
            var refTestSuite = refTestSuiteSources.stream()
                    .map(InMemSource::fromString)
                    .toList();
            var refImplementation = refImplementationSources.stream()
                    .map(InMemSource::fromString)
                    .toList();
            var dependencies = jarFileWriter.write(problemSet.getProjectConfig().getDependencies());
            var task = testSuiteGrader.prepareTask(List.of(refImplementation),
                    refTestSuite, dependencies);
            var config = new TestSuiteGradingConfig(task);

            // set grading config in separate, @Transactional method:
            problemSetService.setConfig(problemSet, config);
            logger.info("Finished preparing Problem Set {}", problemSet.getId());
        } catch (Throwable e) {
            logger.error("Error while preparing Problem Set {}", problemSet.getId(), e);
        }
    }

    @Transactional
    public Future<Void> grade(Submission submission) {
        // remove previous result
        submission.clearResult();
        submission.setGradingStarted(false);
        submission = submissionRepo.save(submission);

        // initialize lazy collections needed for grading, in this thread
        var ignored1 = submission.getSolution().getProblemSet()
                .getProjectConfig().getDependencies().size();
        var ignored2 = submission.getSolution().getSubmissions().size();

        // then submit
        var task = new GradeTask(submission);
        executor.execute(task);
        return task;
    }

    private final class GradeTask extends Task {
        final Submission submission;

        GradeTask(Submission submission) {
            super(() -> doGrade(submission), null);
            this.submission = submission;
        }

        public int compareTo(Task task) {
            if (!(task instanceof GradeTask other)) {
                // prepare tasks first
                return 1;
            } else if (submission.isLatest() != other.submission.isLatest()) {
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
        logger.info("Grading Submission {} for \"{}\" ({})", submission.getId(),
                submission.getSolution().getProblemSet().getName(),
                submission.getCodeLocation());
        submissionService.setGradingStarted(submission, true);

        GradingResult result;
        try {
            result = tryGrade(submission);
            logger.info("Finished grading Submission {}", submission.getId());
        } catch (Throwable e) {
            logger.error("Error while grading Submission {}", submission.getId(), e);
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
                projectConfig.getSrcRoot(),
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
            return new ImplGradingResult(compress(result));
        } else if (gradingConfig instanceof TestSuiteGradingConfig config) {
            var testSuite = downloader.checkoutCode(
                    projectConfig.getTestRoot(),
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

            return new TestSuiteGradingResult(compress(testSuiteResult),
                    new ImplGradingResult(compress(implResult)));
        } else {
            throw new AssertionError("Unknown grading config type: " + gradingConfig);
        }
    }

    private Grader.Result compress(Grader.Result result) {
        return result.with(result.testResults().stream()
                .map(r -> compress(r))
                .toList());
    }

    private TestSuiteGrader.Result compress(TestSuiteGrader.Result result) {
        return result.with(result.refImplementationResults().stream()
                .map(r -> compress(r))
                .toList());
    }

    /**
     * Removes irrelevant or redundant information from the result: details for
     * exceptions thrown due to compilation errors and stack trace elements
     * corresponding to framework code.
     */
    private TestResult compress(TestResult r) {
        return r.with(r.exceptions().stream()
                .map(e -> reduceCompileErrorDetails(e))
                .map(e -> pruneStackTraces(e))
                .toList());
    }

    private ExceptionDescription reduceCompileErrorDetails(ExceptionDescription e) {
        if (e.className().equals("java.lang.Error") &&
            e.message().startsWith("Unresolved compilation problem")) {
            return e.with("Unresolved compilation problem", e.cause(),
                    List.of(e.stackTrace().getFirst()));
        }
        return e.with(e.message(), e.cause(), e.stackTrace());
    }

    private ExceptionDescription pruneStackTraces(ExceptionDescription e) {
        var pruned = e.stackTrace().stream()
                .dropWhile(s -> s.getClassName().startsWith("org.junit."))
                .toList()
                .reversed().stream()
                .dropWhile(s -> s.getClassName().startsWith("java.") ||
                                s.getClassName().startsWith("jdk.") ||
                                s.getClassName().startsWith("org.junit.") ||
                                s.getClassName().startsWith("ch.trick17.jtt."))
                .toList()
                .reversed();
        var cause = e.cause() == null ? null : pruneStackTraces(e.cause());
        return e.with(e.message(), cause, pruned);
    }
}
