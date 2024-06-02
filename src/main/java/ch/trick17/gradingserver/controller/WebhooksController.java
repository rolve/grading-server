package ch.trick17.gradingserver.controller;

import ch.trick17.gradingserver.model.GitLabPushEvent;
import ch.trick17.gradingserver.model.SolutionRepository;
import ch.trick17.gradingserver.model.Submission;
import ch.trick17.gradingserver.model.SubmissionRepository;
import ch.trick17.gradingserver.service.GitRepoDiffFetcher;
import ch.trick17.gradingserver.service.GradingService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static java.time.ZonedDateTime.now;

@RestController
public class WebhooksController {

    public static final String GITLAB_PUSH_PATH = "/webhooks/gitlab-push";

    private static final Logger logger = LoggerFactory.getLogger(WebhooksController.class);

    private final SolutionRepository solRepo;
    private final SubmissionRepository submissionRepo;
    private final GradingService gradingService;

    public WebhooksController(SolutionRepository solRepo,
                              SubmissionRepository submissionRepo,
                              GradingService gradingService) {
        this.solRepo = solRepo;
        this.submissionRepo = submissionRepo;
        this.gradingService = gradingService;
    }

    @PostMapping(GITLAB_PUSH_PATH)
    public ResponseEntity<Void> gitlabPush(@RequestBody GitLabPushEvent event) throws GitAPIException {
        logger.info("Received push event for {} ({})", event.project().repoUrl(), event.ref());

        var matchingSolutions = solRepo.findByRepoUrl(event.project().repoUrl());
        if (matchingSolutions.isEmpty()) {
            logger.warn("No matching solutions for push event found");
        }
        var ignoredRefs = 0;
        var ignoredUser = 0;
        var ignoredPath = 0;
        for (var sol : matchingSolutions) {
            if (!event.ref().equals("refs/heads/" + sol.getBranch())) {
                ignoredRefs++;
                continue;
            }
            if (sol.getIgnoredPushers().contains(event.username())) {
                ignoredUser++;
                continue;
            }
            var srcPath = sol.getProblemSet().getProjectConfig().getSrcPackageDir().toString()
                    .replace('\\', '/'); // in case we are on Windows...
            var testPath = sol.getProblemSet().getProjectConfig().getTestPackageDir().toString()
                    .replace('\\', '/');
            var token = sol.getAccessToken().getToken();
            try (var fetcher = new GitRepoDiffFetcher(sol.getRepoUrl(), sol.getBranch(), "", token)) {
                var paths = fetcher.affectedPaths(event.beforeCommit(), event.afterCommit());
                if (paths.stream().noneMatch(p -> p.startsWith(srcPath) ||
                                                  p.startsWith(testPath))) {
                    ignoredPath++;
                    continue;
                }
            }

            var submission = new Submission(sol, event.afterCommit(), now());
            submission = submissionRepo.save(submission);
            gradingService.grade(submission); // async
        }
        if (ignoredRefs > 0) {
            logger.info("Ignored push to ref {} for {} solutions", event.ref(), ignoredRefs);
        }
        if (ignoredUser > 0) {
            logger.info("Ignored push from user {} for {} solutions", event.username(), ignoredUser);
        }
        if (ignoredPath > 0) {
            logger.info("Ignored push affecting only paths outside project directories for {} solutions", ignoredPath);
        }
        return ResponseEntity.noContent().build();
    }
}
