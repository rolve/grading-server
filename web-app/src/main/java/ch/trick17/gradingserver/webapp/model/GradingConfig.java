package ch.trick17.gradingserver.webapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.Embeddable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Embeddable
public class GradingConfig {

    @Lob
    private String testClass;
    private String projectRoot;
    private ProjectStructure structure;
    @ManyToMany
    private List<JarFile> dependencies;
    private GradingOptions options;

    protected GradingConfig() {}

    @JsonCreator
    public GradingConfig(String testClass, String projectRoot,
                         ProjectStructure structure,
                         List<JarFile> dependencies,
                         GradingOptions options) {
        this.testClass = requireNonNull(testClass);
        this.projectRoot = requireNonNull(projectRoot);
        this.structure = requireNonNull(structure);
        this.dependencies = new ArrayList<>(dependencies);
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

    public List<JarFile> getDependencies() {
        return dependencies;
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

    public enum ProjectStructure {
        ECLIPSE, MAVEN
    }
}
