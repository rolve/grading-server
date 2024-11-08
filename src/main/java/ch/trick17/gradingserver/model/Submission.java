package ch.trick17.gradingserver.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.LAZY;
import static java.util.Objects.requireNonNull;

@Entity
public class Submission {

    @Id
    @GeneratedValue
    private int id;
    @ManyToOne(cascade = PERSIST)
    private Solution solution;
    private String commitHash;
    private Instant receivedTime;
    private boolean gradingStarted;
    @Basic(fetch = LAZY)
    @Type(JsonType.class)
    private GradingResult result;

    protected Submission() {}

    public Submission(Solution solution, String commitHash, Instant receivedTime) {
        this.solution = requireNonNull(solution);
        this.commitHash = requireNonNull(commitHash);
        this.receivedTime = requireNonNull(receivedTime);
        solution.getSubmissions().add(this);
    }

    public int getId() {
        return id;
    }

    public Solution getSolution() {
        return solution;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String shortCommitHash() {
        return commitHash.substring(0, 8);
    }

    public Instant getReceivedTime() {
        return receivedTime;
    }

    public CodeLocation getCodeLocation() {
        return new CodeLocation(solution.getRepoUrl(), commitHash);
    }

    public boolean isGradingStarted() {
        return gradingStarted;
    }

    public void setGradingStarted(boolean gradingStarted) {
        this.gradingStarted = gradingStarted;
    }

    public boolean hasResult() {
        return result != null;
    }

    public GradingResult getResult() {
        return result;
    }

    public void setResult(GradingResult result) {
        this.result = requireNonNull(result);
    }

    public void clearResult() {
        result = null;
    }

    public SubmissionState getStatus() {
        if (!gradingStarted) {
            return SubmissionState.QUEUED;
        } else if (!hasResult()) {
            return SubmissionState.GRADING;
        } else if (result instanceof OutdatedResult) {
            return SubmissionState.OUTDATED;
        } else if (result instanceof ErrorResult) {
            return SubmissionState.ERROR;
        } else {
            return SubmissionState.GRADED;
        }
    }

    public ImplGradingResult getImplResult() {
        return result instanceof ImplGradingResult i ? i : null;
    }

    public TestSuiteGradingResult getTestSuiteResult() {
        return result instanceof TestSuiteGradingResult t ? t : null;
    }

    public boolean isLatest() {
        return solution.latestSubmission() == this;
    }
}
