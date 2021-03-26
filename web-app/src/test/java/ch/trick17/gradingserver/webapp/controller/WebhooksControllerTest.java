package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.*;
import ch.trick17.gradingserver.webapp.model.GitLabPushEvent.Project;
import ch.trick17.gradingserver.webapp.service.GradingService;
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

    @Mock
    SolutionRepository solRepo;
    @Mock
    Solution sol;
    @Mock
    SubmissionRepository submissionRepo;
    @Mock
    GradingService gradingService;

    @Test
    void gitlabPush() {
        var repoUrl = HOST + GROUP + "/mike.git";
        var commit = "e0ad1c713b80833375f9a4170f74b84ce4625096";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getIgnoredPushers()).thenReturn(List.of("rolve-gitlab-test-user"));

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new Project(repoUrl),
                "refs/heads/master", commit, "mike-trick17");
        controller.gitlabPush(event);

        var expected = ArgumentCaptor.forClass(Submission.class);
        var submission = verify(submissionRepo).save(expected.capture());
        assertEquals(sol, expected.getValue().getSolution());
        assertEquals(commit, expected.getValue().getCommitHash());
        verify(gradingService).grade(submission);
    }

    @Test
    void gitlabPushIgnored() {
        var repoUrl = HOST + GROUP + "/mike.git";
        var commit = "5bf40ce3e32f8737f60ffc3002b0acf0b4e38702";

        when(solRepo.findByRepoUrl(repoUrl)).thenReturn(List.of(sol));
        when(sol.getIgnoredPushers()).thenReturn(List.of("rolve-gitlab-test-user"));

        var controller = new WebhooksController(solRepo, submissionRepo, gradingService);
        var event = new GitLabPushEvent(new Project(repoUrl),
                "refs/heads/master", commit, "rolve-gitlab-test-user");
        controller.gitlabPush(event);

        verifyNoInteractions(submissionRepo);
        verifyNoInteractions(gradingService);
    }
}
