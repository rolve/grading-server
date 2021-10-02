package ch.trick17.gradingserver.gradingservice.controller;

import ch.trick17.gradingserver.*;
import ch.trick17.gradingserver.GradingConfig.ProjectStructure;
import ch.trick17.gradingserver.gradingservice.model.GradingJob;
import ch.trick17.gradingserver.gradingservice.model.GradingJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static ch.trick17.gradingserver.GradingConfig.ProjectStructure.MAVEN;
import static ch.trick17.gradingserver.GradingOptions.Compiler.ECLIPSE;
import static ch.trick17.gradingserver.GradingOptions.Compiler.JAVAC;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptyList;
import static java.util.regex.Pattern.compile;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class GradingJobControllerTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private GradingJobRepository repo;

    @Test
    @DirtiesContext
    void create() {
        var code = new CodeLocation("https://github.com/rolve/java-teaching-tools.git",
                "c61e753ad81f76cca7491efb441ce2fb915ef231");
        var config = new GradingConfig("class Foo {}", "/", ProjectStructure.ECLIPSE,
                new GradingOptions(ECLIPSE, 7, Duration.ofSeconds(6), Duration.ofMillis(10), true));
        var job = new GradingJob(code, null, config);
        var location = rest.postForLocation("/api/v1/grading-jobs", job);
        var matcher = compile("/api/v1/grading-jobs/([a-f0-9]{32})").matcher(location.getPath());
        assertTrue(matcher.matches(), location.getPath());

        var response = rest.getForObject(location, GradingJob.class);
        assertEquals(matcher.group(1), response.getId());
        assertEquals(code, response.getSubmission());
        assertNull(response.getAccessToken());
        assertEquals(config, response.getConfig());
    }

    @Test
    @DirtiesContext
    void doNotStoreCredentials() {
        var code = new CodeLocation("https://github.com/rolve/java-teaching-tools.git",
                "c61e753ad81f76cca7491efb441ce2fb915ef231");
        var options = new GradingOptions(ECLIPSE, 7, Duration.ofSeconds(6),
                Duration.ofMillis(10), true);
        var job = new GradingJob(code, "pw",
                new GradingConfig("class Foo {}", "/", ProjectStructure.ECLIPSE, options));
        var uri = rest.postForLocation("/api/v1/grading-jobs", job);
        var matcher = compile("/api/v1/grading-jobs/([a-f0-9]{32})").matcher(uri.getPath());
        assertTrue(matcher.matches(), uri.getPath());

        var response = rest.getForObject(uri, GradingJob.class);
        assertEquals(matcher.group(1), response.getId());
        assertNull(response.getAccessToken());
    }

    @Test
    void createIncomplete() {
        var job = new GradingJob() {};
        var response = rest.postForEntity("/api/v1/grading-jobs", job, null);
        assertEquals(BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getNonexistent() {
        var response = rest.getForEntity("/api/v1/grading-jobs/0", GradingJob.class);
        assertEquals(NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @DirtiesContext
    void getResult() {
        var code = new CodeLocation("https://github.com/rolve/gui.git",
                "7f9225c2e7b20cb1ff51b0220687c75305341392");
        var options = new GradingOptions(ECLIPSE, 7, Duration.ofSeconds(6),
                Duration.ofMillis(10), true);
        var job = new GradingJob(code, null, new GradingConfig("class Foo {}", "/",
                ProjectStructure.ECLIPSE, options));
        var result = new GradingResult(null, List.of("foo", "bar"), List.of("fooTest"),
                List.of("bazTest"), "no details");
        job.setResult(result);
        repo.save(job);

        var response = rest.getForObject("/api/v1/grading-jobs/" + job.getId() + "/result", GradingResult.class);
        assertEquals(result, response);
    }

    @Test
    @DirtiesContext
    void getGradedResult() throws InterruptedException {
        var code = new CodeLocation("https://github.com/rolve/gui.git",
                "7f9225c2e7b20cb1ff51b0220687c75305341392");
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
        var job = new GradingJob(code, null, new GradingConfig(test, "", MAVEN, options));
        var uri = rest.postForLocation("/api/v1/grading-jobs", job);

        ResponseEntity<GradingResult> response;
        HttpStatus status;
        do {
            response = rest.getForEntity(uri + "/result", GradingResult.class);
            status = response.getStatusCode();
            assertTrue(Set.of(OK, NOT_FOUND).contains(status), status.toString());
            if (status == NOT_FOUND) {
                sleep(1000);
            }
        } while (status == NOT_FOUND);

        var result = response.getBody();
        assertNotNull(result);
        assertTrue(result.successful());
        assertNull(result.getError());
        assertEquals(List.of("compiled"), result.getProperties());
        assertEquals(List.of("testToRgbInt"), result.getPassedTests());
        assertEquals(emptyList(), result.getFailedTests());
    }

    @Test
    @DirtiesContext
    void createAndWait() {
        var code = new CodeLocation("https://github.com/rolve/gui.git",
                "7f9225c2e7b20cb1ff51b0220687c75305341392");
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
        var job = new GradingJob(code, null, config);
        var entity = rest.postForEntity("/api/v1/grading-jobs?waitUntilDone=true", job, GradingJob.class);

        var location = entity.getHeaders().getLocation();
        assertNotNull(location);
        var matcher = compile("/api/v1/grading-jobs/([a-f0-9]{32})").matcher(location.getPath());
        assertTrue(matcher.matches(), location.getPath());

        var response = entity.getBody();
        assertNotNull(response);
        assertEquals(matcher.group(1), response.getId());
        assertEquals(code, response.getSubmission());
        assertNull(response.getAccessToken());
        assertEquals(config, response.getConfig());

        var result = response.getResult();
        assertNotNull(result);
        assertTrue(result.successful());
        assertNull(result.getError());
        assertEquals(List.of("compiled"), result.getProperties());
        assertEquals(List.of("testToRgbInt"), result.getPassedTests());
        assertEquals(emptyList(), result.getFailedTests());
    }

    @Test
    @DirtiesContext
    void privateRepo() throws InterruptedException {
        var accessToken = "VBgo1xky7z87tKdzXacw"; // read-only deploy token
        var code = new CodeLocation("https://gitlab.com/rolve/some-private-repo.git",
                "5f5ffff42176fc05bd3947ad2971712fb409ae9b");
        var options = new GradingOptions(JAVAC, 7, Duration.ofSeconds(6),
                Duration.ofMillis(10), true);
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
        var job = new GradingJob(code, accessToken,
                new GradingConfig(test, "", ProjectStructure.ECLIPSE, options));
        var uri = rest.postForLocation("/api/v1/grading-jobs", job);

        ResponseEntity<GradingResult> response;
        HttpStatus status;
        do {
            response = rest.getForEntity(uri + "/result", GradingResult.class);
            status = response.getStatusCode();
            assertTrue(Set.of(OK, NOT_FOUND).contains(status), status.toString());
            if (status == NOT_FOUND) {
                sleep(1000);
            }
        } while (status == NOT_FOUND);

        var result = response.getBody();
        assertNotNull(result);
        assertTrue(result.successful());
        assertNull(result.getError());
        assertEquals(List.of("compiled"), result.getProperties());
        assertEquals(List.of("testAdd"), result.getPassedTests());
        assertEquals(emptyList(), result.getFailedTests());
    }

    @Test
    @DirtiesContext
    void privateRepoMissingCredentials() throws InterruptedException {
        var code = new CodeLocation("https://gitlab.com/rolve/some-private-repo.git",
                "5f5ffff42176fc05bd3947ad2971712fb409ae9b");
        var options = new GradingOptions(JAVAC, 7, Duration.ofSeconds(6),
                Duration.ofMillis(10), true);
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
        var job = new GradingJob(code, null, // <- no credentials
                new GradingConfig(test, "", ProjectStructure.ECLIPSE, options));
        var uri = rest.postForLocation("/api/v1/grading-jobs", job);

        ResponseEntity<GradingResult> response;
        HttpStatus status;
        do {
            response = rest.getForEntity(uri + "/result", GradingResult.class);
            status = response.getStatusCode();
            assertTrue(Set.of(OK, NOT_FOUND).contains(status), status.toString());
            if (status == NOT_FOUND) {
                sleep(1000);
            }
        } while (status == NOT_FOUND);

        var result = response.getBody();
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
