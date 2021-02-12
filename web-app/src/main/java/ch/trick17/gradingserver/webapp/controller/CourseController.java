package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.Course;
import ch.trick17.gradingserver.webapp.model.CourseRepository;
import ch.trick17.gradingserver.webapp.model.Term;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository repo;

    public CourseController(CourseRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{id}/")
    public String coursePage(@PathVariable int id, Model model) {
        model.addAttribute("course", repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
        return "/courses/course";
    }

    @GetMapping("/create")
    public String createCourse(Model model) {
        return "/courses/create";
    }

    @PostMapping("/create")
    public String createCourse(@RequestParam String name, @RequestParam String termKind,
                               @RequestParam int termYear, @RequestParam String qualifier) {
        var course = new Course(name, new Term(termYear, termKind), qualifier.isBlank() ? null : qualifier);
        course = repo.save(course);
        return "redirect:" + course.getId() + "/";
    }
}
