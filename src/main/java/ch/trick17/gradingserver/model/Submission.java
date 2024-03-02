package ch.trick17.gradingserver.model;

import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;
import static javax.persistence.CascadeType.PERSIST;

@Entity
public class Submission {

    @Id
    @GeneratedValue
    private int id;
    @ManyToOne(cascade = PERSIST)
    private Solution solution;
    private String commitHash;
    private ZonedDateTime receivedDate;
    private boolean gradingStarted;
    @Type(type = "json")
    private GradingResult result;

    protected Submission() {}

    public Submission(Solution solution, String commitHash, ZonedDateTime receivedDate) {
        this.solution = requireNonNull(solution);
        this.commitHash = requireNonNull(commitHash);
        this.receivedDate = requireNonNull(receivedDate);
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

    public ZonedDateTime getReceivedDate() {
        return receivedDate;
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
