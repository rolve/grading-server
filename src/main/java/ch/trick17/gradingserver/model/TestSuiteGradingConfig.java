package ch.trick17.gradingserver.model;

import ch.trick17.jtt.testsuitegrader.TestSuiteGrader.Task;

public record TestSuiteGradingConfig(Task task) implements GradingConfig {
}
