package ch.trick17.gradingserver.model;

import ch.trick17.jtt.testsuitegrader.TestSuiteGrader;

import java.util.ArrayList;
import java.util.List;

import static ch.trick17.gradingserver.model.GradingResult.formatTestMethods;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;

public record TestSuiteGradingResult(
        TestSuiteGrader.Result testSuiteResult,
        ImplGradingResult implResult) implements GradingResult, Comparable<TestSuiteGradingResult> {

    @Override
    public List<String> properties() {
        var properties = new ArrayList<String>();
        if (implResult != null) {
            properties.addAll(implResult.properties());
        }
        if (!testSuiteResult.compiled()) {
            properties.remove("compiled");
        }
        if (testSuiteResult.emptyTestSuite()) {
            properties.add("empty test suite");
        }
        if (!incorrectTests().isEmpty()) {
            properties.add("incorrect tests");
        }
        return properties;
    }

    public int testSuitePercent() {
        return (int) (testSuiteScore() * 100);
    }

    private double testSuiteScore() {
        var correctTestsRatio = 1 - testSuiteResult.incorrectTests().size() / (double) testSuiteResult.allTests().size();
        return testSuiteResult.mutantScore() * correctTestsRatio;
    }

    public int implPercent() {
        var implScore = implResult != null ? implResult.passedTestsRatio() : 0;
        return (int) (testSuiteScore() * implScore * 100);
    }

    public List<String> incorrectTests() {
        return formatTestMethods(testSuiteResult.incorrectTests(), testSuiteResult.allTests());
    }

    @Override
    public int compareTo(TestSuiteGradingResult other) {
        return comparing(TestSuiteGradingResult::testSuiteResult,
                    comparing(TestSuiteGrader.Result::mutantScore, nullsFirst(Double::compare)))
                .thenComparing(TestSuiteGradingResult::implResult)
                .compare(this, other);
    }
}
