package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static ch.trick17.gradingserver.model.GradingConfig.ProjectStructure.ECLIPSE;
import static ch.trick17.gradingserver.model.GradingConfig.ProjectStructure.MAVEN;
import static ch.trick17.gradingserver.model.GradingOptions.Compiler.JAVAC;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class GradingServiceTest {

    @Autowired GradingService service;
    @Autowired SubmissionRepository submissionRepo;
    @Autowired AuthorRepository authorRepo;
    @Autowired
    UserRepository userRepo;

    Course course = new Course("OOPI2", new Term(2021, "FS"), "");
    GradingOptions options = new GradingOptions(JAVAC, 7,
            Duration.ofSeconds(5), Duration.ofSeconds(10), true);

    @Test
    void grade() throws Exception {
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
        var config = new GradingConfig(test, "", MAVEN, emptyList(), options);
        var problemSet = new ProblemSet(course, "Test", config, now(), false, false);

        var solution = new Solution(problemSet, "https://github.com/rolve/gui.git",
                "master", null, emptyList(), emptyList());
        var submission = new Submission(solution, "7f9225c2e7b20cb1ff51b0220687c75305341392",
                ZonedDateTime.now());
        submissionRepo.save(submission);

        service.grade(submission).get();

        var result = submission.getResult();
        assertNotNull(result);
        assertTrue(result.successful());
        assertNull(result.getError());
        assertEquals(List.of("compiled"), result.getProperties());
        assertEquals(List.of("testToRgbInt"), result.getPassedTests());
        assertEquals(emptyList(), result.getFailedTests());
    }

    @Test
    void gradePrivateRepo() throws Exception {
        var user = new User("user", "password", "User");
        userRepo.save(user);
        var token = new AccessToken(user, "https://gitlab.com", "5jiBFYSUisc-xbpCyLAW"); // read-only token from dummy user

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
        var config = new GradingConfig(test, "", ECLIPSE, emptyList(), options);
        var problemSet = new ProblemSet(course, "Test", config, now(), false, false);
        var solution = new Solution(problemSet, "https://gitlab.com/rolve/some-private-repo.git",
                "master", token, emptyList(), emptyList());
        var submission = new Submission(solution, "5f5ffff42176fc05bd3947ad2971712fb409ae9b",
                ZonedDateTime.now());
        submissionRepo.save(submission);

        service.grade(submission).get();

        var result = submission.getResult();
        assertNotNull(result);
        assertTrue(result.successful());
        assertNull(result.getError());
        assertEquals(List.of("compiled"), result.getProperties());
        assertEquals(List.of("testAdd"), result.getPassedTests());
        assertEquals(emptyList(), result.getFailedTests());
    }

    @Test
    void gradePrivateRepoMissingCredentials() throws Exception {
        AccessToken token = null;

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
        var config = new GradingConfig(test, "", ECLIPSE, emptyList(), options);
        var problemSet = new ProblemSet(course, "Test", config, now(), false, false);
        var solution = new Solution(problemSet, "https://gitlab.com/rolve/some-private-repo.git",
                "master", token, emptyList(), emptyList());
        var submission = new Submission(solution, "5f5ffff42176fc05bd3947ad2971712fb409ae9b",
                ZonedDateTime.now());
        submissionRepo.save(submission);

        service.grade(submission).get();

        var result = submission.getResult();
        assertNotNull(result);
        assertFalse(result.successful());
        assertNotNull(result.getError());
        assertTrue(result.getError().toLowerCase()
                .matches(".*ioexception.*authentication.*required.*"));
        assertNull(result.getProperties());
        assertNull(result.getPassedTests());
        assertNull(result.getFailedTests());
    }
}
