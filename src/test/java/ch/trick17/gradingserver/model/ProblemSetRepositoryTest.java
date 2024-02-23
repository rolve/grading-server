package ch.trick17.gradingserver.model;

import ch.trick17.jtt.testrunner.TestMethod;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class ProblemSetRepositoryTest {

    // see test data inserted by V9.1__test-suite-grading-config.sql

    @Autowired
    ProblemSetRepository repo;

    @Test
    void findByIdTestSuiteGradingConfig() {
        var problemSet = repo.findById(20002).orElseThrow(AssertionFailedError::new);
        var config = assertInstanceOf(TestSuiteGradingConfig.class, problemSet.getGradingConfig());
        var task = config.task();
        assertNotNull(task);
        var description = task.refTestDescriptions().get(new TestMethod("io.FirstNonEmptyLinesTest", "testZero"));
        assertEquals("`firstNonEmptyLines` mit `n = 0` aufrufen und prüfen, dass eine leere Liste zurückgegeben wird.", description);
    }
}
