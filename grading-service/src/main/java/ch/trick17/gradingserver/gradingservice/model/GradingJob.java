package ch.trick17.gradingserver.gradingservice.model;

import ch.trick17.gradingserver.CodeLocation;
import ch.trick17.gradingserver.GradingConfig;
import ch.trick17.gradingserver.GradingResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static java.util.Objects.requireNonNull;

@Entity
public class GradingJob {

    @Id
    @GeneratedValue(generator = "job-id-generator")
    @GenericGenerator(name = "job-id-generator",
            strategy = "ch.trick17.gradingserver.gradingservice.model.JobIdGenerator")
    private String id;

    @Embedded
    private CodeLocation submission;

    @Embedded
    private GradingConfig config;

    @Embedded
    private GradingResult result;

    protected GradingJob() {}

    @JsonCreator
    public GradingJob(CodeLocation submission, GradingConfig config) {
        this.submission = requireNonNull(submission);
        this.config = requireNonNull(config);
    }

    public String getId() {
        return id;
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
