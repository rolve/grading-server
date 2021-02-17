package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.CourseRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.data.domain.Sort.by;

@Controller
public class HomeController {

    private final CourseRepository courseRepo;

    public HomeController(CourseRepository courseRepo) {
        this.courseRepo = courseRepo;
    }

    @GetMapping("/")
    public String home(Model model) {
        var courses = courseRepo.findAll(by("term.year", "term.kind").descending()
                .and(by("name", "qualifier").ascending()));
        model.addAttribute("courses", courses);
        return "index";
    }

    @GetMapping("login")
    public String login(Model model) {
        return "login";
    }
}
