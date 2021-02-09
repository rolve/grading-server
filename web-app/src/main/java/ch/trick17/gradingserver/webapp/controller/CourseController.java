package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.Course;
import ch.trick17.gradingserver.webapp.model.CourseRepository;
import ch.trick17.gradingserver.webapp.model.Term;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class CourseController {

    private final CourseRepository repo;

    public CourseController(CourseRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/courses/{id}")
    public String coursePage(@PathVariable int id, Model model) {
        model.addAttribute("course", repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
        return "/courses/course";
    }

    @GetMapping("/courses/create")
    public String createCourse(Model model) {
        return "/courses/create";
    }

    @PostMapping("/courses/create")
    public String createCourse(@RequestParam String name, @RequestParam String termKind,
                               @RequestParam int termYear, @RequestParam String qualifier) {
        repo.save(new Course(name, new Term(termYear, termKind), qualifier.isBlank() ? null : qualifier));
        return "redirect:/";
    }
}
