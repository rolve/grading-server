package ch.trick17.gradingserver;

import ch.trick17.gradingserver.util.UriListConverter;
import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Lob;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;

@Embeddable
public class GradingConfig {

    @Lob
    private String testClass;
    private String projectRoot;
    private ProjectStructure structure;
    @Lob
    @Convert(converter = UriListConverter.class)
    private List<URI> dependencyUrls;
    private GradingOptions options;

    protected GradingConfig() {}

    @JsonCreator
    public GradingConfig(String testClass, String projectRoot,
                         ProjectStructure structure, List<URI> dependencyUrls,
                         GradingOptions options) {
        this.testClass = requireNonNull(testClass);
        this.projectRoot = requireNonNull(projectRoot);
        this.structure = requireNonNull(structure);
        this.dependencyUrls = copyOf(dependencyUrls);
        this.options = requireNonNull(options);
        if (dependencyUrls.stream().anyMatch(uri -> !uri.getScheme().matches("https?"))) {
            throw new IllegalArgumentException("only http(s) URLs allowed");
        }
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

    public List<URI> getDependencyUrls() {
        return dependencyUrls;
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
