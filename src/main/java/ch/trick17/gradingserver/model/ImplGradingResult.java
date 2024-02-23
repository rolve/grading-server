package ch.trick17.gradingserver.model;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

public record ImplGradingResult(
        List<String> properties,
        List<String> passedTests,
        List<String> failedTests) implements GradingResult, Comparable<ImplGradingResult> {

    public boolean compiled() {
        return properties != null && properties.contains("compiled");
    }

    public List<String> allTests() {
        if (passedTests == null || failedTests == null) {
            return null;
        } else {
            return concat(passedTests.stream(), failedTests.stream())
                    .sorted()
                    .collect(toList());
        }
    }

    public int totalTests() {
        if (passedTests == null || failedTests == null) {
            return 0;
        } else {
            return passedTests.size() + failedTests.size();
        }
    }

    public double passedTestsRatio() {
        return passedTests.size() / (double) totalTests();
    }

    public int passedTestsPercent() {
        return (int) Math.floor(passedTestsRatio() * 100);
    }

    @Override
    public int compareTo(ImplGradingResult other) {
        return Double.compare(passedTestsRatio(), other.passedTestsRatio());
    }
}
