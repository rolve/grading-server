package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.GradingConfig;
import ch.trick17.gradingserver.GradingConfig.ProjectStructure;
import ch.trick17.gradingserver.GradingOptions;
import ch.trick17.gradingserver.GradingOptions.Compiler;
import ch.trick17.gradingserver.webapp.model.CourseRepository;
import ch.trick17.gradingserver.webapp.model.ProblemSet;
import ch.trick17.gradingserver.webapp.model.ProblemSetRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class ProblemSetController {

    private final ProblemSetRepository repo;
    private final CourseRepository courseRepo;

    public ProblemSetController(ProblemSetRepository repo, CourseRepository courseRepo) {
        this.repo = repo;
        this.courseRepo = courseRepo;
    }

    @GetMapping("/courses/{courseId}/problem-sets/{id}/")
    public String problemSetPage(@PathVariable int courseId, @PathVariable int id, Model model) {
        var problemSet = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (problemSet.getId() != courseId) {
            // silly, but allowing any course ID would be too
            throw new ResponseStatusException(NOT_FOUND);
        }
        model.addAttribute("problemSet", problemSet);
        return "/problem-sets/problem-set";
    }

    @GetMapping("/courses/{courseId}/problem-sets/add")
    public String addProblemSet(@PathVariable int courseId, Model model) {
        model.addAttribute("course", courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
        return "problem-sets/add";
    }

    @PostMapping("/courses/{courseId}/problem-sets/add")
    public String addProblemSet(@PathVariable int courseId, @RequestParam String name,
                                @RequestParam String deadlineDate, @RequestParam String deadlineTime,
                                @RequestParam MultipartFile testClassFile, @RequestParam String structure,
                                @RequestParam String projectRoot, @RequestParam String compiler,
                                @RequestParam int repetitions, @RequestParam int repTimeoutMs,
                                @RequestParam int testTimeoutMs) throws IOException {
        var course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        var date = LocalDate.parse(deadlineDate);
        var time = LocalTime.parse(deadlineTime);
        var testClass = new String(testClassFile.getBytes(), UTF_8);
        var config = new GradingConfig(testClass, projectRoot, ProjectStructure.valueOf(structure),
                new GradingOptions(Compiler.valueOf(compiler), repetitions,
                        Duration.ofMillis(repTimeoutMs), Duration.ofMillis(testTimeoutMs), true));
        var problemSet = new ProblemSet(course, name, config,
                ZonedDateTime.of(date, time, ZoneId.systemDefault()));
        courseRepo.save(course);
        return "redirect:../";
    }
}
