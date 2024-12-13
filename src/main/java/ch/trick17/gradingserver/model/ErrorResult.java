package ch.trick17.gradingserver.model;

import java.util.List;

import static java.util.Collections.emptyList;

public record ErrorResult(String error) implements GradingResult {
    @Override
    public List<String> properties() {
        return emptyList();
    }

    @Override
    public List<String> detailsFor(String property) {
        return null;
    }
}
