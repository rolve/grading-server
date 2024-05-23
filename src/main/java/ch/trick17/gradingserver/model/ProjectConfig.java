package ch.trick17.gradingserver.model;

import javax.persistence.Embeddable;
import javax.persistence.ManyToMany;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Embeddable
public class ProjectConfig {

    private String projectRoot;
    private ProjectStructure structure;
    private String packageFilter;       // null means no filter
    @ManyToMany
    private List<JarFile> dependencies;

    protected ProjectConfig() {
    }

    public ProjectConfig(String projectRoot, ProjectStructure structure, String packageFilter,
                         List<JarFile> dependencies) {
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
