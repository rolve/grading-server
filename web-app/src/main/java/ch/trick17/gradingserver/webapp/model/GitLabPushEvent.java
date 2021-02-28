package ch.trick17.gradingserver.webapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabPushEvent(
        @JsonProperty("project") Project project,
        @JsonProperty("after") String commitHash) {

    public static record Project(
            @JsonProperty("git_http_url") String repoUrl) {
    }
}
