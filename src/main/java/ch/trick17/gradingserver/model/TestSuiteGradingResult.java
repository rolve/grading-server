package ch.trick17.gradingserver.model;

import ch.trick17.jtt.testsuitegrader.TestSuiteGrader;

import java.util.ArrayList;
import java.util.List;

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
        return properties;
    }

    public int testSuitePercent() {
        return (int) (testSuiteResult.totalScore() * 100);
    }

    public int implPercent() {
        var implRatio = implResult != null ? implResult.passedTestsRatio() : 0;
        return (int) (100 * testSuiteResult.totalScore() * implRatio);
    }

    @Override
    public int compareTo(TestSuiteGradingResult other) {
        return comparing(TestSuiteGradingResult::testSuiteResult,
                    comparing(TestSuiteGrader.Result::totalScore, nullsFirst(Double::compare)))
                .thenComparing(TestSuiteGradingResult::implResult)
                .compare(this, other);
    }
}
