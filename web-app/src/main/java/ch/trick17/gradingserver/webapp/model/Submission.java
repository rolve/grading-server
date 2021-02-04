package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.CodeLocation;
import ch.trick17.gradingserver.GradingResult;

import javax.persistence.*;

import static ch.trick17.gradingserver.webapp.model.SubmissionState.*;
import static java.util.Objects.requireNonNull;

@Entity
public class Submission {

    @Id
    @ManyToOne
    private Solution solution;
    @Id
    @GeneratedValue
    private int number;
    @Column
    private CodeLocation location;
    @Column
    private boolean gradingStarted;
    @Column
    private GradingResult result;

    protected Submission() {}

    public Submission(Solution solution, CodeLocation location) {
        this.solution = requireNonNull(solution);
        this.location = requireNonNull(location);
    }

    public Solution getSolution() {
        return solution;
    }

    public int getNumber() {
        return number;
    }

    public CodeLocation getLocation() {
        return location;
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
