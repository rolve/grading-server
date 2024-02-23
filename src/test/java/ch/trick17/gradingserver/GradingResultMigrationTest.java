package ch.trick17.gradingserver;

import ch.trick17.gradingserver.model.ErrorResult;
import ch.trick17.gradingserver.model.ImplGradingResult;
import ch.trick17.gradingserver.model.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class GradingResultMigrationTest {

    @Autowired
    SubmissionRepository submissionRepo;

    // see test data inserted by V8.1__test-results.sql

    @Test
    void migrate() {
        var submission = submissionRepo.findById(10005).orElseThrow(AssertionFailedError::new);
        assertEquals("main", submission.getSolution().getBranch());
        var result = assertInstanceOf(ImplGradingResult.class, submission.getResult());
        assertEquals(List.of("compiled"), result.properties());
        assertEquals(List.of("fooTest"), result.passedTests());
        assertEquals(List.of("barTest", "bazTest"), result.failedTests());

        submission = submissionRepo.findById(10006).orElseThrow(AssertionFailedError::new);
        result = assertInstanceOf(ImplGradingResult.class, submission.getResult());
        assertEquals(List.of("compiled", "compile errors"), result.properties());
        assertEquals(List.of("FooTest.foo", "FooTest.bar"), result.passedTests());
        assertEquals(emptyList(), result.failedTests());

        submission = submissionRepo.findById(10007).orElseThrow(AssertionFailedError::new);
        result = assertInstanceOf(ImplGradingResult.class, submission.getResult());
        assertEquals(List.of("compiled", "test compile errors", "nondeterministic"), result.properties());
        assertEquals(emptyList(), result.passedTests());
        assertEquals(List.of("FooTest.foo", "FooTest.bar"), result.failedTests());

        submission = submissionRepo.findById(10008).orElseThrow(AssertionFailedError::new);
        var errorResult = assertInstanceOf(ErrorResult.class, submission.getResult());
        assertEquals("IOException: bla", errorResult.error());
    }
}
