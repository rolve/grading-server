package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.CourseRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class CourseController {

    private CourseRepository repo;

    public CourseController(CourseRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/courses/{id}")
    public String coursePage(@PathVariable int id, Model model) {
        model.addAttribute("course", repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
        return "/courses/course";
    }
}
