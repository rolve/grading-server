package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.Credentials;
import ch.trick17.gradingserver.GradingConfig;
import ch.trick17.gradingserver.GradingConfig.ProjectStructure;
import ch.trick17.gradingserver.GradingOptions;
import ch.trick17.gradingserver.GradingOptions.Compiler;
import ch.trick17.gradingserver.webapp.model.*;
import ch.trick17.gradingserver.webapp.service.GitLabGroupSolutionSupplier;
import ch.trick17.gradingserver.webapp.service.SolutionService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitlab4j.api.GitLabApiException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparingInt;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/courses/{courseId}/problem-sets")
public class ProblemSetController {

    private final ProblemSetRepository repo;
    private final CourseRepository courseRepo;
    private final HostCredentialsRepository credRepo;
    private final SolutionService solutionService;

    public ProblemSetController(ProblemSetRepository repo, CourseRepository courseRepo,
                                HostCredentialsRepository credRepo,
                                SolutionService solutionService) {
        this.repo = repo;
        this.courseRepo = courseRepo;
        this.credRepo = credRepo;
        this.solutionService = solutionService;
    }

    @GetMapping("/{id}/")
    public String problemSetPage(@PathVariable int courseId, @PathVariable int id, Model model) {
        var problemSet = findProblemSet(courseId, id);
        model.addAttribute("problemSet", problemSet);
        return "problem-sets/problem-set";
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
        return "problem-sets/register-solutions-gitlab";
    }

    @PostMapping("/{id}/register-solutions-gitlab")
    public String registerSolutionsGitLab(@PathVariable int courseId, @PathVariable int id,
                                          @RequestParam String host, @RequestParam String groupPath,
                                          @RequestParam String token,
                                          @RequestParam(defaultValue = "false") boolean ignoreAuthorless,
                                          HttpServletRequest req)
            throws GitLabApiException, GitAPIException {
        var problemSet = findProblemSet(courseId, id);

        var existingTokens = credRepo.findByHost(host);
        if (token.isBlank()) {
            // if no token is provided, try to use an existing one
            token = existingTokens.stream()
                    .max(comparingInt(HostCredentials::getId))
                    .map(HostCredentials::getCredentials)
                    .map(Credentials::getPassword)
                    .orElse(null);
        } else {
            // otherwise, if the provided token is new, store it
            var known = existingTokens.stream()
                    .map(c -> c.getCredentials().getPassword())
                    .anyMatch(token::equals);
            if (!known) {
                credRepo.save(new HostCredentials(host, new Credentials("", token)));
            }
        }

        problemSet.setRegisteringSolutions(true);
        repo.save(problemSet);

        var serverBaseUrl = String.format("%s://%s:%s", req.getScheme(), req.getServerName(), req.getServerPort());
        var supplier = new GitLabGroupSolutionSupplier("https://" + host, groupPath, token);
        supplier.setWebhookBaseUrl(serverBaseUrl);
        supplier.setIgnoringAuthorless(ignoreAuthorless);
        solutionService.registerSolutions(problemSet.getId(), supplier); // async
        return "redirect:./";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int courseId, @PathVariable int id) {
        var problemSet = findProblemSet(courseId, id);
        repo.delete(problemSet);
        return "redirect:../..";
    }
}
