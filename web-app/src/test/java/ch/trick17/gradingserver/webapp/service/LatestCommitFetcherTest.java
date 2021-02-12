package ch.trick17.gradingserver.webapp.service;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatestCommitFetcherTest {

    @Test
    void fetchLatestCommit() throws GitAPIException, IOException {
        var latest = new LatestCommitFetcher().fetchLatestCommit(
                "https://gitlab.com/rolves-private-group/some-private-group/rolve.git",
                "5jiBFYSUisc-xbpCyLAW");
        assertEquals(Optional.of("e876d3e1d934879958937574aba3fa25b57f5ab5"), latest);
    }

    @Test
    void noCommitYet() throws GitAPIException, IOException {
        var latest = new LatestCommitFetcher().fetchLatestCommit(
                "https://gitlab.com/rolves-private-group/some-private-group/mike.git",
                "5jiBFYSUisc-xbpCyLAW");
        assertEquals(Optional.empty(), latest);
    }
}
