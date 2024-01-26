package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.GradingConfig;
import ch.trick17.gradingserver.webapp.model.*;
import ch.trick17.gradingserver.webapp.model.GitLabPushEvent.Project;
import ch.trick17.gradingserver.webapp.service.GradingService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhooksControllerTest {

    static final String HOST = "https://gitlab.com/";
    static final String GROUP = "rolves-private-group/some-subgroup";
    static final String TOKEN = "5jiBFYSUisc-xbpCyLAW";  // read-only token from dummy user

    @Mock
    SolutionRepository solRepo;
    @Mock
    ProblemSet problemSet;
    @Mock
    GradingConfig config;
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
        var before = "224e840dcda30e7a898b98d3bef8aa079114ef7b";
        var after = "e0ad1c713b80833375f9a4170f74b84ce4625096";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getProblemSet()).thenReturn(problemSet);
        when(sol.getBranch()).thenReturn("master");
        when(problemSet.getGradingConfig()).thenReturn(config);
        when(config.getProjectRoot()).thenReturn("");

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new Project(repoUrl),
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
        var event = new GitLabPushEvent(new Project(repoUrl),
                "refs/heads/master", before, after, "rolve-gitlab-test-user");
        controller.gitlabPush(event);

        verifyNoInteractions(submissionRepo);
        verifyNoInteractions(gradingService);
    }

    @Test
    void gitlabPushOutsideProjectRoot() throws GitAPIException {
        var repoUrl = HOST + GROUP + "/rolve2.git";
        var before = "f42abd5f309d61fd69c1a76eadb7b546cdf727c3";
        var after = "392dc0cc14c5be947310aff311264c22265adcb3";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getProblemSet()).thenReturn(problemSet);
        when(sol.getRepoUrl()).thenReturn(repoUrl);
        when(sol.getBranch()).thenReturn("master");
        when(sol.getAccessToken()).thenReturn(new AccessToken(
                new User("name", "pass", "User"), HOST, TOKEN));
        when(problemSet.getGradingConfig()).thenReturn(config);
        when(config.getProjectRoot()).thenReturn("foo");

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new Project(repoUrl),
                "refs/heads/master", before, after, "rolve");
        controller.gitlabPush(event);

        verifyNoInteractions(submissionRepo);
        verifyNoInteractions(gradingService);
    }

    @Test
    void gitlabPushInsideProjectRoot() throws GitAPIException {
        var repoUrl = HOST + GROUP + "/rolve2.git";
        var before = "f42abd5f309d61fd69c1a76eadb7b546cdf727c3";
        var after = "392dc0cc14c5be947310aff311264c22265adcb3";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getProblemSet()).thenReturn(problemSet);
        when(sol.getRepoUrl()).thenReturn(repoUrl);
        when(sol.getBranch()).thenReturn("master");
        when(sol.getAccessToken()).thenReturn(new AccessToken(
                new User("name", "pass", "User"), HOST, TOKEN));
        when(problemSet.getGradingConfig()).thenReturn(config);
        when(config.getProjectRoot()).thenReturn("bar");

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new Project(repoUrl),
                "refs/heads/master", before, after, "rolve");
        controller.gitlabPush(event);

        var expected = ArgumentCaptor.forClass(Submission.class);
        var submission = verify(submissionRepo).save(expected.capture());
        assertEquals(sol, expected.getValue().getSolution());
        assertEquals(after, expected.getValue().getCommitHash());
        verify(gradingService).grade(submission);
    }
}
