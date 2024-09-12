package ch.trick17.gradingserver.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GradingResultTest {

    @Test
    void betterFirst() {
        var sorted = Stream.of(
                new ImplGradingResult(null, List.of("foo"), List.of("bar", "baz")),
                new ImplGradingResult(null, List.of("foo", "bar", "baz"), emptyList()),
                new ImplGradingResult(null, List.of("foo", "baz"), List.of("bar"))
        ).sorted(GradingResult.betterFirst()).toList();

        assertEquals(List.of(
                new ImplGradingResult(null, List.of("foo", "bar", "baz"), emptyList()),
                new ImplGradingResult(null, List.of("foo", "baz"), List.of("bar")),
                new ImplGradingResult(null, List.of("foo"), List.of("bar", "baz"))
        ), sorted);

        // TODO: Test other types
    }
}
