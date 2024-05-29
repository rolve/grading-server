package ch.trick17.gradingserver.model;

import javax.persistence.Embeddable;
import javax.persistence.ManyToMany;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Embeddable
public class ProjectConfig {

    private static final String PACKAGE_PATTERN = "[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*";

    private String projectRoot;
    private ProjectStructure structure;
    private String packageFilter;       // null means no filter
    @ManyToMany
    private List<JarFile> dependencies;

    protected ProjectConfig() {
    }

    /**
     * @param packageFilter When not <code>null</code>, only classes in that
     *                      package and subpackages be considered. For example,
     *                      "foo.bar" will include classes in the packages
     *                      "foo.bar", "foo.bar.baz", and "foo.bar.baz.qux" but
     *                      not, e.g., "foo", "bar", or "foo.barista".
     */
    public ProjectConfig(String projectRoot, ProjectStructure structure,
                         String packageFilter, List<JarFile> dependencies) {
        if (packageFilter != null && !packageFilter.matches(PACKAGE_PATTERN)) {
            throw new IllegalArgumentException("Invalid package filter: " + packageFilter);
        }
        this.projectRoot = requireNonNull(projectRoot);
        this.structure = requireNonNull(structure);
        this.packageFilter = packageFilter;
        this.dependencies = new ArrayList<>(dependencies);
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public ProjectStructure getStructure() {
        return structure;
    }

    public String getPackageFilter() {
        return packageFilter;
    }

    public List<JarFile> getDependencies() {
        return dependencies;
    }

    public enum ProjectStructure {
        ECLIPSE(Path.of("src"), Path.of("test")),
        MAVEN(Path.of("src/main/java"), Path.of("src/test/java"));

        public final Path srcDirPath;
        public final Path testDirPath;

        ProjectStructure(Path srcDirPath, Path testDirPath) {
            this.srcDirPath = srcDirPath;
            this.testDirPath = testDirPath;
        }
    }

    public Path getSrcDirPath() {
        return Path.of(projectRoot).resolve(structure.srcDirPath);
    }

    public Path getTestDirPath() {
        return Path.of(projectRoot).resolve(structure.testDirPath);
    }
}
