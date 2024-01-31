package ch.trick17.gradingserver.controller;

import ch.trick17.gradingserver.model.CourseRepository;
import ch.trick17.gradingserver.model.User;
import ch.trick17.gradingserver.service.AccessController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.data.domain.Sort.by;

@Controller
public class HomeController {

    private final CourseRepository courseRepo;
    private final AccessController access;

    public HomeController(CourseRepository courseRepo, AccessController access) {
        this.courseRepo = courseRepo;
        this.access = access;
    }

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal User user) {
        var allCourses = courseRepo.findAll(
                by("term.year", "term.kind").descending().and(
                by("name", "qualifier").ascending()));
        var courses = allCourses.stream()
                .filter(course -> !course.isHidden() || access.check(course))
                .toList();
        model.addAttribute("courses", courses);
        return "index";
    }

    @GetMapping("login")
    public String login() {
        return "login";
    }
}
