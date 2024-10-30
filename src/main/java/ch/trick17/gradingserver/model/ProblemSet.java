package ch.trick17.gradingserver.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static java.util.Objects.requireNonNull;

@Entity
public class ProblemSet {

    @Id
    @GeneratedValue
    private int id;
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Course course;
    private String name;
    private ProjectConfig projectConfig;
    @Type(JsonType.class)
    private GradingConfig gradingConfig;
    private ZonedDateTime deadline;
    @Column(columnDefinition = "integer")
    private DisplaySetting displaySetting;
    private int percentageGoal;
    private boolean registeringSolutions = false;

    @OneToMany(mappedBy = "problemSet", cascade = ALL, orphanRemoval = true)
    private final List<Solution> solutions = new ArrayList<>();

    @Formula("""
            (SELECT COUNT(*)
            FROM solution sol
            WHERE sol.problem_set_id = id AND EXISTS (
                SELECT 1
                FROM submission sub
                WHERE sub.solution_id = sol.id
            ))
            """)
    private int solutionsWithSubmissions;

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
        return solutionsWithSubmissions;
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
