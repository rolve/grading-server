package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

import static ch.trick17.gradingserver.webapp.model.Role.LECTURER;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository repo;
    private final UserRepository userRepo;

    public CourseController(CourseRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @GetMapping("/{id}/")
    public String coursePage(@PathVariable int id, Model model) {
        model.addAttribute("course", repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
        return "courses/course";
    }

    @GetMapping("/create")
    public String createCourse(@AuthenticationPrincipal User user,
                               Model model) {
        var possibleCoLecturers = userRepo.findAll().stream()
                .filter(u -> u.getId() != user.getId())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.includes(LECTURER)))
                .collect(toList());
        model.addAttribute("possibleCoLecturers", possibleCoLecturers);
        return "courses/create";
    }

    @PostMapping("/create")
    public String createCourse(@RequestParam String name, @RequestParam String termKind,
                               @RequestParam int termYear, @RequestParam String qualifier,
                               @RequestParam(required = false) Set<Integer> coLecturers,
                               @AuthenticationPrincipal User user) {
        var lecturers = new HashSet<>(Set.of(user));
        if (coLecturers != null) {
            lecturers.addAll(userRepo.findAllById(coLecturers));
        }
        if (!lecturers.stream().allMatch(u -> u.getRoles().stream().anyMatch(r -> r.includes(LECTURER)))) {
            throw new RuntimeException("cannot add non-LECTURER user as lecturer");
        }

        var course = new Course(name, new Term(termYear, termKind), qualifier.isBlank() ? null : qualifier);
        course.getLecturers().addAll(lecturers);
        course = repo.save(course);
        return "redirect:" + course.getId() + "/";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id) {
        repo.deleteById(id);
        return "redirect:../..";
    }
}
