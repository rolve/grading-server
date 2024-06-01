package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.Submission;
import ch.trick17.gradingserver.model.TestSuiteGradingConfig;
import ch.trick17.gradingserver.model.TestSuiteGradingResult;
import ch.trick17.jtt.testrunner.TestMethod;
import ch.trick17.jtt.testsuitegrader.Mutation;
import org.springframework.stereotype.Service;

import java.util.*;

import static java.util.Collections.emptySet;

@Service
public class TestSuiteResultService {

    private static final int SUGGESTIONS = 3;

    public List<Suggestion> getSuggestions(Submission submission, TestSuiteGradingResult result) {
        var config = (TestSuiteGradingConfig) submission.getSolution().getProblemSet().getGradingConfig();

        var killedMutants = new HashMap<TestMethod, Set<Mutation>>();
        var totalMutants = new HashMap<TestMethod, Set<Mutation>>();
        for (var r : result.testSuiteResult().mutantResults()) {
            for (var test : r.mutation().killers()) {
                totalMutants.computeIfAbsent(test, t -> new HashSet<>())
                        .add(r.mutation());
                if (!r.passed()) {
                    killedMutants.computeIfAbsent(test, t -> new HashSet<>())
                            .add(r.mutation());
                }
            }
        }

        // consider only mutants that are not also killed by "weaker" tests
        // (earlier in the map of test descriptions)
        var tests = new ArrayList<>(config.task().refTestDescriptions().keySet());
        for (int i = 0; i < tests.size(); i++) {
            var killed = killedMutants.getOrDefault(tests.get(i), emptySet());
            var total = totalMutants.get(tests.get(i));
            for (int j = i + 1; j < tests.size(); j++) {
                killedMutants.getOrDefault(tests.get(j), new HashSet<>()).removeAll(killed);
                totalMutants.get(tests.get(j)).removeAll(total);
            }
        }

        return config.task().refTestDescriptions().entrySet().stream()
                .map(e -> new Suggestion(e.getValue(),
                        killedMutants.getOrDefault(e.getKey(), emptySet()).size(),
                        totalMutants.get(e.getKey()).size()))
                .filter(s -> s.mutantsKilled() < s.mutantsTotal())
                .limit(SUGGESTIONS)
                .toList();
    }

    public record Suggestion(
            String description,
            int mutantsKilled,
            int mutantsTotal) {}
}
