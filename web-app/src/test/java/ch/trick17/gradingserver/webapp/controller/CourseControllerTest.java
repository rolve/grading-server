package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.Course;
import ch.trick17.gradingserver.webapp.model.CourseRepository;
import ch.trick17.gradingserver.webapp.model.Term;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class CourseControllerTest {

    @Autowired
    private CourseRepository repo;

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DirtiesContext
    void coursePage() {
        var course = new Course("OOPI2", new Term(2021, "FS"), "2Ibb1");
        repo.save(course);

        var response = rest.getForObject("/courses/" + course.getId(), String.class);
        assertThat(response).contains("<title>" + course.fullName() + "</title>");
    }
}
