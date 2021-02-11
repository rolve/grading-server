package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.CodeLocation;
import ch.trick17.gradingserver.GradingResult;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.Serializable;

import static ch.trick17.gradingserver.webapp.model.SubmissionState.*;
import static java.util.Objects.requireNonNull;

@Entity
public class Submission implements Serializable {

    @Id
    @ManyToOne
    private Solution solution;
    @Id
    @GeneratedValue
    private int number;
    private String commitHash;
    private boolean gradingStarted;
    private GradingResult result;

    protected Submission() {}

    public Submission(Solution solution, String commitHash) {
        this.solution = requireNonNull(solution);
        this.commitHash = requireNonNull(commitHash);
    }

    public Solution getSolution() {
        return solution;
    }

    public int getNumber() {
        return number;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public CodeLocation getCodeLocation() {
        return new CodeLocation(solution.getRepoUrl(), commitHash);
    }

    public boolean isGradingStarted() {
        return gradingStarted;
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

    public SubmissionState getState() {
        if (!gradingStarted) {
            return RECEIVED;
        } else if (!hasResult()) {
            return GRADING;
        } else if (result.successful()) {
            return GRADED;
        } else {
            return ERROR;
        }
    }
}
