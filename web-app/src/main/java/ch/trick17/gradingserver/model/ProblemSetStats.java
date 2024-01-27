package ch.trick17.gradingserver.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparingDouble;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.*;

public record ProblemSetStats(List<TestGroupStats> groups) {

    public static ProblemSetStats create(ProblemSet problemSet) {
        var results = problemSet.getSolutions().stream()
                .map(Solution::latestResult)
                .filter(Objects::nonNull)
                .collect(toList());
        var missing = problemSet.getSolutions().size() - results.size();

        var groupedTests = results.stream()
                .filter(GradingResult::successful)
                .flatMap(res -> res.getAllTests().stream())
                .collect(groupingBy(ProblemSetStats::testGroupFromName, toSet()));

        var groupStats = groupedTests.entrySet().stream().map(entry -> {
            var testStats = entry.getValue().stream().map(test -> {
                var passed = (int) results.stream()
                        .map(GradingResult::getPassedTests)
                        .filter(tests -> tests != null && tests.contains(test))
                        .count();
                var failed = results.size() - passed;
                return new TestStats(test, passed, failed, missing);
            }).collect(toList());
            return new TestGroupStats(entry.getKey().orElse(null), testStats);
        }).collect(toList());

        return new ProblemSetStats(groupStats);
    }

    private static Optional<String> testGroupFromName(String testName) {
        int lastDot;
        if ((lastDot = testName.lastIndexOf('.')) > 0) {
            return Optional.of(testName.substring(0, lastDot));
        } else {
            return Optional.empty();
        }
    }

    public ProblemSetStats {
        // sort by passed-to-submitted ratio
        groups = groups.stream()
                .sorted(reverseOrder(comparingDouble(TestGroupStats::passedToSubmittedRatio)))
                .collect(toUnmodifiableList());
    }

    public record TestGroupStats(String name, List<TestStats> tests) {
        public TestGroupStats {
            // sort by number of times a test was passed
            tests = tests.stream()
                    .sorted(reverseOrder(comparingInt(TestStats::passed)))
                    .collect(toUnmodifiableList());
        }
        public int totalPassed() {
            return tests.stream()
                    .mapToInt(TestStats::passed)
                    .sum();
        }
        public int totalFailed() {
            return tests.stream()
                    .mapToInt(TestStats::failed)
                    .sum();
        }
        public int totalMissing() {
            return tests.stream()
                    .mapToInt(TestStats::missing)
                    .sum();
        }
        public double passedToSubmittedRatio() {
            var passed = totalPassed();
            return passed / (double) (passed + totalFailed());
        }
    }

    public record TestStats(String name, int passed, int failed, int missing) {
        public double passedToSubmittedRatio() {
            return passed / (double) (passed + failed);
        }
    }
}
