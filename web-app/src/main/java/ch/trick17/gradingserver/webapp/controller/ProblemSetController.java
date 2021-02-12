package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.GradingConfig;
import ch.trick17.gradingserver.GradingConfig.ProjectStructure;
import ch.trick17.gradingserver.GradingOptions;
import ch.trick17.gradingserver.GradingOptions.Compiler;
import ch.trick17.gradingserver.webapp.model.*;
import ch.trick17.gradingserver.webapp.service.GitLabGroupSolutionSupplier;
import org.gitlab4j.api.GitLabApiException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.*;
import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/courses/{courseId}/problem-sets")
public class ProblemSetController {

    private final ProblemSetRepository repo;
    private final CourseRepository courseRepo;
    private final AuthorRepository authorRepo;

    public ProblemSetController(ProblemSetRepository repo, CourseRepository courseRepo,
                                AuthorRepository authorRepo) {
        this.repo = repo;
        this.courseRepo = courseRepo;
        this.authorRepo = authorRepo;
    }

    @GetMapping("/{id}/")
    public String problemSetPage(@PathVariable int courseId, @PathVariable int id, Model model) {
        var problemSet = findProblemSet(courseId, id);
        model.addAttribute("problemSet", problemSet);
        return "/problem-sets/problem-set";
    }

    private ProblemSet findProblemSet(int courseId, int id) {
        var problemSet = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (problemSet.getCourse().getId() != courseId) {
            // silly, but allowing any course ID would be too
            throw new ResponseStatusException(NOT_FOUND);
        }
        return problemSet;
    }

    @GetMapping("/add")
    public String addProblemSet(@PathVariable int courseId, Model model) {
        model.addAttribute("course", courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
        return "problem-sets/add";
    }

    @PostMapping("/add")
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
        problemSet = repo.save(problemSet);
        return "redirect:" + problemSet.getId() + "/";
    }

    @GetMapping("/{id}/register-solutions-gitlab")
    public String registerSolutionsGitLab(@PathVariable int courseId, @PathVariable int id, Model model) {
        var problemSet = findProblemSet(courseId, id);
        model.addAttribute("problemSet", problemSet);
        return "/problem-sets/register-solutions-gitlab";
    }

    @PostMapping("/{id}/register-solutions-gitlab")
    public String registerSolutionsGitLab(@PathVariable int courseId, @PathVariable int id,
                                          @RequestParam String host, @RequestParam String groupPath,
                                          @RequestParam String token) throws GitLabApiException {
        var problemSet = findProblemSet(courseId, id);
        var existingSols = problemSet.getSolutions().stream()
                .map(Solution::getRepoUrl)
                .collect(toSet());
        // TODO: do in a background task
        var solutions = new GitLabGroupSolutionSupplier("https://" + host, groupPath, token).get();
        for (var info : solutions) {
            if (existingSols.contains(info.repoUrl())) {
                continue;
            }
            var authors = new ArrayList<Author>();
            for (var name : info.authorNames()) {
                var existing = authorRepo.findByName(name);
                if (existing.isPresent()) {
                    authors.add(existing.get());
                } else {
                    authors.add(new Author(name));
                }
            }
            var sol = new Solution(info.repoUrl(), authors);
            sol.setProblemSet(problemSet);
        }
        repo.save(problemSet);
        return "redirect:./";
    }
}
