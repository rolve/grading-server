package ch.trick17.gradingserver.gradingservice.controller;

import ch.trick17.gradingserver.CodeLocation;
import ch.trick17.gradingserver.GradingConfig;
import ch.trick17.gradingserver.GradingConfig.ProjectStructure;
import ch.trick17.gradingserver.GradingOptions;
import ch.trick17.gradingserver.GradingResult;
import ch.trick17.gradingserver.gradingservice.model.GradingJob;
import ch.trick17.gradingserver.gradingservice.model.GradingJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;

import static ch.trick17.gradingserver.GradingOptions.Compiler.ECLIPSE;
import static java.util.regex.Pattern.compile;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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
        var options = new GradingOptions(ECLIPSE, 7, Duration.ofSeconds(6),
                Duration.ofMillis(10), true);
        var job = new GradingJob(code, new GradingConfig("foo.Foo", "/",
                ProjectStructure.ECLIPSE, options));
        var uri = rest.postForLocation("/api/v1/grading-jobs", job);
        var matcher = compile("/api/v1/grading-jobs/([a-f0-9]{32})").matcher(uri.getPath());
        assertTrue(matcher.matches(), uri.getPath());

        var response = rest.getForObject(uri, GradingJob.class);
        assertEquals(matcher.group(1), response.getId());
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
        var code = new CodeLocation("https://github.com/rolve/java-teaching-tools.git",
                "c61e753ad81f76cca7491efb441ce2fb915ef231");
        var options = new GradingOptions(ECLIPSE, 7, Duration.ofSeconds(6),
                Duration.ofMillis(10), true);
        var job = new GradingJob(code, new GradingConfig("foo.Foo", "/",
                ProjectStructure.ECLIPSE, options));
        var result = new GradingResult(null, List.of("foo", "bar"), List.of("fooTest"),
                List.of("bazTest"), "no details");
        job.setResult(result);
        repo.save(job);

        var response = rest.getForObject("/api/v1/grading-jobs/" + job.getId() + "/result", GradingResult.class);
        assertEquals(result, response);
    }
}
