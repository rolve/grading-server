package ch.trick17.gradingserver.controller;

import ch.trick17.gradingserver.GradingServerProperties;
import ch.trick17.gradingserver.Internationalization;
import ch.trick17.gradingserver.model.*;
import ch.trick17.gradingserver.model.ProblemSet.DisplaySetting;
import ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure;
import ch.trick17.gradingserver.service.GitLabGroupSolutionSupplier;
import ch.trick17.gradingserver.service.GradingService;
import ch.trick17.gradingserver.service.JarFileService;
import ch.trick17.gradingserver.service.JarFileService.JarDownloadFailedException;
import ch.trick17.gradingserver.service.ProblemSetService;
import ch.trick17.gradingserver.view.SolutionView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitlab4j.api.GitLabApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ch.trick17.gradingserver.controller.ProblemSetController.GradingType.IMPLEMENTATION;
import static ch.trick17.gradingserver.controller.ProblemSetController.GradingType.TEST_SUITE;
import static ch.trick17.gradingserver.model.ProblemSet.DisplaySetting.ANONYMOUS;
import static ch.trick17.gradingserver.model.ProblemSet.DisplaySetting.WITH_SHORTENED_NAMES;
import static ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure.ECLIPSE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.List.copyOf;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Controller
@RequestMapping("/courses/{courseId}/problem-sets")
public class ProblemSetController {

    private static final Set<String> LOCALHOST = Set.of("localhost", "127.0.0.1", "[::1]");

    private static final ImplGradingConfig DEFAULT_GRADING_CONFIG = new ImplGradingConfig(null, new GradingOptions(
            GradingOptions.Compiler.ECLIPSE, 7, Duration.ofSeconds(1), Duration.ofSeconds(5), true));

    private static final Logger logger = LoggerFactory.getLogger(ProblemSetController.class);

    private final CourseRepository courseRepo;
    private final AccessTokenRepository accessTokenRepo;
    private final ProblemSetService service;
    private final JarFileService jarFileService;
    private final GradingService gradingService;
    private final Internationalization i18n;

    private final String defaultGitLabHost;

    public ProblemSetController(ProblemSetService service,
                                CourseRepository courseRepo,
                                AccessTokenRepository accessTokenRepo,
                                JarFileService jarFileService, GradingService gradingService,
                                Internationalization i18n,
                                GradingServerProperties props) {
        this.service = service;
        this.courseRepo = courseRepo;
        this.accessTokenRepo = accessTokenRepo;
        this.jarFileService = jarFileService;
        this.gradingService = gradingService;
        this.i18n = i18n;
        this.defaultGitLabHost = props.getDefaultGitLabHost();
    }

    @GetMapping("/{id}/")
    public String problemSetPage(@PathVariable int courseId, @PathVariable int id, Model model) {
        var problemSet = findProblemSet(courseId, id);
        model.addAttribute("problemSet", problemSet);
        var solutions = problemSet.getSolutions().stream()
                .sorted(problemSet.getDisplaySetting() == ANONYMOUS
                        ? Solution.byCommitHash()
                        : Solution.byResult())
                .map(sol -> new SolutionView(sol.getId(), sol.getProblemSet(),
                        Set.copyOf(sol.getAuthors()), sol.latestSubmission()))
                .toList();
        model.addAttribute("solutions", solutions);
        return "problem-sets/problem-set";
    }

    public ProblemSet findProblemSet(int courseId, int id) {
        var problemSet = service.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (problemSet.getCourse().getId() != courseId) {
            // silly, but allowing any course ID would be too
            throw new ResponseStatusException(NOT_FOUND);
        }
        return problemSet;
    }

    @GetMapping({"/add", "/{id}/edit"})
    public String addOrEdit(@PathVariable int courseId,
                            @PathVariable(required = false) Integer id,
                            Model model) {
        var course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        model.addAttribute("course", course);
        model.addAttribute("add", id == null);
        if (id == null) {
            populateEditModel(model, "", null,
                    WITH_SHORTENED_NAMES, 80, "", ECLIPSE, null, emptyList(), "",
                    DEFAULT_GRADING_CONFIG, "");
        } else {
            var problemSet = findProblemSet(courseId, id);
            var projectConfig = problemSet.getProjectConfig();
            populateEditModel(model, problemSet.getName(),
                    problemSet.getDeadline(),
                    problemSet.getDisplaySetting(),
                    problemSet.getPercentageGoal(),
                    projectConfig.getProjectRoot(), projectConfig.getStructure(),
                    projectConfig.getPackageFilter(),
                    projectConfig.getDependencies(), "",
                    problemSet.getGradingConfig(), "");
        }
        return "problem-sets/edit";
    }

    public enum GradingType {IMPLEMENTATION, TEST_SUITE}

    @PostMapping({"/add", "/{id}/edit"})
    public String addOrEdit(@PathVariable int courseId,
                            @PathVariable(required = false) Integer id,
                            String name, LocalDate deadlineDate,
                            LocalTime deadlineTime, int deadlineTimeZoneOffset, // minutes
                            DisplaySetting displaySetting,
                            int percentageGoal, String projectRoot,
                            ProjectStructure structure, String packageFilter,
                            @RequestParam(required = false) Set<Integer> dependencies,
                            String newDependencies,
                            GradingType gradingType, MultipartFile testClassFile,
                            List<MultipartFile> refTestSuiteFiles,
                            List<MultipartFile> refImplementationFiles,
                            GradingOptions.Compiler compiler, int repetitions,
                            int repTimeoutMs, int testTimeoutMs, Model model,
                            HttpServletResponse response) throws IOException {
        var course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        // Basic properties
        var problemSet = id == null
                ? new ProblemSet(course)
                : findProblemSet(courseId, id);
        problemSet.setName(name);
        var offset = ZoneOffset.ofTotalSeconds(deadlineTimeZoneOffset * 60);
        var deadline = deadlineDate.atTime(deadlineTime).toInstant(offset);
        problemSet.setDeadline(deadline);
        problemSet.setDisplaySetting(displaySetting);
        problemSet.setPercentageGoal(percentageGoal);

        // Project config
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
            model.addAttribute("course", course);
            model.addAttribute("add", id == null);
            populateEditModel(model, name, deadline, displaySetting,
                    percentageGoal, projectRoot, structure, packageFilter, dependencyJars,
                    newDependencies, problemSet.getGradingConfig(), errorFor(e));
            response.setStatus(UNPROCESSABLE_ENTITY.value()); // required for Turbo
            return "problem-sets/edit";
        }
        var prevDependencies = id == null
                ? new ArrayList<JarFile>()
                : copyOf(problemSet.getProjectConfig().getDependencies());
        problemSet.setProjectConfig(new ProjectConfig(projectRoot, structure,
                packageFilter.isBlank() ? null : packageFilter, dependencyJars));

        // Grading config
        if (gradingType == IMPLEMENTATION) {
            String testClass;
            if (testClassFile.isEmpty() && problemSet.getGradingConfig() instanceof ImplGradingConfig gradingConfig) {
                testClass = gradingConfig.testClass(); // keep old one
            } else {
                testClass = new String(testClassFile.getBytes(), UTF_8);
            }
            var options = new GradingOptions(compiler, repetitions,
                    Duration.ofMillis(repTimeoutMs), Duration.ofMillis(testTimeoutMs), true);
            problemSet.setGradingConfig(new ImplGradingConfig(testClass, options));
            problemSet = service.save(problemSet);
        } else {
            if (problemSet.getGradingConfig() instanceof TestSuiteGradingConfig
                && refTestSuiteFiles.getFirst().isEmpty()
                && refImplementationFiles.getFirst().isEmpty()) {
                // keep old one
                problemSet = service.save(problemSet);
            } else if (!refTestSuiteFiles.getFirst().isEmpty() && !refImplementationFiles.getFirst().isEmpty()) {
                // update config
                var refTestSuite = getContents(refTestSuiteFiles);
                var refImplementation = getContents(refImplementationFiles);
                service.prepareAndSave(problemSet, refTestSuite, refImplementation);
            } else {
                // error, must provide both
                model.addAttribute("course", course);
                model.addAttribute("add", id == null);
                populateEditModel(model, name, deadline,
                        displaySetting, percentageGoal, projectRoot, structure,
                        packageFilter, dependencyJars, newDependencies,
                        new TestSuiteGradingConfig(null),
                        i18n.message("problem-set.empty-test-suite-or-impl"));
                response.setStatus(UNPROCESSABLE_ENTITY.value()); // required for Turbo
                return "problem-sets/edit";
            }
        }

        prevDependencies.forEach(jarFileService::deleteIfUnused);
        return "redirect:/courses/" + courseId + "/problem-sets/" + problemSet.getId() + "/";
    }

    private List<String> getContents(List<MultipartFile> files) throws IOException {
        var contents = new ArrayList<String>();
        for (var file : files) {
            contents.add(new String(file.getBytes(), UTF_8));
        }
        return contents;
    }

    private void populateEditModel(Model model, String name, Instant deadline,
                                   DisplaySetting displaySetting, int percentageGoal,
                                   String projectRoot, ProjectStructure structure,
                                   String packageFilter, List<JarFile> dependencies,
                                   String newDependencies, GradingConfig gradingConfig,
                                   String error) {
        model.addAttribute("name", name);
        model.addAttribute("deadline", deadline);
        model.addAttribute("displaySetting", displaySetting);
        model.addAttribute("displaySettings", DisplaySetting.values());
        model.addAttribute("percentageGoal", percentageGoal);
        model.addAttribute("projectRoot", projectRoot);
        model.addAttribute("structure", structure);
        model.addAttribute("packageFilter", packageFilter);
        model.addAttribute("possibleDependencies", jarFileService.findAll());
        model.addAttribute("dependencies", dependencies);
        model.addAttribute("newDependencies", newDependencies);
        if (gradingConfig instanceof ImplGradingConfig impl) {
            model.addAttribute("gradingType", IMPLEMENTATION);
            model.addAttribute("options", impl.options());
        } else if (gradingConfig instanceof TestSuiteGradingConfig) {
            model.addAttribute("gradingType", TEST_SUITE);
            model.addAttribute("options", DEFAULT_GRADING_CONFIG.options());
        } else {
            throw new AssertionError("Unexpected grading config type: " + gradingConfig.getClass());
        }
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
                                          Model model, HttpServletResponse response)
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
                model.addAttribute("error", i18n.message("problem-set.no-token", host));
                response.setStatus(UNPROCESSABLE_ENTITY.value()); // required for Turbo
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

        var supplier = new GitLabGroupSolutionSupplier("https://" + host, groupPath,
                tokenRecord.getToken(), problemSet.getProjectConfig());
        if (LOCALHOST.contains(req.getServerName())) {
            logger.warn("Cannot determine server name (access via localhost), will not add webhooks");
        } else {
            var serverBaseUrl = format("https://%s%s", req.getServerName(), req.getContextPath());
            supplier.setWebhookBaseUrl(serverBaseUrl);
        }
        supplier.setIgnoringAuthorless(ignoreAuthorless);

        problemSet.setRegisteringSolutions(true);
        service.save(problemSet);
        service.registerSolutions(problemSet.getId(), tokenRecord.getId(), supplier); // async
        return "redirect:./";
    }

    @PostMapping("/{id}/re-grade-latest")
    @Transactional
    public String reGradeLatest(@PathVariable int courseId, @PathVariable int id) {
        var problemSet = findProblemSet(courseId, id);
        for (var solution : problemSet.getSolutions()) {
            var latest = solution.latestSubmission();
            if (latest != null) {
                gradingService.grade(latest);
            }
        }
        return "redirect:./";
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable int courseId, @PathVariable int id) {
        var problemSet = findProblemSet(courseId, id);
        service.delete(problemSet);
        problemSet.getProjectConfig().getDependencies().forEach(jarFileService::deleteIfUnused);
        return "redirect:../../";
    }

    @PostMapping("/{id}/remove-solutions")
    public String removeSolutions(@PathVariable int courseId, @PathVariable int id) {
        var problemSet = findProblemSet(courseId, id);
        problemSet.getSolutions().clear();
        service.save(problemSet);
        return "redirect:./";
    }
}
