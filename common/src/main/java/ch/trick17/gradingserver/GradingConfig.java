package ch.trick17.gradingserver;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Embeddable
public class GradingConfig {

    @Column
    private String testClass;
    private String projectRoot;
    private ProjectStructure structure;
    @Column
    private GradingOptions options;

    protected GradingConfig() {}

    @JsonCreator
    public GradingConfig(String testClass, String projectRoot,
                         ProjectStructure structure, GradingOptions options) {
        this.testClass = requireNonNull(testClass);
        this.projectRoot = requireNonNull(projectRoot);
        this.structure = requireNonNull(structure);
        this.options = requireNonNull(options);
    }

    public String getTestClass() {
        return testClass;
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public ProjectStructure getStructure() {
        return structure;
    }

    public GradingOptions getOptions() {
        return options;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GradingConfig) obj;
        return testClass.equals(that.testClass) && options.equals(that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(options, testClass);
    }

    @Override
    public String toString() {
        return "GradingConfig[" +
                "options=" + options + ", " +
                "testClass=" + testClass + ']';
    }

    public enum ProjectStructure {
        ECLIPSE, MAVEN
    }
}
