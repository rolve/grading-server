package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.model.GradingConfig;
import ch.trick17.gradingserver.model.GradingConfig.ProjectStructure;
import ch.trick17.gradingserver.model.GradingOptions;
import ch.trick17.gradingserver.model.GradingOptions.Compiler;
import ch.trick17.gradingserver.model.JarFile;
import ch.trick17.gradingserver.webapp.Internationalization;
import ch.trick17.gradingserver.webapp.WebAppProperties;
import ch.trick17.gradingserver.webapp.model.*;
import ch.trick17.gradingserver.webapp.service.GitLabGroupSolutionSupplier;
import ch.trick17.gradingserver.webapp.service.JarFileService;
import ch.trick17.gradingserver.webapp.service.JarFileService.JarDownloadFailedException;
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
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ch.trick17.gradingserver.webapp.model.Solution.byCommitHash;
import static ch.trick17.gradingserver.webapp.model.Solution.byResult;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Controller
@RequestMapping("/courses/{courseId}/problem-sets")
public class ProblemSetController {

    private static final Set<String> LOCALHOST = Set.of("localhost", "127.0.0.1", "[::1]");

    private static final Logger logger = LoggerFactory.getLogger(ProblemSetController.class);

    private final ProblemSetRepository repo;
    private final CourseRepository courseRepo;
    private final AccessTokenRepository accessTokenRepo;
    private final ProblemSetService problemSetService;
    private final JarFileService jarFileService;
    private final Internationalization i18n;

    private final String defaultGitLabHost;

    public ProblemSetController(ProblemSetRepository repo, CourseRepository courseRepo,
                                AccessTokenRepository accessTokenRepo,
                                ProblemSetService problemSetService,
                                JarFileService jarFileService,
                                Internationalization i18n,
                                WebAppProperties props) {
        this.repo = repo;
        this.courseRepo = courseRepo;
        this.accessTokenRepo = accessTokenRepo;
        this.problemSetService = problemSetService;
        this.jarFileService = jarFileService;
        this.i18n = i18n;
        this.defaultGitLabHost = props.getDefaultGitLabHost();
    }

    @GetMapping("/{id}/")
    @PreAuthorize("!this.findProblemSet(#courseId, #id).hidden || hasRole('LECTURER')")
    public String problemSetPage(@PathVariable int courseId, @PathVariable int id, Model model) {
        var problemSet = findProblemSet(courseId, id);
        model.addAttribute("problemSet", problemSet);
        // somehow, sorting in the template doesn't work, so do it here:
        var sort = problemSet.isAnonymous() ? byCommitHash() : byResult();
        var solutions = problemSet.getSolutions().stream()
                .sorted(sort).collect(toList());
        model.addAttribute("solutions", solutions);
        model.addAttribute("stats", ProblemSetStats.create(problemSet));
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
        var course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        populateModel(model, course, true,
                "", LocalDate.now().plusDays(7), LocalTime.of(23, 59),
                false, false, ProjectStructure.ECLIPSE, "", emptyList(), "",
                Compiler.ECLIPSE, 7, 1_000, 5_000, "");
        return "problem-sets/edit";
    }

    @PostMapping("/add")
    public String addProblemSet(@PathVariable int courseId, String name,
                                LocalDate deadlineDate, LocalTime deadlineTime,
                                @RequestParam(defaultValue = "false") boolean anonymous,
                                @RequestParam(defaultValue = "false") boolean hidden,
                                MultipartFile testClassFile, ProjectStructure structure,
                                String projectRoot,
                                @RequestParam(required = false) Set<Integer> dependencies,
                                String newDependencies,
                                Compiler compiler, int repetitions,
                                int repTimeoutMs, int testTimeoutMs,
                                Model model, HttpServletResponse response) throws IOException {
        var course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        var testClass = new String(testClassFile.getBytes(), UTF_8);

        var dependencyJars = new ArrayList<JarFile>();
        if (dependencies != null) {
            dependencyJars.addAll(jarFileService.findAllById(dependencies));
        }
        try {
            if (!newDependencies.isBlank()) {
                for (var identifier : newDependencies.split("\\s+")) {
                    dependencyJars.add(jarFileService.downloadAndStoreJarFile(identifier));
                }
            }
        } catch (JarDownloadFailedException e) {
            populateModel(model, course, true,
                    name, deadlineDate, deadlineTime, anonymous, hidden,
                    structure, projectRoot, dependencyJars, newDependencies,
                    compiler, repetitions, repTimeoutMs, testTimeoutMs, errorFor(e));
            response.setStatus(UNPROCESSABLE_ENTITY.value());
            return "problem-sets/edit";
        }

        var config = new GradingConfig(testClass, projectRoot, structure,
                dependencyJars, new GradingOptions(compiler, repetitions,
                        Duration.ofMillis(repTimeoutMs), Duration.ofMillis(testTimeoutMs), true));
        var deadline = ZonedDateTime.of(deadlineDate, deadlineTime, ZoneId.systemDefault());
        var problemSet = new ProblemSet(course, name, config,
                deadline, anonymous, hidden);
        problemSet = repo.save(problemSet);
        return "redirect:" + problemSet.getId() + "/";
    }

    @GetMapping("/{id}/edit")
    public String editProblemSet(@PathVariable int courseId,
                                 @PathVariable int id, Model model) {
        var problemSet = findProblemSet(courseId, id);
        var config = problemSet.getGradingConfig();
        var options = config.getOptions();
        populateModel(model, problemSet.getCourse(), false,
                problemSet.getName(),
                problemSet.getDeadline().toLocalDate(),
                problemSet.getDeadline().toLocalTime(),
                problemSet.isAnonymous(), problemSet.isHidden(),
                config.getStructure(), config.getProjectRoot(),
                config.getDependencies(), "", options.getCompiler(),
                options.getRepetitions(), options.getRepTimeout().toMillis(),
                options.getTestTimeout().toMillis(), "");
        return "problem-sets/edit";
    }

    @PostMapping("/{id}/edit")
    public String editProblemSet(@PathVariable int courseId, @PathVariable int id,
                                 String name, LocalDate deadlineDate, LocalTime deadlineTime,
                                 @RequestParam(defaultValue = "false") boolean anonymous,
                                 @RequestParam(defaultValue = "false") boolean hidden,
                                 MultipartFile testClassFile, ProjectStructure structure,
                                 String projectRoot,
                                 @RequestParam(required = false) Set<Integer> dependencies,
                                 String newDependencies, Compiler compiler,
                                 int repetitions, int repTimeoutMs, int testTimeoutMs,
                                 Model model, HttpServletResponse response) throws IOException {
        var problemSet = findProblemSet(courseId, id);
        var testClass = testClassFile.isEmpty()
                ? problemSet.getGradingConfig().getTestClass()
                : new String(testClassFile.getBytes(), UTF_8);

        var dependencyJars = new ArrayList<JarFile>();
        if (dependencies != null) {
            dependencyJars.addAll(jarFileService.findAllById(dependencies));
        }
        try {
            if (!newDependencies.isBlank()) {
                for (var identifier : newDependencies.split("\\s+")) {
                    dependencyJars.add(jarFileService.downloadAndStoreJarFile(identifier));
                }
            }
        } catch (JarDownloadFailedException e) {
            populateModel(model, problemSet.getCourse(), false,
                    name, deadlineDate, deadlineTime, anonymous, hidden,
                    structure, projectRoot, dependencyJars, newDependencies,
                    compiler, repetitions, repTimeoutMs, testTimeoutMs, errorFor(e));
            response.setStatus(UNPROCESSABLE_ENTITY.value());
            return "problem-sets/edit";
        }

        var prevDependencies = copyOf(problemSet.getGradingConfig().getDependencies());

        var options = new GradingOptions(compiler, repetitions,
                Duration.ofMillis(repTimeoutMs), Duration.ofMillis(testTimeoutMs), true);
        var config = new GradingConfig(testClass, projectRoot, structure,
                dependencyJars, options);
        problemSet.setName(name);
        problemSet.setGradingConfig(config);
        problemSet.setDeadline(ZonedDateTime.of(deadlineDate, deadlineTime, ZoneId.systemDefault()));
        problemSet.setAnonymous(anonymous);
        problemSet.setHidden(hidden);
        repo.save(problemSet);

        prevDependencies.forEach(jarFileService::deleteIfUnused);
        return "redirect:.";
    }

    private void populateModel(Model model, Course course, boolean add, String name,
                               LocalDate deadlineDate, LocalTime deadlineTime,
                               boolean anonymous, boolean hidden,
                               ProjectStructure structure, String projectRoot,
                               List<JarFile> dependencies, String newDependencies,
                               Compiler compiler, int repetitions,
                               long repTimeoutMs, long testTimeoutMs,
                               String error) {
        model.addAttribute("course", course);
        model.addAttribute("add", add);
        model.addAttribute("name", name);
        model.addAttribute("deadlineDate", deadlineDate);
        model.addAttribute("deadlineTime", deadlineTime);
        model.addAttribute("anonymous", anonymous);
        model.addAttribute("hidden", hidden);
        model.addAttribute("structure", structure);
        model.addAttribute("projectRoot", projectRoot);
        model.addAttribute("possibleDependencies", jarFileService.findAll());
        model.addAttribute("dependencies", dependencies);
        model.addAttribute("newDependencies", newDependencies);
        model.addAttribute("compiler", compiler);
        model.addAttribute("repetitions", repetitions);
        model.addAttribute("repTimeoutMs", repTimeoutMs);
        model.addAttribute("testTimeoutMs", testTimeoutMs);
        model.addAttribute("error", error);
    }

    private String errorFor(JarDownloadFailedException e) {
        return i18n.message(switch (e.getReason()) {
            case INVALID_URL -> "problem-set.invalid-url";
            case NOT_FOUND -> "problem-set.jar-not-found";
            case INVALID_JAR -> "problem-set.invalid-jar-file";
            case DOWNLOAD_FAILED -> "problem-set.download-failed";
        }, e.getUrl());
    }

    @GetMapping("/{id}/register-solutions-gitlab")
    public String registerSolutionsGitLab(@PathVariable int courseId, @PathVariable int id,
                                          Model model) {
        var problemSet = findProblemSet(courseId, id);
        model.addAttribute("problemSet", problemSet);
        model.addAttribute("host", defaultGitLabHost);
        model.addAttribute("ignoreAuthorless", true);
        return "problem-sets/register-solutions-gitlab";
    }

    @PostMapping("/{id}/register-solutions-gitlab")
    public String registerSolutionsGitLab(@PathVariable int courseId, @PathVariable int id,
                                          String host, String groupPath, String token,
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
    @Transactional
    public String delete(@PathVariable int courseId, @PathVariable int id) {
        var problemSet = findProblemSet(courseId, id);
        repo.delete(problemSet);
        problemSet.getGradingConfig().getDependencies().forEach(jarFileService::deleteIfUnused);
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
