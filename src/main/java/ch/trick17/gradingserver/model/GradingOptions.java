package ch.trick17.gradingserver.model;

import java.time.Duration;

public record GradingOptions(
        Compiler compiler,
        int repetitions,
        Duration repTimeout,
        Duration testTimeout,
        boolean permRestrictions) {

    public enum Compiler {
        JAVAC, ECLIPSE
    }
}
