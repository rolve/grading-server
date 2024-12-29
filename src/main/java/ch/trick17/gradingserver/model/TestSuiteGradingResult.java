package ch.trick17.gradingserver.model;

import ch.trick17.jtt.testrunner.ExceptionDescription;
import ch.trick17.jtt.testrunner.TestMethod;
import ch.trick17.jtt.testrunner.TestResult;
import ch.trick17.jtt.testsuitegrader.TestSuiteGrader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static ch.trick17.gradingserver.model.GradingResult.formatTestMethods;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

public record TestSuiteGradingResult(
        TestSuiteGrader.Result testSuiteResult,
        ImplGradingResult implResult) implements GradingResult, Comparable<TestSuiteGradingResult> {

    @Override
    public List<String> properties() {
        var properties = new ArrayList<String>();
        if (implResult != null) {
            properties.addAll(implResult.properties());
        }
        if (testSuiteResult.emptyTestSuite()) {
            properties.add("empty test suite");
        }
        if (!testSuiteResult.compilationFailed()) {
            properties.add("compiled test suite");
        }
        if (!incorrectTests().isEmpty()) {
            properties.add("incorrect tests");
        }
        return properties;
    }

    @Override
    public List<String> detailsFor(String property) {
        return implResult.detailsFor(property);
    }

    public int percentFinished() {
        return (int) ((testSuiteScore() + implScore()) / 2 * 100);
    }

    public int testSuitePercent() {
        return (int) (testSuiteScore() * 100);
    }

    public int implPercent() {
        return (int) (implScore() * 100);
    }

    private double testSuiteScore() {
        var correctTestsRatio = 1 - testSuiteResult.incorrectTests().size() / (double) testSuiteResult.allTests().size();
        return testSuiteResult.mutantScore() * correctTestsRatio;
    }

    private double implScore() {
        var passedTestsRatio = implResult != null ? implResult.passedTestsRatio() : 0;
        return testSuiteScore() * passedTestsRatio;
    }

    public List<TestMethod> incorrectTests() {
        return testSuiteResult.incorrectTests().stream()
                .sorted(comparing(TestMethod::className)
                        .thenComparing(TestMethod::name))
                .toList();
    }

    public String format(TestMethod test) {
        return formatTestMethods(List.of(test), testSuiteResult.allTests()).getFirst();
    }

    public ExceptionDescription exceptionFor(TestMethod incorrectTest) {
        return refImplResultsFor(incorrectTest)
                .flatMap(r -> r.exceptions().stream())
                .findFirst().orElse(null);
    }

    public int exceptionLineNumberFor(TestMethod incorrectTest) {
        var stackTrace = exceptionFor(incorrectTest).stackTrace();
        // try to find the stack trace element for the test method first, then for the test class
        // in general (in case the error occurred in a setup method)
        return stackTrace.stream()
                .filter(e -> e.getClassName().equals(incorrectTest.className())
                             && e.getMethodName().equals(incorrectTest.name()))
                .findFirst()
                .or(() -> stackTrace.stream()
                        .filter(e -> e.getClassName().equals(incorrectTest.className()))
                        .findFirst())
                .map(e -> e.getLineNumber())
                .orElseThrow(AssertionError::new);
    }

    public List<String> illegalOpsFor(TestMethod incorrectTest) {
        return refImplResultsFor(incorrectTest)
                .flatMap(r -> r.illegalOps().stream())
                .distinct()
                .toList();
    }

    public boolean ranOutOfMemory(TestMethod incorrectTest) {
        return refImplResultsFor(incorrectTest).anyMatch(r -> r.outOfMemory());
    }

    public boolean hasTimedOut(TestMethod incorrectTest) {
        return refImplResultsFor(incorrectTest).anyMatch(r -> r.timeout());
    }

    private Stream<TestResult> refImplResultsFor(TestMethod incorrectTest) {
        return testSuiteResult.refImplementationResults().stream()
                .filter(r -> r.method().equals(incorrectTest));
    }

    @Override
    public int compareTo(TestSuiteGradingResult other) {
        return comparingInt(TestSuiteGradingResult::percentFinished)
                .thenComparingInt(TestSuiteGradingResult::testSuitePercent)
                .compare(this, other);
    }
}
