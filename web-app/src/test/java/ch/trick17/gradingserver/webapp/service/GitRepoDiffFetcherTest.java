package ch.trick17.gradingserver.webapp.service;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitRepoDiffFetcherTest {

    static final String REPO = "https://gitlab.com/rolves-private-group/some-subgroup/rolve.git";
    static final String TOKEN = "5jiBFYSUisc-xbpCyLAW";  // read-only token from dummy user

    @Test
    void create() throws GitAPIException, IOException {
        var fetcher = new GitRepoDiffFetcher(REPO, "", TOKEN);
        var paths = fetcher.affectedPaths(
                "28ee26ac5c662e0be8e5afdfffacad07141e4487",
                "e96481f61e8333245bd2d89cd943b75136782047");
        assertEquals(Set.of("foo/.gitkeep"), paths);
    }

    @Test
    void delete() throws GitAPIException, IOException {
        var fetcher = new GitRepoDiffFetcher(REPO, "", TOKEN);
        var paths = fetcher.affectedPaths(
                "5dcf0093c3cf97d50bba5b316a77586d6eb7c3f3",
                "f09d278dce2255cf38bf1b002b66aabb15a0ab8f");
        assertEquals(Set.of("bar/bar.txt"), paths);
    }

    @Test
    void modify() throws GitAPIException, IOException {
        var fetcher = new GitRepoDiffFetcher(REPO, "", TOKEN);
        var paths = fetcher.affectedPaths(
                "e876d3e1d934879958937574aba3fa25b57f5ab5",
                "28ee26ac5c662e0be8e5afdfffacad07141e4487");
        assertEquals(Set.of("README.md"), paths);
    }

    @Test
    void multipleCommits() throws GitAPIException, IOException {
        var fetcher = new GitRepoDiffFetcher(REPO, "", TOKEN);
        var paths = fetcher.affectedPaths(
                "28ee26ac5c662e0be8e5afdfffacad07141e4487",
                "5dcf0093c3cf97d50bba5b316a77586d6eb7c3f3");
        assertEquals(Set.of("foo/foo.txt", "bar/bar.txt"), paths); // .gitkeep created and deleted
    }

    @Test
    void fromStart() throws GitAPIException, IOException {
        var fetcher = new GitRepoDiffFetcher(REPO, "", TOKEN);
        var paths = fetcher.affectedPaths(
                null,
                "e876d3e1d934879958937574aba3fa25b57f5ab5");
        assertEquals(Set.of("README.md"), paths);

        paths = fetcher.affectedPaths(
                null,
                "5dcf0093c3cf97d50bba5b316a77586d6eb7c3f3");
        assertEquals(Set.of("README.md", "foo/foo.txt", "bar/bar.txt"), paths);
    }
}
