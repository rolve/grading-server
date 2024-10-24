package ch.trick17.gradingserver;

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
import static ch.trick17.gradingserver.model.Role.LECTURER;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@AutoConfigureTestDatabase
class CoursePageIT {

    @Autowired
    CourseRepository repo;
    @Autowired
    UserRepository userRepo;

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
        var lecturer = userRepo.save(new User("lecturer", "password", "Lecturer", LECTURER));
        var course = new Course("OOPI2", new Term(2021, "FS"), "2Ibb1");
        course.getLecturers().add(lecturer);

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

        // anonymous users should not see hidden problem sets
        mockMvc.perform(get("/courses/" + course.getId() + "/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>OOPI2 (FS 2021 2Ibb1)</title>")))
                .andExpect(content().string(containsString("Regular Problem Set")))
                .andExpect(content().string(containsString("2021")))
                .andExpect(content().string(not(containsString("Hidden Problem Set"))))
                .andExpect(content().string(not(containsString("2022"))));

        // lecturers of the course should see them
        mockMvc.perform(get("/courses/" + course.getId() + "/").with(user(lecturer)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>OOPI2 (FS 2021 2Ibb1)</title>")))
                .andExpect(content().string(containsString("Regular Problem Set")))
                .andExpect(content().string(containsString("2021")))
                .andExpect(content().string(containsString("Hidden Problem Set")))
                .andExpect(content().string(containsString("2022")));

        // other lecturers should not see them
        var otherLecturer = userRepo.save(new User("other", "password", "Other", LECTURER));
        var otherCourse = new Course("Other", new Term(2021, "FS"), "2Ibb1");
        otherCourse.getLecturers().add(otherLecturer);
        repo.save(otherCourse);

        mockMvc.perform(get("/courses/" + course.getId() + "/").with(user(otherLecturer)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>OOPI2 (FS 2021 2Ibb1)</title>")))
                .andExpect(content().string(containsString("Regular Problem Set")))
                .andExpect(content().string(containsString("2021")))
                .andExpect(content().string(not(containsString("Hidden Problem Set"))))
                .andExpect(content().string(not(containsString("2022"))));
    }

    @Test
    @DirtiesContext
    void hiddenCoursePage() throws Exception {
        var lecturer = userRepo.save(new User("lecturer", "password", "Lecturer", LECTURER));
        var course = new Course("OOPI2", new Term(2021, "FS"), "2Ibb1");
        course.getLecturers().add(lecturer);
        course.setHidden(true);
        repo.save(course);

        // anonymous users should not see hidden courses
        mockMvc.perform(get("/courses/" + course.getId() + "/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")))
                .andExpect(content().string(is(emptyString())));

        // lecturers of a course should see it, of course
        mockMvc.perform(get("/courses/" + course.getId() + "/").with(user(lecturer)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>OOPI2 (FS 2021 2Ibb1)</title>")));

        var otherLecturer = userRepo.save(new User("other", "password", "Other", LECTURER));
        var otherCourse = new Course("Other", new Term(2021, "FS"), "2Ibb1");
        otherCourse.getLecturers().add(otherLecturer);
        repo.save(otherCourse);

        // other lecturers should not see it
        mockMvc.perform(get("/courses/" + course.getId() + "/").with(user(otherLecturer)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("OOPI2"))));
    }
}
