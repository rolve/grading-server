package ch.trick17.gradingserver;

import ch.trick17.gradingserver.model.GradingOptions.Compiler;
import ch.trick17.gradingserver.model.ProblemSetRepository;
import ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.target=7"})
public class MigrateV7Test {

    @Autowired
    ProblemSetRepository problemSetRepo;

    // see test data inserted by V6.1__test-config.sql

    @Test
    void migrate() {
        var problemSet = problemSetRepo.findById(2).orElseThrow(AssertionFailedError::new);

        assertEquals("Foo", problemSet.getName());
        assertEquals("aufgaben-01", problemSet.getProjectConfig().getProjectRoot());
        assertEquals(ProjectStructure.ECLIPSE, problemSet.getProjectConfig().getStructure());
        assertTrue(problemSet.getProjectConfig().getDependencies().isEmpty());

        assertEquals("class FooTest {}", problemSet.getGradingConfig().testClass());
        assertEquals(Compiler.JAVAC, problemSet.getGradingConfig().options().compiler());
        assertEquals(7, problemSet.getGradingConfig().options().repetitions());
        assertEquals(1000, problemSet.getGradingConfig().options().repTimeout().toMillis());
        assertEquals(5000, problemSet.getGradingConfig().options().testTimeout().toMillis());
        assertFalse(problemSet.getGradingConfig().options().permRestrictions());

        problemSet = problemSetRepo.findById(3).orElseThrow(AssertionFailedError::new);

        assertEquals("Bar", problemSet.getName());
        assertEquals("aufgaben-02", problemSet.getProjectConfig().getProjectRoot());
        assertEquals(ProjectStructure.MAVEN, problemSet.getProjectConfig().getStructure());
        assertTrue(problemSet.getProjectConfig().getDependencies().isEmpty());

        assertEquals("class BarTest {}", problemSet.getGradingConfig().testClass());
        assertEquals(Compiler.ECLIPSE, problemSet.getGradingConfig().options().compiler());
        assertEquals(5, problemSet.getGradingConfig().options().repetitions());
        assertEquals(300, problemSet.getGradingConfig().options().repTimeout().toMillis());
        assertEquals(1500, problemSet.getGradingConfig().options().testTimeout().toMillis());
        assertTrue(problemSet.getGradingConfig().options().permRestrictions());
    }
}
