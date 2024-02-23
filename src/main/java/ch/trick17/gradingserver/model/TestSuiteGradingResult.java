package ch.trick17.gradingserver.model;

import ch.trick17.jtt.testsuitegrader.MutantResult;
import ch.trick17.jtt.testsuitegrader.RefImplementationResult;
import ch.trick17.jtt.testsuitegrader.TestSuiteGrader;

import java.util.List;

public record TestSuiteGradingResult(
        TestSuiteGrader.Result wrapped) implements GradingResult, Comparable<TestSuiteGradingResult> {

    public boolean compiled() {
        return wrapped.compiled();
    }

    public boolean emptyTestSuite() {
        return wrapped.emptyTestSuite();
    }

    public List<RefImplementationResult> refImplementationResults() {
        return wrapped.refImplementationResults();
    }

    public List<MutantResult> mutantResults() {
        return wrapped.mutantResults();
    }

    public Double refImplementationScore() {
        return wrapped.refImplementationScore();
    }

    public Double mutantScore() {
        return wrapped.mutantScore();
    }

    public Double totalScore() {
        return wrapped.totalScore();
    }

    @Override
    public int compareTo(TestSuiteGradingResult o) {
        return Double.compare(totalScore(), o.totalScore());
    }
}
