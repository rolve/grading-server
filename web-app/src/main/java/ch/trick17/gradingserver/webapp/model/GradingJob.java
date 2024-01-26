package ch.trick17.gradingserver.webapp.model;

public record GradingJob(CodeLocation submission,
                         String username,
                         String password,
                         GradingConfig config) {
}
