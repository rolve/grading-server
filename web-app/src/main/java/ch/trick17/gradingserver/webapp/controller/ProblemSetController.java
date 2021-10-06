package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.GradingConfig;
import ch.trick17.gradingserver.GradingConfig.ProjectStructure;
import ch.trick17.gradingserver.GradingOptions;
import ch.trick17.gradingserver.GradingOptions.Compiler;
import ch.trick17.gradingserver.webapp.WebAppProperties;
import ch.trick17.gradingserver.webapp.model.*;
import ch.trick17.gradingserver.webapp.service.GitLabGroupSolutionSupplier;
import ch.trick17.gradingserver.webapp.service.ProblemSetService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitlab4j.api.GitLabApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.*;
import java.util.Set;

import static ch.trick17.gradingserver.webapp.model.Solution.byCommitHash;
import static ch.trick17.gradingserver.webapp.model.Solution.byResult;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/courses/{courseId}/problem-sets")
public class ProblemSetController {

    private static final Set<String> LOCALHOST = Set.of("localhost", "127.0.0.1", "[::1]");

    private static final Logger logger = LoggerFactory.getLogger(ProblemSetController.class);

    private final ProblemSetRepository repo;
    private final CourseRepository courseRepo;
    private final AccessTokenRepository accessTokenRepo;
    private final ProblemSetService problemSetService;

    private final String defaultGitLabHost;

    public ProblemSetController(ProblemSetRepository repo, CourseRepository courseRepo,
                                AccessTokenRepository accessTokenRepo,
                                ProblemSetService problemSetService,
                                WebAppProperties props) {
        this.repo = repo;
        this.courseRepo = courseRepo;
        this.accessTokenRepo = accessTokenRepo;
        this.problemSetService = problemSetService;
        this.defaultGitLabHost = props.getDefaultGitLabHost();
    }

    @GetMapping("/{id}/")
    @PreAuthorize("!this.findProblemSet(#courseId, #id).hidden || isAuthenticated()")
    public String problemSetPage(@PathVariable int courseId, @PathVariable int id, Model model) {
        var problemSet = findProblemSet(courseId, id);
        model.addAttribute("problemSet", problemSet);
        // somehow, sorting in the template doesn't work, so do it here:
        var sort = problemSet.isAnonymous() ? byCommitHash() : byResult();
        var solutions = problemSet.getSolutions().stream()
                .sorted(sort).collect(toList());
        model.addAttribute("solutions", solutions);
        return "problem-sets/problem-set";
    }

    public ProblemSet findProblemSet(int courseId, int id) {
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
                                @RequestParam(defaultValue = "false") boolean anonymous,
                                @RequestParam(defaultValue = "false") boolean hidden,
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
                ZonedDateTime.of(date, time, ZoneId.systemDefault()), anonymous, hidden);
        problemSet = repo.save(problemSet);
        return "redirect:" + problemSet.getId() + "/";
    }

    @GetMapping("/{id}/register-solutions-gitlab")
    public String registerSolutionsGitLab(@PathVariable int courseId, @PathVariable int id, Model model) {
        var problemSet = findProblemSet(courseId, id);
        model.addAttribute("problemSet", problemSet);
        model.addAttribute("host", defaultGitLabHost);
        model.addAttribute("ignoreAuthorless", true);
        return "problem-sets/register-solutions-gitlab";
    }

    @PostMapping("/{id}/register-solutions-gitlab")
    public String registerSolutionsGitLab(@PathVariable int courseId, @PathVariable int id,
                                          @RequestParam String host, @RequestParam String groupPath,
                                          @RequestParam String token,
                                          @RequestParam(defaultValue = "false") boolean ignoreAuthorless,
                                          HttpServletRequest req,
                                          @AuthenticationPrincipal User user,
                                          Model model) // only used if unsuccessful
            throws GitLabApiException, GitAPIException {
        var problemSet = findProblemSet(courseId, id);

        var existingTokens = accessTokenRepo.findByOwnerAndHost(user, host);
        AccessToken tokenRecord;
        if (token.isBlank()) {
            // if no token is provided, try to use an existing one
            tokenRecord = existingTokens.stream()
                    .max(comparingInt(AccessToken::getId))
                    .orElse(null);
            if (tokenRecord == null) {
                model.addAttribute("problemSet", problemSet);
                model.addAttribute("host", host);
                model.addAttribute("groupPath", groupPath);
                model.addAttribute("ignoreAuthorless", ignoreAuthorless);
                model.addAttribute("error", "No token for host " + host + " available. Please provide one.");
                return "problem-sets/register-solutions-gitlab";
            }
        } else {
            // otherwise, if the provided token is new, store it
            var existing = existingTokens.stream()
                    .filter(t -> t.getToken().equals(token))
                    .findFirst();
            if (existing.isPresent()) {
                tokenRecord = existing.get();
            } else {
                tokenRecord = new AccessToken(user, host, token);
                accessTokenRepo.save(tokenRecord);
            }
        }

        problemSet.setRegisteringSolutions(true);
        repo.save(problemSet);

        var supplier = new GitLabGroupSolutionSupplier("https://" + host, groupPath,
                problemSet.getGradingConfig().getProjectRoot(), tokenRecord.getToken());
        if (LOCALHOST.contains(req.getServerName())) {
            logger.warn("Cannot determine server name (access via localhost), will not add webhooks");
        } else {
            var serverBaseUrl = format("https://%s%s", req.getServerName(), req.getContextPath());
            supplier.setWebhookBaseUrl(serverBaseUrl);
        }
        supplier.setIgnoringAuthorless(ignoreAuthorless);
        problemSetService.registerSolutions(problemSet.getId(), tokenRecord.getId(), supplier); // async
        return "redirect:./";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int courseId, @PathVariable int id) {
        var problemSet = findProblemSet(courseId, id);
        repo.delete(problemSet);
        return "redirect:../..";
    }

    @PostMapping("/{id}/remove-solutions")
    public String removeSolutions(@PathVariable int courseId, @PathVariable int id) {
        var problemSet = findProblemSet(courseId, id);
        problemSet.getSolutions().clear();
        repo.save(problemSet);
        return "redirect:.";
    }
}
