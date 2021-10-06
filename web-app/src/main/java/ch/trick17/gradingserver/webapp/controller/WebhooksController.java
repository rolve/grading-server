package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.GitLabPushEvent;
import ch.trick17.gradingserver.webapp.model.SolutionRepository;
import ch.trick17.gradingserver.webapp.model.Submission;
import ch.trick17.gradingserver.webapp.model.SubmissionRepository;
import ch.trick17.gradingserver.webapp.service.GitRepoDiffFetcher;
import ch.trick17.gradingserver.webapp.service.GradingService;
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
        if (!event.ref().equals("refs/heads/master")) {
            logger.info("Ignored push of ref '{}'", event.ref());
            return ResponseEntity.noContent().build();
        }

        var matchingSolutions = solRepo.findByRepoUrl(event.project().repoUrl());
        if (matchingSolutions.isEmpty()) {
            logger.warn("No matching solutions for push event found");
        }
        var ignoredUser = 0;
        var ignoredPath = 0;
        for (var sol : matchingSolutions) {
            if (sol.getIgnoredPushers().contains(event.username())) {
                ignoredUser++;
                continue;
            }
            var projectRoot = sol.getProblemSet().getGradingConfig().getProjectRoot();
            if (!projectRoot.isEmpty()) {
                var token = sol.getAccessToken().getToken();
                try (var fetcher = new GitRepoDiffFetcher(sol.getRepoUrl(), "", token)) {
                    var paths = fetcher.affectedPaths(event.beforeCommit(), event.afterCommit());
                    if (paths.stream().noneMatch(p -> p.startsWith(projectRoot))) {
                        ignoredPath++;
                        continue;
                    }
                }
            }
            var submission = new Submission(sol, event.afterCommit(), now());
            submission = submissionRepo.save(submission);
            gradingService.grade(submission); // async
        }
        if (ignoredUser > 0) {
            logger.info("Ignored push from user {} for {} solutions", event.username(), ignoredUser);
        }
        if (ignoredPath > 0) {
            logger.info("Ignored push affecting only paths outside project root for {} solutions", ignoredPath);
        }
        return ResponseEntity.noContent().build();
    }
}
