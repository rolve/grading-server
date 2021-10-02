package ch.trick17.gradingserver;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import static java.util.Objects.requireNonNull;

@MappedSuperclass
public class GradingJob {

    private CodeLocation submission;
    @Transient
    private String accessToken;
    private GradingConfig config;
    private GradingResult result;

    protected GradingJob() {}

    @JsonCreator
    public GradingJob(CodeLocation submission, String accessToken,
                      GradingConfig config) {
        this.submission = requireNonNull(submission);
        this.accessToken = accessToken;
        this.config = requireNonNull(config);
    }

    public CodeLocation getSubmission() {
        return submission;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public GradingConfig getConfig() {
        return config;
    }

    public boolean hasResult() {
        return result != null;
    }

    public GradingResult getResult() {
        return result;
    }

    public void setResult(GradingResult result) {
        this.result = result;
    }
}
