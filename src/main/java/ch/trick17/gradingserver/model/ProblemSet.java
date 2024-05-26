package ch.trick17.gradingserver.model;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private ProjectConfig projectConfig;
    @Type(type = "json")
    private GradingConfig gradingConfig;
    private ZonedDateTime deadline;
    private DisplaySetting displaySetting;
    private int percentageGoal;

    @OneToMany(mappedBy = "problemSet", cascade = ALL, orphanRemoval = true)
    private List<Solution> solutions = new ArrayList<>();
    private boolean registeringSolutions = false;

    protected ProblemSet() {
    }

    public ProblemSet(Course course) {
        this.course = requireNonNull(course);
        course.getProblemSets().add(this);
    }

    public ProblemSet(Course course, String name, ProjectConfig projectConfig,
                      GradingConfig gradingConfig, ZonedDateTime deadline,
                      DisplaySetting displaySetting) {
        this(course);
        this.name = requireNonNull(name);
        this.deadline = deadline;
        this.projectConfig = requireNonNull(projectConfig);
        this.gradingConfig = gradingConfig;
        this.displaySetting = requireNonNull(displaySetting);
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

    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public void setProjectConfig(ProjectConfig projectConfig) {
        this.projectConfig = requireNonNull(projectConfig);
    }

    public GradingConfig getGradingConfig() {
        return gradingConfig;
    }

    public void setGradingConfig(GradingConfig gradingConfig) {
        this.gradingConfig = gradingConfig;
    }

    public ZonedDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(ZonedDateTime deadline) {
        this.deadline = requireNonNull(deadline);
    }

    public DisplaySetting getDisplaySetting() {
        return displaySetting;
    }

    public void setDisplaySetting(DisplaySetting displaySetting) {
        this.displaySetting = requireNonNull(displaySetting);
    }

    public int getPercentageGoal() {
        return percentageGoal;
    }

    public void setPercentageGoal(int percentageGoal) {
        this.percentageGoal = percentageGoal;
    }

    public List<Solution> getSolutions() {
        return solutions;
    }

    public int solutionsWithSubmissions() {
        return (int) solutions.stream()
                .map(Solution::latestSubmission)
                .filter(Objects::nonNull)
                .count();
    }

    public boolean isRegisteringSolutions() {
        return registeringSolutions;
    }

    public void setRegisteringSolutions(boolean loadingSolutions) {
        this.registeringSolutions = loadingSolutions;
    }

    public enum DisplaySetting {
        WITH_FULL_NAMES, WITH_SHORTENED_NAMES, ANONYMOUS, HIDDEN
    }
}
