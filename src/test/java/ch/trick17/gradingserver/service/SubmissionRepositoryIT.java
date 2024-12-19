package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.*;
import ch.trick17.jtt.grader.Grader;
import ch.trick17.jtt.testrunner.ExceptionDescription;
import ch.trick17.jtt.testrunner.TestMethod;
import ch.trick17.jtt.testrunner.TestResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.Duration;
import java.util.List;

import static ch.trick17.gradingserver.model.GradingOptions.Compiler.JAVAC;
import static ch.trick17.gradingserver.model.ProblemSet.DisplaySetting.HIDDEN;
import static ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure.ECLIPSE;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@TestPropertySource("/test.properties")
public class SubmissionRepositoryIT {

    @Autowired
    SubmissionRepository repo;
    @Autowired
    PlatformTransactionManager txManager;

    @Test
    @DirtiesContext
    void save() {
        var tx = txManager.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        var course = new Course("OOPI2", new Term(2021, "FS"), "2Ibb1");
        var projectConfig = new ProjectConfig("", ECLIPSE, null, emptyList());
        var gradingConfig = new ImplGradingConfig("""
                class FooTest {
                    @org.junit.jupiter.api.Test
                    void test() {
                        org.junit.jupiter.api.Assertions.assertEquals((char) 0, 'a');
                    }
                }""",
                new GradingOptions(JAVAC, 3, Duration.ofSeconds(1), Duration.ofSeconds(5), true));
        var problemSet = new ProblemSet(course, "Task",
                projectConfig, gradingConfig, now(), HIDDEN);
        var solution = new Solution(problemSet, "https://example.com/", "main",
                null, emptyList(), emptyList());
        var receivedTime = now();
        var submission = new Submission(solution, "012345678", receivedTime);
        var id = repo.save(submission).getId();
        txManager.commit(tx);

        tx = txManager.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        var found = repo.findById(id).orElseThrow();
        assertEquals("012345678", found.getCommitHash());
        assertEquals(0, Duration.between(receivedTime, found.getReceivedTime()).toMillis());

        var exception = new ExceptionDescription("org. opentest4j. AssertionFailedError",
                "expected '\0' but was 'a'", null, emptyList());
        var testResult = new TestResult(new TestMethod("FooTest", "test"), true,
                List.of(exception), false, 7, false, false, false, emptyList(), emptyList());
        var result = new ImplGradingResult(new Grader.Result(emptyList(),
                emptyList(), true, List.of(testResult)));
        found.setResult(result);
        repo.save(found);
        txManager.commit(tx);

        found = repo.findById(id).orElseThrow();
        var foundResult = assertInstanceOf(ImplGradingResult.class, found.getResult());
        var foundException = foundResult.result().testResults().getFirst().exceptions().getFirst();
        // PostgreSQL does not support the NUL char in strings, so it is
        // replaced with '␀' before saving to the database
        assertEquals("expected '␀' but was 'a'", foundException.message());
    }
}
