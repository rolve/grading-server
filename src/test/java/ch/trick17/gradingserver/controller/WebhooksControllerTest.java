package ch.trick17.gradingserver.controller;

import ch.trick17.gradingserver.model.*;
import ch.trick17.gradingserver.service.GradingService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure.ECLIPSE;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhooksControllerTest {

    static final String HOST = "https://gitlab.com/";
    static final String GROUP = "rolves-private-group/some-subgroup";
    static final String TOKEN = "glpat-pzyGzruzoVgEnQQPosZB";  // read-only token from dummy user

    @Mock
    SolutionRepository solRepo;
    @Mock
    ProblemSet problemSet;
    @Mock
    Solution sol;
    @Mock
    SubmissionRepository submissionRepo;
    @Mock
    GradingService gradingService;

    @BeforeEach
    void setupMocks() {
        when(sol.getIgnoredPushers()).thenReturn(List.of("rolve-gitlab-test-user"));
    }

    @Test
    void gitlabPush() throws GitAPIException {
        var repoUrl = HOST + GROUP + "/mike.git";
        var before = "073413c2284ba8a3f46f96924ac86dbf45278f59";
        var after = "dd99a20445be20575bebfc54195157618cbc51de";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getProblemSet()).thenReturn(problemSet);
        when(sol.getRepoUrl()).thenReturn(repoUrl);
        when(sol.getBranch()).thenReturn("master");
        when(sol.getAccessToken()).thenReturn(new AccessToken(
                new User("name", "pass", "User"), HOST, TOKEN));
        when(problemSet.getProjectConfig()).thenReturn(
                new ProjectConfig("", ECLIPSE, null, emptyList()));

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new GitLabPushEvent.Project(repoUrl),
                "refs/heads/master", before, after, "mike-trick17");
        controller.gitlabPush(event);

        var expected = ArgumentCaptor.forClass(Submission.class);
        var submission = verify(submissionRepo).save(expected.capture());
        assertEquals(sol, expected.getValue().getSolution());
        assertEquals(after, expected.getValue().getCommitHash());
        verify(gradingService).grade(submission);
    }

    @Test
    void gitlabPushIgnoredAuthor() throws GitAPIException {
        var repoUrl = HOST + GROUP + "/mike.git";
        var before = "8c52ee38ff0445dd76f52aa6d48d6a2f1ca716fb";
        var after = "5bf40ce3e32f8737f60ffc3002b0acf0b4e38702";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getBranch()).thenReturn("master");

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new GitLabPushEvent.Project(repoUrl),
                "refs/heads/master", before, after, "rolve-gitlab-test-user");
        controller.gitlabPush(event);

        verifyNoInteractions(submissionRepo);
        verifyNoInteractions(gradingService);
    }

    @Test
    void gitlabPushOutsideProjectRoot() throws GitAPIException {
        var repoUrl = HOST + GROUP + "/rolve2.git";
        // everything outside /foo
        var before = "260b646bdf32665178e94415e11402c060de9364";
        var after = "e7eb4cc0d0d2bb0a2da503a06452455984aeacad";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getProblemSet()).thenReturn(problemSet);
        when(sol.getRepoUrl()).thenReturn(repoUrl);
        when(sol.getBranch()).thenReturn("master");
        when(sol.getAccessToken()).thenReturn(new AccessToken(
                new User("name", "pass", "User"), HOST, TOKEN));
        when(problemSet.getProjectConfig()).thenReturn(
                new ProjectConfig("foo", ECLIPSE, null, emptyList()));

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new GitLabPushEvent.Project(repoUrl),
                "refs/heads/master", before, after, "rolve");
        controller.gitlabPush(event);

        verifyNoInteractions(submissionRepo);
        verifyNoInteractions(gradingService);
    }

    @Test
    void gitlabPushInsideProjectRoot() throws GitAPIException {
        var repoUrl = HOST + GROUP + "/rolve2.git";
        // inside /foo
        var before = "e743fce70bc7b21b3d216a85ec29ae854e585f4d";
        var after = "260b646bdf32665178e94415e11402c060de9364";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getProblemSet()).thenReturn(problemSet);
        when(sol.getRepoUrl()).thenReturn(repoUrl);
        when(sol.getBranch()).thenReturn("master");
        when(sol.getAccessToken()).thenReturn(new AccessToken(
                new User("name", "pass", "User"), HOST, TOKEN));
        when(problemSet.getProjectConfig()).thenReturn(
                new ProjectConfig("foo", ECLIPSE, null, emptyList()));

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new GitLabPushEvent.Project(repoUrl),
                "refs/heads/master", before, after, "rolve");
        controller.gitlabPush(event);

        var expected = ArgumentCaptor.forClass(Submission.class);
        var submission = verify(submissionRepo).save(expected.capture());
        assertEquals(sol, expected.getValue().getSolution());
        assertEquals(after, expected.getValue().getCommitHash());
        verify(gradingService).grade(submission);
    }

    @Test
    void gitlabPushOutsidePackage() throws GitAPIException {
        var repoUrl = HOST + GROUP + "/rolve3.git";
        // outside foo.bar package
        var before = "195f821910b7a0775a975c53facbccbbcb4bb596";
        var after = "77d028e285e8f3b3078993d66972a7f6fd6cc27b";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getProblemSet()).thenReturn(problemSet);
        when(sol.getRepoUrl()).thenReturn(repoUrl);
        when(sol.getBranch()).thenReturn("main");
        when(sol.getAccessToken()).thenReturn(new AccessToken(
                new User("name", "pass", "User"), HOST, TOKEN));
        when(problemSet.getProjectConfig()).thenReturn(
                new ProjectConfig("foo", ECLIPSE, "foo.bar", emptyList()));

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new GitLabPushEvent.Project(repoUrl),
                "refs/heads/main", before, after, "rolve");
        controller.gitlabPush(event);

        verifyNoInteractions(submissionRepo);
        verifyNoInteractions(gradingService);
    }

    @Test
    void gitlabPushInsidePackage() throws GitAPIException {
        var repoUrl = HOST + GROUP + "/rolve3.git";
        // inside foo.bar package
        var before = "e7d265e2fa005a660aec8458fca8afdfa9a3e5be";
        var after = "ba0dd4832b5315e3a8e15bebad9183d81cae3e28";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getProblemSet()).thenReturn(problemSet);
        when(sol.getRepoUrl()).thenReturn(repoUrl);
        when(sol.getBranch()).thenReturn("main");
        when(sol.getAccessToken()).thenReturn(new AccessToken(
                new User("name", "pass", "User"), HOST, TOKEN));
        when(problemSet.getProjectConfig()).thenReturn(
                new ProjectConfig("foo", ECLIPSE, "foo.bar", emptyList()));

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new GitLabPushEvent.Project(repoUrl),
                "refs/heads/main", before, after, "rolve");
        controller.gitlabPush(event);

        var expected = ArgumentCaptor.forClass(Submission.class);
        var submission = verify(submissionRepo).save(expected.capture());
        assertEquals(sol, expected.getValue().getSolution());
        assertEquals(after, expected.getValue().getCommitHash());
        verify(gradingService).grade(submission);
    }
}
