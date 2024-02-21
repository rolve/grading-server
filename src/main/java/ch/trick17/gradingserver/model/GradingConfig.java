package ch.trick17.gradingserver.model;

public record GradingConfig(
        String testClass,
        GradingOptions options) {
}
