package ch.trick17.gradingserver.webapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabPushEvent(
        @JsonProperty("project") Project project,
        @JsonProperty("ref") String ref,
        @JsonProperty("after") String commitHash,
        @JsonProperty("user_username") String username) {

    public static record Project(
            @JsonProperty("git_http_url") String repoUrl) {
    }
}
