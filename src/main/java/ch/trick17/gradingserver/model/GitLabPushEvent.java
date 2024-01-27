package ch.trick17.gradingserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabPushEvent(
        @JsonProperty("project") Project project,
        @JsonProperty("ref") String ref,
        @JsonProperty("before") String beforeCommit,
        @JsonProperty("after") String afterCommit,
        @JsonProperty("user_username") String username) {

    public static record Project(
            @JsonProperty("git_http_url") String repoUrl) {
    }
}
