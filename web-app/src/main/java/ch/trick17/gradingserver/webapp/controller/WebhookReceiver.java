package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.GitLabPushEvent;
import ch.trick17.gradingserver.webapp.model.SolutionRepository;
import ch.trick17.gradingserver.webapp.service.SubmissionService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookReceiver {

    public static final String GITLAB_PUSH_PATH = "/webhooks/gitlab-push";

    private static final Logger logger = LoggerFactory.getLogger(WebhookReceiver.class);

    private final SolutionRepository solRepo;
    private final SubmissionService submissionService;

    public WebhookReceiver(SolutionRepository solRepo,
                           SubmissionService submissionService) {
        this.solRepo = solRepo;
        this.submissionService = submissionService;
    }

    @PostMapping(GITLAB_PUSH_PATH)
    public ResponseEntity<Void> gitlabPush(@RequestBody GitLabPushEvent event)
            throws GitAPIException {
        logger.info("Received push event for {}", event.project().repoUrl());
        var matchingSolutions = solRepo.findByRepoUrl(event.project().repoUrl());
        if (matchingSolutions.isEmpty()) {
            logger.warn("No matching solutions for push event found");
        }
        for (var sol : matchingSolutions) {
            sol.setFetchingSubmission(true);
            submissionService.fetchSubmission(sol.getId()); // async
        }
        return ResponseEntity.noContent().build();
    }
}
