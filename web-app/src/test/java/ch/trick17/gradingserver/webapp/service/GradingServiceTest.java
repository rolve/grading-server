package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.GradingConfig;
import ch.trick17.gradingserver.GradingOptions;
import ch.trick17.gradingserver.gradingservice.GradingServiceApplication;
import ch.trick17.gradingserver.webapp.WebAppProperties;
import ch.trick17.gradingserver.webapp.model.*;
import ch.trick17.javaprocesses.JavaProcessBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static ch.trick17.gradingserver.GradingConfig.ProjectStructure.MAVEN;
import static ch.trick17.gradingserver.GradingOptions.Compiler.JAVAC;
import static java.lang.ProcessBuilder.Redirect.INHERIT;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class GradingServiceTest {

    @Autowired
    private GradingService service;

    @Autowired
    private SubmissionRepository submissionRepo;

    @Autowired
    private WebAppProperties properties;

    private Process gradingServiceApp;

    @BeforeEach
    void startGradingService() throws IOException {
        var port = properties.getGradingServicePort();

        gradingServiceApp = new JavaProcessBuilder(GradingServiceApplication.class)
                .autoExit(true)
                .addVmArgs("-Dserver.port=" + port, "-Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")
                .build()
                .redirectOutput(INHERIT)
                .redirectError(INHERIT)
                .start();

        while (true) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", port));
                return;
            } catch (IOException ignored) {}
        }
    }

    @Test
    void grade() throws ExecutionException, InterruptedException {
        var course = new Course("OOPI2", new Term(2021, "FS"), "");
        var options = new GradingOptions(JAVAC, 7, Duration.ofSeconds(6),
                Duration.ofMillis(10), true);
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
        var config = new GradingConfig(test, "", MAVEN, options);
        var problemSet = new ProblemSet(course, "Test", config, now(), false);
        var solution = new Solution(problemSet, "https://github.com/rolve/gui.git",
                List.of(new Author("rolve")), null);
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

    @AfterEach
    void stopGradingServiceApp() {
        gradingServiceApp.destroyForcibly();
    }
}
