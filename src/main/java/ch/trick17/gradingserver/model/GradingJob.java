package ch.trick17.gradingserver.model;

public record GradingJob(CodeLocation submission,
                         String username,
                         String password,
                         ProjectConfig projectConfig,
                         GradingConfig gradingConfig) {
}
