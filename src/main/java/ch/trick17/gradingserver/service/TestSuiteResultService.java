package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.Submission;
import ch.trick17.gradingserver.model.TestSuiteGradingConfig;
import ch.trick17.gradingserver.model.TestSuiteGradingResult;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Comparator.comparingInt;

@Service
public class TestSuiteResultService {

    private static final int SUGGESTIONS = 3;

    public List<String> getSuggestions(Submission submission, TestSuiteGradingResult result) {
        var config = (TestSuiteGradingConfig) submission.getSolution().getProblemSet().getGradingConfig();
        var missingTests = result.testSuiteResult().mutantResults().stream()
                .filter(r -> r.passed())
                .flatMap(r -> r.mutation().killers().stream())
                .distinct()
                .sorted(comparingInt(result.testSuiteResult().allTests()::indexOf))
                .toList();
        return missingTests.stream()
                .map(config.task().refTestDescriptions()::get)
                .limit(SUGGESTIONS)
                .toList();
    }
}
