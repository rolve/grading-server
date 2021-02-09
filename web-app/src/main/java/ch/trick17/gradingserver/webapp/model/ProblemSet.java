package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.GradingConfig;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

@Entity
public class ProblemSet {

    @Id
    @GeneratedValue
    private int id;
    @ManyToOne
    private Course course;
    private String name;
    private GradingConfig gradingConfig;
    private ZonedDateTime deadline;

    protected ProblemSet() {}

    public ProblemSet(Course course, String name, GradingConfig gradingConfig, ZonedDateTime deadline) {
        this.course = requireNonNull(course);
        this.name = requireNonNull(name);
        this.deadline = deadline;
        this.gradingConfig = gradingConfig;
        course.getProblemSets().add(this);
    }

    public int getId() {
        return id;
    }

    public Course getCourse() {
        return course;
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
