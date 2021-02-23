package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.service.SolutionSupplier.SolutionInfo;
import org.gitlab4j.api.GitLabApiException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GitLabGroupSolutionSupplierTest {

    @Test
    void privateSubgroup() throws GitLabApiException {
        var host = "https://gitlab.com/";
        var group = "rolves-private-group/some-subgroup";
        var supplier = new GitLabGroupSolutionSupplier(host,
                group, "5jiBFYSUisc-xbpCyLAW"); // read-only token from dummy user
        var solutions = supplier.get();
        assertThat(solutions).hasSize(3);
        assertThat(solutions).contains(
                new SolutionInfo(host + group + "/rolve.git", Set.of("rolve"), null));
        assertThat(solutions).contains(
                new SolutionInfo(host + group + "/michael.git", Set.of("michael-trick17"), null));
        assertThat(solutions).contains(
                new SolutionInfo(host + group + "/mike.git", Set.of("mike-trick17"), null));
    }
}
