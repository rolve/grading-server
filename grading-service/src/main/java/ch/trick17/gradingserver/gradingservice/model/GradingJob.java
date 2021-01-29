package ch.trick17.gradingserver.gradingservice.model;

import ch.trick17.gradingserver.CodeLocation;
import ch.trick17.gradingserver.GradingConfig;
import ch.trick17.gradingserver.GradingResult;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Random;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

@Entity
public class GradingJob {

    @Id
    private String id;
    @Embedded
    private CodeLocation submission;
    @Embedded
    private GradingConfig config;
    @Embedded
    private GradingResult result;

    protected GradingJob() {}

    public GradingJob(CodeLocation submission, GradingConfig config, GradingResult result) {
        // generate 128-bit hex string, similar to Git commit hashes
        var random = new Random();
        this.id = range(0, 4)
                .mapToObj(i -> format("%08x", random.nextInt()))
                .collect(joining());
        this.submission = submission;
        this.config = config;
        this.result = result;
    }

    public String id() {
        return id;
    }

    public CodeLocation submission() {
        return submission;
    }

    public GradingConfig config() {
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
