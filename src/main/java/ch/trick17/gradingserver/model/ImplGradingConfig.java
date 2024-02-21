package ch.trick17.gradingserver.model;

public record ImplGradingConfig(
        String testClass,
        GradingOptions options) implements GradingConfig {
}
