package ch.trick17.gradingserver.gradingservice.model;

import ch.trick17.gradingserver.CodeLocation;
import ch.trick17.gradingserver.Credentials;
import ch.trick17.gradingserver.GradingConfig;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class GradingJob extends ch.trick17.gradingserver.GradingJob {

    @Id
    @GeneratedValue(generator = "job-id-generator")
    @GenericGenerator(name = "job-id-generator",
            strategy = "ch.trick17.gradingserver.gradingservice.model.JobIdGenerator")
    private String id;

    protected GradingJob() {}

    @JsonCreator
    public GradingJob(CodeLocation submission, Credentials credentials,
                      GradingConfig config) {
        super(submission, credentials, config);
    }

    public String getId() {
        return id;
    }
}
