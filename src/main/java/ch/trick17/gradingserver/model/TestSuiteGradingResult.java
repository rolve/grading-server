package ch.trick17.gradingserver.model;

public record TestSuiteGradingResult(
        /* TODO */) implements GradingResult, Comparable<TestSuiteGradingResult> {

    @Override
    public int compareTo(TestSuiteGradingResult o) {
        // TODO
        return 0;
    }
}
