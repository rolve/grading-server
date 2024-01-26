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
import static java.time.LocalDate.now;
import static java.util.function.Predicate.not;
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

    @GetMapping({"/create", "/{id}/edit"})
    public String createOrEdit(@PathVariable(required = false) Integer id,
                               @AuthenticationPrincipal User user, Model model) {
        model.addAttribute("create", id == null);
        if (id == null) {
            model.addAttribute("termKind", now().getMonthValue() < 7 ? "FS" : "HS");
            model.addAttribute("termYear", now().getYear());
        } else {
            var course = repo.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
            model.addAttribute("name", course.getName());
            model.addAttribute("termKind", course.getTerm().getKind());
            model.addAttribute("termYear", course.getTerm().getYear());
            model.addAttribute("qualifier", course.getQualifier());
            model.addAttribute("coLecturers", course.getLecturers().stream()
                    .filter(not(user::equals))
                    .toList());
        }
        var possibleCoLecturers = userRepo.findAll().stream()
                .filter(not(user::equals))
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.includes(LECTURER)))
                .toList();
        model.addAttribute("possibleCoLecturers", possibleCoLecturers);
        return "courses/edit";
    }

    @PostMapping({"/create", "/{id}/edit"})
    public String createOrEdit(@PathVariable(required = false) Integer id,
                               String name, String termKind, int termYear, String qualifier,
                               @RequestParam(required = false) Set<Integer> coLecturers,
                               @AuthenticationPrincipal User user) {
        var lecturers = new HashSet<>(Set.of(user));
        if (coLecturers != null) {
            lecturers.addAll(userRepo.findAllById(coLecturers));
        }
        if (!lecturers.stream().allMatch(u -> u.getRoles().stream()
                .anyMatch(r -> r.includes(LECTURER)))) {
            throw new RuntimeException("cannot add non-LECTURER user as lecturer");
        }

        var course = id == null
                ? new Course()
                : repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        course.setName(name);
        course.setTerm(new Term(termYear, termKind));
        course.setQualifier(qualifier.isBlank() ? null : qualifier);
        course.setLecturers(lecturers);
        course = repo.save(course);
        return "redirect:/courses/" + course.getId() + "/";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id) {
        repo.deleteById(id);
        return "redirect:../..";
    }
}
