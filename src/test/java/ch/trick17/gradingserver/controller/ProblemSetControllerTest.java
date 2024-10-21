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
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@AutoConfigureTestDatabase
class ProblemSetControllerTest {

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
    void problemSetPageRegular() throws Exception {
        var course = new Course("OOPI2", new Term(2021, "FS"), "2Ibb1");
        new ProblemSet(course, "First Task", projectConfig(), gradingConfig(),
                deadline(), WITH_SHORTENED_NAMES);
        repo.save(course);
        var problemSet = course.getProblemSets().getFirst();

        mockMvc.perform(get("/courses/" + course.getId() + "/problem-sets/" + problemSet.getId() + "/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("First Task")))
                .andExpect(content().string(containsString("OOPI2")))
                .andExpect(content().string(containsString("2021")));
    }

    @Test
    @DirtiesContext
    void problemSetPageHidden() throws Exception {
        var course = new Course("OOPI2", new Term(2021, "FS"), "2Ibb1");
        new ProblemSet(course, "First Task", projectConfig(), gradingConfig(),
                deadline(), HIDDEN);
        repo.save(course);
        var problemSet = course.getProblemSets().getFirst();

        mockMvc.perform(get("/courses/" + course.getId() + "/problem-sets/" + problemSet.getId() + "/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")))
                .andExpect(content().string(is(emptyString())));
    }

    @Test
    @DirtiesContext
    void problemSetPageHiddenCourse() throws Exception {
        var course = new Course("OOPI2", new Term(2021, "FS"), "2Ibb1");
        course.setHidden(true);
        new ProblemSet(course, "First Task", projectConfig(), gradingConfig(),
                deadline(), WITH_SHORTENED_NAMES);
        repo.save(course);
        var problemSet = course.getProblemSets().getFirst();

        mockMvc.perform(get("/courses/" + course.getId() + "/problem-sets/" + problemSet.getId() + "/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")))
                .andExpect(content().string(is(emptyString())));
    }

    private static ProjectConfig projectConfig() {
        return new ProjectConfig("", ECLIPSE, null, emptyList());
    }

    private static GradingConfig gradingConfig() {
        return new ImplGradingConfig("class Foo {}",
                new GradingOptions(JAVAC, 3, Duration.ofSeconds(1), Duration.ofSeconds(5), true));
    }

    private static ZonedDateTime deadline() {
        return ZonedDateTime.of(LocalDateTime.of(2021, 3, 1, 23, 59), ZoneId.of("Europe/Zurich"));
    }
}
