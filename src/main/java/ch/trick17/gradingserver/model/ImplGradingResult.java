package ch.trick17.gradingserver.model;

import ch.trick17.jtt.grader.Grader;
import ch.trick17.jtt.grader.Property;
import ch.trick17.jtt.testrunner.TestResult;

import java.util.List;
import java.util.function.Predicate;

import static ch.trick17.gradingserver.model.GradingResult.formatTestMethods;
import static ch.trick17.jtt.grader.Property.*;

public record ImplGradingResult(
        Grader.Result result) implements GradingResult, Comparable<ImplGradingResult> {

    @Override
    public List<String> properties() {
        return result.properties().stream()
                .map(Property::prettyName)
                .toList();
    }

    public boolean compiled() {
        return properties().contains("compiled");
    }

    public List<String> allTests() {
        return formatTestMethods(result.allTests(), result.allTests());
    }

    public int allTestsCount() {
        return result.allTests().size();
    }

    public List<String> passedTests() {
        return formatTestMethods(result.passedTests(), result.allTests());
    }

    public int passedTestsCount() {
        return result.passedTests().size();
    }

    public double passedTestsRatio() {
        double allTests = allTestsCount();
        return allTests > 0 ? result.passedTests().size() / allTests : 0;
    }

    public int passedTestsPercent() {
        return (int) Math.floor(passedTestsRatio() * 100);
    }

    public List<String> failedTests() {
        return formatTestMethods(result.failedTests(), result.allTests());
    }

    public List<String> detailsFor(String property) {
        if (COMPILE_ERRORS.prettyName().equals(property)) {
            return result.compileErrors();
        } else if (NONDETERMINISTIC.prettyName().equals(property)) {
            return formatTestMethodsWhere(r -> r.nonDeterm());
        } else if (TIMEOUT.prettyName().equals(property)) {
            return formatTestMethodsWhere(r -> r.timeout());
        } else if (OUT_OF_MEMORY.prettyName().equals(property)) {
            return formatTestMethodsWhere(r -> r.outOfMemory());
        } else if (INCOMPLETE_REPETITIONS.prettyName().equals(property)) {
            return formatTestMethodsWhere(r -> r.incompleteReps());
        } else if (ILLEGAL_OPERATION.prettyName().equals(property)) {
            return result.testResults().stream()
                    .flatMap(r -> r.illegalOps().stream())
                    .distinct()
                    .sorted()
                    .toList();
        }
        return null;
    }

    private List<String> formatTestMethodsWhere(Predicate<TestResult> filter) {
        var methods = result.testResults().stream()
                .filter(filter)
                .map(r -> r.method())
                .toList();
        return formatTestMethods(methods, result.allTests());
    }

    @Override
    public int compareTo(ImplGradingResult other) {
        return Double.compare(passedTestsRatio(), other.passedTestsRatio());
    }
}
