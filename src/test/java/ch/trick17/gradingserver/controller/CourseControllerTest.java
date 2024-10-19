package ch.trick17.gradingserver.controller;

import ch.trick17.gradingserver.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static ch.trick17.gradingserver.model.GradingOptions.Compiler.JAVAC;
import static ch.trick17.gradingserver.model.ProblemSet.DisplaySetting.HIDDEN;
import static ch.trick17.gradingserver.model.ProblemSet.DisplaySetting.WITH_SHORTENED_NAMES;
import static ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure.ECLIPSE;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@AutoConfigureTestDatabase
class CourseControllerTest {

    @Autowired
    private CourseRepository repo;

    MockMvc mockMvc;

    @BeforeEach
    void setUp(WebApplicationContext wac) {
        mockMvc = webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DirtiesContext
    void coursePage() throws Exception {
        var course = new Course("OOPI2", new Term(2021, "FS"), "2Ibb1");

        var projectConfig = new ProjectConfig("", ECLIPSE, null, emptyList());
        var gradingConfig = new ImplGradingConfig("class Foo {}",
                new GradingOptions(JAVAC, 3, Duration.ofSeconds(1), Duration.ofSeconds(5), true));
        new ProblemSet(course, "Regular Problem Set", projectConfig, gradingConfig,
                ZonedDateTime.of(LocalDateTime.of(2021, 3, 1, 23, 59), ZoneId.of("Europe/Zurich")),
                WITH_SHORTENED_NAMES);

        projectConfig = new ProjectConfig("", ECLIPSE, null, emptyList());
        gradingConfig = new ImplGradingConfig("class Foo {}",
                new GradingOptions(JAVAC, 3, Duration.ofSeconds(1), Duration.ofSeconds(5), true));
        new ProblemSet(course, "Hidden Problem Set", projectConfig, gradingConfig,
                ZonedDateTime.of(LocalDateTime.of(2022, 3, 1, 23, 59), ZoneId.of("Europe/Zurich")),
                HIDDEN);

        repo.save(course);

        mockMvc.perform(get("/courses/" + course.getId() + "/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>OOPI2 (FS 2021 2Ibb1)</title>")))
                .andExpect(content().string(containsString("Regular Problem Set")))
                .andExpect(content().string(containsString("2021")))
                .andExpect(content().string(not(containsString("Hidden Problem Set"))))
                .andExpect(content().string(not(containsString("2022"))));
    }
}
