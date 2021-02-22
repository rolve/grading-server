package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.CodeLocation;
import ch.trick17.gradingserver.GradingResult;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.time.ZonedDateTime;

import static ch.trick17.gradingserver.webapp.model.SubmissionState.*;
import static java.util.Objects.requireNonNull;
import static javax.persistence.CascadeType.PERSIST;

@Entity
public class Submission implements Serializable {

    @Id
    @GeneratedValue
    private int id;
    @ManyToOne(cascade = PERSIST)
    private Solution solution;
    private String commitHash;
    private ZonedDateTime receivedDate;
    private boolean gradingStarted;
    private GradingResult result;

    protected Submission() {}

    public Submission(Solution solution, String commitHash, ZonedDateTime receivedDate) {
        this.solution = requireNonNull(solution);
        this.commitHash = requireNonNull(commitHash);
        this.receivedDate = requireNonNull(receivedDate);
    }

    public Solution getSolution() {
        return solution;
    }

    public int getId() {
        return id;
    }

    public String getCommitHash() {
        return commitHash;
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

    public SubmissionState getStatus() {
        if (!gradingStarted) {
            return QUEUED;
        } else if (!hasResult()) {
            return GRADING;
        } else if (result.successful()) {
            return GRADED;
        } else {
            return ERROR;
        }
    }
}
