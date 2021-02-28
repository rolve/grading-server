package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.GitLabPushEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookReceiver {

    public static final String GITLAB_PUSH_PATH = "/webhooks/gitlab-push";

    @PostMapping(GITLAB_PUSH_PATH)
    public ResponseEntity<Void> gitlabPush(@RequestBody GitLabPushEvent event) {
        // TODO
        System.out.println(event.project().repoUrl());
        System.out.println(event.commitHash());
        return ResponseEntity.noContent().build();
    }
}
