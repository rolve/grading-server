package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.GradingConfig;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static javax.persistence.CascadeType.ALL;

@Entity
public class ProblemSet {

    @Id
    @GeneratedValue
    private int id;
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Course course;
    private String name;
    private GradingConfig gradingConfig;
    private ZonedDateTime deadline;
    private boolean anonymous;

    @OneToMany(mappedBy = "problemSet", cascade = ALL, orphanRemoval = true)
    private List<Solution> solutions = new ArrayList<>();
    private boolean registeringSolutions = false;

    protected ProblemSet() {}

    public ProblemSet(Course course, String name, GradingConfig gradingConfig, ZonedDateTime deadline, boolean anonymous) {
        this.course = requireNonNull(course);
        this.name = requireNonNull(name);
        this.deadline = deadline;
        this.gradingConfig = gradingConfig;
        this.anonymous = anonymous;
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

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public List<Solution> getSolutions() {
        return solutions;
    }

    public boolean isRegisteringSolutions() {
        return registeringSolutions;
    }

    public void setRegisteringSolutions(boolean loadingSolutions) {
        this.registeringSolutions = loadingSolutions;
    }
}
