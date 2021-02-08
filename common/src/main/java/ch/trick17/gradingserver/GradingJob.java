package ch.trick17.gradingserver;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.MappedSuperclass;

import static java.util.Objects.requireNonNull;

@MappedSuperclass
public class GradingJob {

    private CodeLocation submission;
    private GradingConfig config;
    private GradingResult result;

    protected GradingJob() {}

    @JsonCreator
    public GradingJob(CodeLocation submission, GradingConfig config) {
        this.submission = requireNonNull(submission);
        this.config = requireNonNull(config);
    }

    public CodeLocation getSubmission() {
        return submission;
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
