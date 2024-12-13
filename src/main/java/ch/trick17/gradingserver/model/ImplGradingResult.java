package ch.trick17.gradingserver.model;

import ch.trick17.jtt.grader.Grader;
import ch.trick17.jtt.grader.Property;

import java.util.List;

import static ch.trick17.gradingserver.model.GradingResult.formatTestMethods;

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

    public List<String> passedTests() {
        return formatTestMethods(result.passedTests(), result.allTests());
    }

    public List<String> failedTests() {
        return formatTestMethods(result.failedTests(), result.allTests());
    }

    public double passedTestsRatio() {
        double totalTests = result.allTests().size();
        return totalTests > 0 ? result.passedTests().size() / totalTests : 0;
    }

    public int passedTestsPercent() {
        return (int) Math.floor(passedTestsRatio() * 100);
    }

    @Override
    public int compareTo(ImplGradingResult other) {
        return Double.compare(passedTestsRatio(), other.passedTestsRatio());
    }
}
