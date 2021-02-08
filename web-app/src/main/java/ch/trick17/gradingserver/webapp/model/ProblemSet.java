package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.GradingConfig;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

@Entity
public class ProblemSet implements Serializable {

    @Id
    @ManyToOne
    private Course course;
    @Id
    private String name;
    private GradingConfig gradingConfig;
    private ZonedDateTime deadline;

    protected ProblemSet() {}

    public ProblemSet(Course course, String name, GradingConfig gradingConfig, ZonedDateTime deadline) {
        this.course = requireNonNull(course);
        this.name = requireNonNull(name);
        this.deadline = deadline;
        this.gradingConfig = gradingConfig;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = requireNonNull(course);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireNonNull(name);
    }

    public GradingConfig getGradingConfig() {
        return gradingConfig;
    }

    public void setGradingConfig(GradingConfig gradingConfig) {
        this.gradingConfig = requireNonNull(gradingConfig);
    }

    public ZonedDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(ZonedDateTime deadline) {
        this.deadline = requireNonNull(deadline);
    }
}
