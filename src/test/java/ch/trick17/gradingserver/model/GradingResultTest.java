package ch.trick17.gradingserver.model;

import ch.trick17.jtt.grader.Grader;
import ch.trick17.jtt.testrunner.TestMethod;
import ch.trick17.jtt.testrunner.TestResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Stream.concat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GradingResultTest {

    @Test
    void betterFirst() {
        var sorted = Stream.of(
                new ImplGradingResult(asGraderResult(List.of("foo"), List.of("bar", "baz"))),
                new ImplGradingResult(asGraderResult(List.of("foo", "bar", "baz"), emptyList())),
                new ImplGradingResult(asGraderResult(List.of("foo", "baz"), List.of("bar")))
        ).sorted(GradingResult.betterFirst()).toList();

        assertEquals(List.of(
                new ImplGradingResult(asGraderResult(List.of("foo", "bar", "baz"), emptyList())),
                new ImplGradingResult(asGraderResult(List.of("foo", "baz"), List.of("bar"))),
                new ImplGradingResult(asGraderResult(List.of("foo"), List.of("bar", "baz")))
        ), sorted);

        // TODO: Test other types
    }

    private Grader.Result asGraderResult(List<String> passedTests, List<String> failedTests) {
        var testResults = concat(passedTests.stream(), failedTests.stream())
                .map(t -> new TestResult(new TestMethod("", t), passedTests.contains(t),
                        emptyList(), false, 0, false, false, false, emptyList(), emptyList()))
                .toList();
        return new Grader.Result(emptyList(), emptyList(), true, testResults);
    }
}
