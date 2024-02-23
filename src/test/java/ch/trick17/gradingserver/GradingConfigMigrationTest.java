package ch.trick17.gradingserver;

import ch.trick17.gradingserver.model.GradingOptions.Compiler;
import ch.trick17.gradingserver.model.ImplGradingConfig;
import ch.trick17.gradingserver.model.ProblemSetRepository;
import ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class GradingConfigMigrationTest {

    @Autowired
    ProblemSetRepository problemSetRepo;

    // see test data inserted by V6.1__test-configs.sql

    @Test
    void migrate() {
        var problemSet = problemSetRepo.findById(10002).orElseThrow(AssertionFailedError::new);

        assertEquals("Foo", problemSet.getName());
        assertEquals("aufgaben-01", problemSet.getProjectConfig().getProjectRoot());
        assertEquals(ProjectStructure.ECLIPSE, problemSet.getProjectConfig().getStructure());
        assertTrue(problemSet.getProjectConfig().getDependencies().isEmpty());

        var gradingConfig = assertInstanceOf(ImplGradingConfig.class, problemSet.getGradingConfig());
        assertEquals("class FooTest {}", gradingConfig.testClass());
        assertEquals(Compiler.JAVAC, gradingConfig.options().compiler());
        assertEquals(7, gradingConfig.options().repetitions());
        assertEquals(1000, gradingConfig.options().repTimeout().toMillis());
        assertEquals(5000, gradingConfig.options().testTimeout().toMillis());
        assertFalse(gradingConfig.options().permRestrictions());

        problemSet = problemSetRepo.findById(10003).orElseThrow(AssertionFailedError::new);

        assertEquals("Bar", problemSet.getName());
        assertEquals("aufgaben-02", problemSet.getProjectConfig().getProjectRoot());
        assertEquals(ProjectStructure.MAVEN, problemSet.getProjectConfig().getStructure());
        assertTrue(problemSet.getProjectConfig().getDependencies().isEmpty());

        gradingConfig = assertInstanceOf(ImplGradingConfig.class, problemSet.getGradingConfig());
        assertEquals("class BarTest {}", gradingConfig.testClass());
        assertEquals(Compiler.ECLIPSE, gradingConfig.options().compiler());
        assertEquals(5, gradingConfig.options().repetitions());
        assertEquals(300, gradingConfig.options().repTimeout().toMillis());
        assertEquals(1500, gradingConfig.options().testTimeout().toMillis());
        assertTrue(gradingConfig.options().permRestrictions());
    }
}
