package ch.trick17.gradingserver.model;

import java.util.List;

/**
 * Used when the grading result retrieved from the database could not be deserialized because it
 * originates from an earlier version of the grading server. Allows low-effort migration of
 * result structures.
 */
public record OutdatedResult(
        String errorMessage) implements GradingResult {

    @Override
    public List<String> properties() {
        return List.of("outdated result");
    }

    @Override
    public List<String> detailsFor(String property) {
        return null;
    }
}
