package ch.trick17.gradingserver.controller;

import ch.trick17.gradingserver.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static ch.trick17.gradingserver.model.GradingConfig.ProjectStructure.ECLIPSE;
import static ch.trick17.gradingserver.model.GradingOptions.Compiler.JAVAC;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class CourseControllerTest {

    @Autowired
    private CourseRepository repo;

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DirtiesContext
    void coursePage() {
        var course = new Course("OOPI2", new Term(2021, "FS"), "2Ibb1");
        var config = new GradingConfig("class Foo {}", "", ECLIPSE,
                emptyList(), new GradingOptions(JAVAC, 3, Duration.ofSeconds(1), Duration.ofSeconds(5), true));
        var problemSet = new ProblemSet(course, "Woche 3", config, ZonedDateTime.of(
                LocalDateTime.of(2021, 3, 1, 23, 59), ZoneId.of("Europe/Zurich")),
                false, false);
        repo.save(course);

        var response = rest.getForObject("/courses/" + course.getId() + "/", String.class);
        assertThat(response).contains("<title>OOPI2 (FS 2021 2Ibb1)</title>");
        assertThat(response).contains(problemSet.getName()).contains(Integer.toString(problemSet.getDeadline().getYear()));
    }
}