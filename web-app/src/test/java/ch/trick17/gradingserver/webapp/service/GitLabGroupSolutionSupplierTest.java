package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.service.SolutionSupplier.NewSolution;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitlab4j.api.GitLabApiException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class GitLabGroupSolutionSupplierTest {

    static final String HOST = "https://gitlab.com/";
    static final String GROUP = "rolves-private-group/some-subgroup";
    static final String TOKEN = "5jiBFYSUisc-xbpCyLAW";  // read-only token from dummy user

    static List<NewSolution> solutions;

    @BeforeAll
    static void getSolutions() throws GitLabApiException, GitAPIException {
        // not so nice to do this here, but saves quite some time
        var supplier = new GitLabGroupSolutionSupplier(HOST, GROUP, "", TOKEN);
        assertTrue(supplier.isIgnoringAuthorless());
        solutions = supplier.get(emptyList());
    }

    @Test
    void solutionsFound() {
        assertThat(solutions).hasSize(4);
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
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // multiple pushes, but all from the group owner, who is ignored
        assertNull(solRolve.latestCommitHash());

        var solMichael = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/michael.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // no pushes at all
        assertNull(solMichael.latestCommitHash());

        var solMike = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/mike.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // multiple pushes from non-ignored user, followed by pushes from
        // group owner (later), which should be ignored. Also, commit author
        // (should be irrelevant) is different from pusher and the last push
        // from the non-ignored user contains two commits
        assertEquals("e0ad1c713b80833375f9a4170f74b84ce4625096", solMike.latestCommitHash());
    }

    @Test
    void latestCommitHashWithProjectRoot() throws GitLabApiException, GitAPIException {
        var solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, "foo", TOKEN)
                .get(emptyList());
        var solRolve2 = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve2.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // last couple of pushes did not change anything inside foo/, so should be ignored
        assertEquals("5f4961d694e0224343cec6b1e999a15569e0343a", solRolve2.latestCommitHash());

        solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, "bar", TOKEN)
                .get(emptyList());
        solRolve2 = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve2.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // last push affected bar/
        assertEquals("392dc0cc14c5be947310aff311264c22265adcb3", solRolve2.latestCommitHash());

        solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, "baz", TOKEN)
                .get(emptyList());
        solRolve2 = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve2.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // no push for baz/
        assertNull(solRolve2.latestCommitHash());
    }

    @Test
    void authorless() throws GitLabApiException, GitAPIException {
        var supplier = new GitLabGroupSolutionSupplier(HOST, GROUP, "", TOKEN);
        supplier.setIgnoringAuthorless(false);
        var solutions = supplier.get(emptyList());

        assertEquals(5, solutions.size());

        var authorlessSol = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/without-member.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        assertEquals(emptySet(), authorlessSol.authorNames());
    }
}
