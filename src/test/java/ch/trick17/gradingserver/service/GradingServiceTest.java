package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.*;
import ch.trick17.gradingserver.model.GradingOptions.Compiler;
import ch.trick17.jtt.testrunner.TestMethod;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static ch.trick17.gradingserver.model.ProblemSet.DisplaySetting.WITH_SHORTENED_NAMES;
import static ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure.ECLIPSE;
import static ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure.MAVEN;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.transaction.TransactionDefinition.withDefaults;

@SpringBootTest
@AutoConfigureTestDatabase
class GradingServiceTest {

    @Autowired GradingService service;
    @Autowired SubmissionRepository submissionRepo;
    @Autowired ProblemSetRepository problemSetRepo;
    @Autowired UserRepository userRepo;
    @Autowired PlatformTransactionManager txManager;

    Course course = new Course("OOPI2", new Term(2021, "FS"), "");
    GradingOptions options = new GradingOptions(Compiler.ECLIPSE, 7,
            Duration.ofSeconds(5), Duration.ofSeconds(10), true);

    User user = new User("user", "password", "User");
    // read-only token from dummy user
    AccessToken token = new AccessToken(user, "https://gitlab.com", "glpat-pzyGzruzoVgEnQQPosZB");

    @DirtiesContext
    @ParameterizedTest
    @MethodSource
    void grade(String projectRoot, String commitHash) throws Exception {
        var test = """
                package gui;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                class ColorTest {
                    @Test
                    void testToRgbInt() {
                        assertEquals(0xFF8532, new Color(0xFF, 0x85, 0x32).toRgbInt());
                    }
                }""";
        var projectConfig = new ProjectConfig(projectRoot, MAVEN, null, emptyList());
        var gradingConfig = new ImplGradingConfig(test, options);
        var problemSet = new ProblemSet(course, "Test", projectConfig, gradingConfig,
                now(), WITH_SHORTENED_NAMES);

        var solution = new Solution(problemSet, "https://github.com/rolve/gui.git",
                "master", null, emptyList(), emptyList());
        var submission = submissionRepo.save(new Submission(solution, commitHash, now()));

        service.grade(submission).get();

        var tx = txManager.getTransaction(withDefaults());
        submission = submissionRepo.findById(submission.getId()).orElseThrow();
        Hibernate.initialize(submission.getResult());
        txManager.commit(tx);

        var result = assertInstanceOf(ImplGradingResult.class, submission.getResult());
        assertTrue(result.properties().contains("compiled"));
        assertEquals(List.of("testToRgbInt"), result.passedTests());
        assertEquals(emptyList(), result.failedTests());
    }

    static List<Arguments> grade() {
        return List.of(
                arguments("", "7f9225c2e7b20cb1ff51b0220687c75305341392"),
                arguments("gui", "082bfad2c587a14c140cb1e1eba54670654d4880"));
    }

    @DirtiesContext
    @Test
    void gradePrivateRepo() throws Exception {
        var test = """
                package foo;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                class FooTest {
                    @Test
                    void testAdd() {
                        assertEquals(3, Foo.add(1, 2));
                    }
                }""";
        var projectConfig = new ProjectConfig("", ECLIPSE, null, emptyList());
        var gradingConfig = new ImplGradingConfig(test, options);
        var problemSet = new ProblemSet(course, "Test", projectConfig, gradingConfig,
                now(), WITH_SHORTENED_NAMES);
        var solution = new Solution(problemSet, "https://gitlab.com/rolve/some-private-repo.git",
                "master", token, emptyList(), emptyList());
        userRepo.save(user);
        var submission = submissionRepo.save(
                new Submission(solution, "5f5ffff42176fc05bd3947ad2971712fb409ae9b", now()));

        service.grade(submission).get();

        var tx = txManager.getTransaction(withDefaults());
        submission = submissionRepo.findById(submission.getId()).orElseThrow();
        Hibernate.initialize(submission.getResult());
        txManager.commit(tx);

        var result = assertInstanceOf(ImplGradingResult.class, submission.getResult());
        assertEquals(List.of("compiled"), result.properties());
        assertEquals(List.of("testAdd"), result.passedTests());
        assertEquals(emptyList(), result.failedTests());
    }

    @DirtiesContext
    @Test
    void gradePrivateRepoMissingCredentials() throws Exception {
        var test = """
                package foo;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                class FooTest {
                    @Test
                    void testAdd() {
                        assertEquals(3, Foo.add(1, 2));
                    }
                }""";
        var projectConfig = new ProjectConfig("", ECLIPSE, null, emptyList());
        var gradingConfig = new ImplGradingConfig(test, options);
        var problemSet = new ProblemSet(course, "Test", projectConfig, gradingConfig,
                now(), WITH_SHORTENED_NAMES);
        AccessToken missingToken = null;
        var solution = new Solution(problemSet, "https://gitlab.com/rolve/some-private-repo.git",
                "master", missingToken, emptyList(), emptyList());
        var submission = submissionRepo.save(
                new Submission(solution, "5f5ffff42176fc05bd3947ad2971712fb409ae9b", now()));

        service.grade(submission).get();

        var tx = txManager.getTransaction(withDefaults());
        submission = submissionRepo.findById(submission.getId()).orElseThrow();
        Hibernate.initialize(submission.getResult());
        txManager.commit(tx);

        var result = assertInstanceOf(ErrorResult.class, submission.getResult());
        assertNotNull(result.error());
        assertTrue(result.error().toLowerCase()
                .matches(".*ioexception.*not.*authorized.*"));
    }

    @DirtiesContext
    @Test
    void prepareAndGradeTestSuite() throws ExecutionException, InterruptedException {
        var refTestSuite = List.of("""
                package foo;
                import org.junit.jupiter.api.Order;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                class FooTest {
                    /**
                     * Test that subtracting a number from itself returns 0.
                     */
                    @Order(1)
                    @Test
                    void subtractSame() {
                        assertEquals(0, Foo.subtract(1, 1));
                        assertEquals(0, Foo.subtract(2, 2));
                        assertEquals(0, Foo.subtract(3, 3));
                    }
                
                    /**
                     * Test the general case.
                     */
                    @Order(2)
                    @Test
                    void subtractGeneral() {
                        assertEquals(3, Foo.subtract(5, 2));
                        assertEquals(1, Foo.subtract(2, 1));
                        assertEquals(-5, Foo.subtract(0, 5));
                    }
                }""");
        var refImpl = List.of("""
                package foo;
                class Foo {
                    static int subtract(int a, int b) {
                        return a - b;
                    }
                }""");

        var projectConfig = new ProjectConfig("", ECLIPSE, null, emptyList());
        var problemSet = new ProblemSet(course, "Test", projectConfig, null,
                now(), WITH_SHORTENED_NAMES);
        var solution = new Solution(problemSet, "https://gitlab.com/rolve/some-private-repo.git",
                "master", token, emptyList(), emptyList());
        userRepo.save(user);
        var submission = submissionRepo.save(
                new Submission(solution, "58889314d0b75616cfa83f7ef89a51ecc5479654", now()));

        var tx = txManager.getTransaction(withDefaults());
        problemSet = problemSetRepo.findById(problemSet.getId()).orElseThrow();
        Hibernate.initialize(problemSet.getProjectConfig().getDependencies());
        txManager.commit(tx);

        service.prepare(problemSet, refTestSuite, refImpl).get();

        var config = problemSet.getGradingConfig();
        var task = assertInstanceOf(TestSuiteGradingConfig.class, config).task();
        assertTrue(task.mutations().size() >= 2);
        var descriptions = task.refTestDescriptions();
        assertEquals(2, descriptions.size());
        assertEquals("Test that subtracting a number from itself returns 0.",
                descriptions.get(new TestMethod("foo.FooTest", "subtractSame")));
        assertEquals("Test the general case.",
                descriptions.get(new TestMethod("foo.FooTest", "subtractGeneral")));

        service.grade(submission).get();

        tx = txManager.getTransaction(withDefaults());
        submission = submissionRepo.findById(submission.getId()).orElseThrow();
        Hibernate.initialize(submission.getResult());
        txManager.commit(tx);

        var result = assertInstanceOf(TestSuiteGradingResult.class, submission.getResult());
        assertFalse(result.testSuiteResult().emptyTestSuite());
        assertFalse(result.testSuiteResult().compilationFailed());
        assertEquals(0.5, result.testSuiteResult().mutantScore(), 0.001); // test suite covers one of two mutants

        assertEquals(1, result.implResult().passedTests().size()); // implementation passes the test
        assertEquals(0, result.implResult().failedTests().size());
        assertEquals(1.0, result.implResult().passedTestsRatio(), 0.001);
    }
}
