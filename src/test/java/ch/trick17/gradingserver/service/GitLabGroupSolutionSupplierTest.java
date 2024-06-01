package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.ProjectConfig;
import ch.trick17.gradingserver.service.SolutionSupplier.NewSolution;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitlab4j.api.GitLabApiException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.List;
import java.util.Set;

import static ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure.ECLIPSE;
import static ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure.MAVEN;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class GitLabGroupSolutionSupplierTest {

    static final String HOST = "https://gitlab.com/";
    static final String GROUP = "rolves-private-group/some-subgroup";
    static final String TOKEN = "glpat-pzyGzruzoVgEnQQPosZB"; // read-only token from dummy user
    static final ProjectConfig CONFIG = new ProjectConfig("", ECLIPSE, null, emptyList());;

    static List<NewSolution> solutions;

    @BeforeAll
    static void getSolutions() throws GitLabApiException, GitAPIException {
        // not so nice to do this here, but saves quite some time
        var supplier = new GitLabGroupSolutionSupplier(HOST, GROUP, TOKEN, CONFIG);
        assertTrue(supplier.isIgnoringAuthorless());
        solutions = supplier.get(emptyList());
    }

    @Test
    void solutionsFound() {
        assertThat(solutions).hasSize(5);
    }

    @Test
    void ignoredPushers() {
        assertThat(solutions).isNotEmpty();
        solutions.forEach(sol -> assertEquals(Set.of("rolve-gitlab-test-user"), sol.ignoredPushers()));
    }

    @Test
    void repoUrlAndAuthorNames() {
        var solRolve = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        assertEquals(Set.of("rolve"), solRolve.authorNames());

        var solMichael = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/michael.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        assertEquals(Set.of("michael-trick17"), solMichael.authorNames());

        var solMike = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/mike.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        assertEquals(Set.of("mike-trick17"), solMike.authorNames());
    }

    @Test
    void latestCommitHash() {
        var solRolve = solutions.stream()
                .filter(s -> s.repoUrl().endsWith("/rolve.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // multiple pushes, but all from the group owner, who is ignored
        assertNull(solRolve.latestCommitHash());

        var solMichael = solutions.stream()
                .filter(s -> s.repoUrl().endsWith("/michael.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // no pushes at all
        assertNull(solMichael.latestCommitHash());

        var solMike = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/mike.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // multiple pushes from non-ignored user, followed by pushes from
        // group owner (later), which should be ignored.
        assertEquals("759a46f5ec39c18d6184ea6f960e9d76ef19e688", solMike.latestCommitHash());
    }

    @Test
    void latestCommitHashWithProjectRoot() throws GitLabApiException, GitAPIException {
        var config = new ProjectConfig("foo", ECLIPSE, null, emptyList());
        var solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, TOKEN, config)
                .get(emptyList());
        var solRolve2 = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve2.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // last pushes did not change anything inside foo/, so should be ignored
        assertEquals("260b646bdf32665178e94415e11402c060de9364", solRolve2.latestCommitHash());

        config = new ProjectConfig("bar", MAVEN, null, emptyList());
        solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, TOKEN, config)
                .get(emptyList());
        solRolve2 = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve2.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // last push affected bar/src/test/java
        assertEquals("e7eb4cc0d0d2bb0a2da503a06452455984aeacad", solRolve2.latestCommitHash());

        config = new ProjectConfig("baz", ECLIPSE, null, emptyList());
        solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, TOKEN, config)
                .get(emptyList());
        solRolve2 = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve2.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // no push for baz/
        assertNull(solRolve2.latestCommitHash());
    }

    @Test
    void latestCommitHashWithPackageFilter() throws GitLabApiException, GitAPIException {
        var config = new ProjectConfig("foo", ECLIPSE, "foo.bar", emptyList());
        var solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, TOKEN, config)
                .get(emptyList());
        var solRolve3 = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve3.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // last two pushes did not change anything inside foo.bar, so should be ignored
        assertEquals("195f821910b7a0775a975c53facbccbbcb4bb596", solRolve3.latestCommitHash());

        config = new ProjectConfig("foo", ECLIPSE, "foo", emptyList());
        solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, TOKEN, config)
                .get(emptyList());
        solRolve3 = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve3.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // last push did not change anything inside foo package, so should be ignored
        assertEquals("6f3a18f4f8c39e8fce3fb9846346909b3b4cd1bd", solRolve3.latestCommitHash());

        config = new ProjectConfig("foo", ECLIPSE, null, emptyList());
        solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, TOKEN, config)
                .get(emptyList());
        solRolve3 = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve3.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // last push affected foo/src (no package)
        assertEquals("77d028e285e8f3b3078993d66972a7f6fd6cc27b", solRolve3.latestCommitHash());

        config = new ProjectConfig("foo", ECLIPSE, "baz", emptyList());
        solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, TOKEN, config)
                .get(emptyList());
        solRolve3 = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve3.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // no push affected baz package
        assertNull(solRolve3.latestCommitHash());
    }

    @Test
    void authorless() throws GitLabApiException, GitAPIException {
        var supplier = new GitLabGroupSolutionSupplier(HOST, GROUP, TOKEN, CONFIG);
        supplier.setIgnoringAuthorless(false);
        var solutions = supplier.get(emptyList());

        assertEquals(6, solutions.size());

        var authorlessSol = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/without-member.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        assertEquals(emptySet(), authorlessSol.authorNames());
    }
}
