package ch.trick17.gradingserver;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import static java.util.Objects.requireNonNull;

@MappedSuperclass
public class GradingJob {

    private CodeLocation submission;
    @Transient
    private String username;
    @Transient
    private String password;
    private GradingConfig config;
    private GradingResult result;

    protected GradingJob() {}

    @JsonCreator
    public GradingJob(CodeLocation submission, String username, String password,
                      GradingConfig config) {
        this.submission = requireNonNull(submission);
        this.username = username;
        this.password = password;
        this.config = requireNonNull(config);
    }

    public CodeLocation getSubmission() {
        return submission;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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
