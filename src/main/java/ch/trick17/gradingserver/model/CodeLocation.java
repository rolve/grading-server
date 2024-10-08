package ch.trick17.gradingserver.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Embeddable;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Embeddable
public class CodeLocation {

    private String repoUrl;
    private String commitHash;

    protected CodeLocation() {}

    @JsonCreator
    public CodeLocation(String repoUrl, String commitHash) {
        this.repoUrl = requireNonNull(repoUrl);
        this.commitHash = requireNonNull(commitHash);
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String shortCommitHash() {
        return commitHash.substring(0, 8);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CodeLocation) obj;
        return Objects.equals(this.repoUrl, that.repoUrl) &&
                Objects.equals(this.commitHash, that.commitHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repoUrl, commitHash);
    }

    @Override
    public String toString() {
        return repoUrl + ":" + shortCommitHash();
    }
}
