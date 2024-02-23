package ch.trick17.gradingserver.model;

import ch.trick17.jtt.testsuitegrader.TestSuiteGrader;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;

public record TestSuiteGradingResult(
        TestSuiteGrader.Result testSuiteResult,
        ImplGradingResult implResult) implements GradingResult, Comparable<TestSuiteGradingResult> {

    @Override
    public int compareTo(TestSuiteGradingResult other) {
        return comparing(TestSuiteGradingResult::testSuiteResult,
                    comparing(TestSuiteGrader.Result::totalScore, nullsFirst(Double::compare)))
                .thenComparing(TestSuiteGradingResult::implResult)
                .compare(this, other);
    }
}
