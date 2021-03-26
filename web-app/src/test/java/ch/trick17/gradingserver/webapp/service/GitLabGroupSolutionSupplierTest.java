package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.service.SolutionSupplier.NewSolution;
import org.gitlab4j.api.GitLabApiException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GitLabGroupSolutionSupplierTest {

    static final String HOST = "https://gitlab.com/";
    static final String GROUP = "rolves-private-group/some-subgroup";
    static final String TOKEN = "5jiBFYSUisc-xbpCyLAW";  // read-only token from dummy user

    static List<NewSolution> solutions;

    @BeforeAll
    static void getSolutions() throws GitLabApiException {
        // not so nice to do this here, but saves quite some time
        solutions = new GitLabGroupSolutionSupplier(HOST, GROUP, TOKEN).get(emptyList());
    }

    @Test
    void solutionsFound() {
        assertThat(solutions).hasSize(3);
    }

    @Test
    void ignoredPushers() {
        assertThat(solutions).isNotEmpty();
        solutions.forEach(sol -> assertEquals(Set.of("rolve-gitlab-test-user"), sol.ignoredPushers()));
    }

    @Test
    void repoUrlAndAuthorNames() {
        var repoRolve = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        assertEquals(Set.of("rolve"), repoRolve.authorNames());

        var repoMichael = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/michael.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        assertEquals(Set.of("michael-trick17"), repoMichael.authorNames());

        var repoMike = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/mike.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        assertEquals(Set.of("mike-trick17"), repoMike.authorNames());
    }

    @Test
    void latestCommitHash() {
        var repoRolve = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/rolve.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // multiple pushes, but all from the group owner, who is ignored
        assertNull(repoRolve.latestCommitHash());

        var repoMichael = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/michael.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // no pushes at all
        assertNull(repoMichael.latestCommitHash());

        var repoMike = solutions.stream()
                .filter(s -> s.repoUrl().equals(HOST + GROUP + "/mike.git"))
                .findFirst().orElseThrow(AssertionFailedError::new);
        // multiple pushes from non-ignored user, followed by pushes from
        // group owner (later), which should be ignored. Also, commit author
        // (should be irrelevant) is different from pusher and the last push
        // from the non-ignored user contains two commits
        assertEquals("e0ad1c713b80833375f9a4170f74b84ce4625096", repoMike.latestCommitHash());
    }
}
