package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.*;
import ch.trick17.gradingserver.service.SolutionSupplier.NewSolution;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static ch.trick17.gradingserver.model.GradingOptions.Compiler.ECLIPSE;
import static ch.trick17.gradingserver.model.ProblemSet.DisplaySetting.WITH_SHORTENED_NAMES;
import static ch.trick17.gradingserver.model.ProjectConfig.ProjectStructure.MAVEN;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@DataJpaTest
public class ProblemSetServiceIT {

    static final String HOST = "https://example.com/";

    @Autowired
    ProblemSetRepository repo;
    @Autowired
    AuthorRepository authorRepo;
    @Autowired
    UserRepository userRepo;
    @Autowired
    AccessTokenRepository tokenRepo;
    @Autowired
    SubmissionRepository submissionRepo;
    @Mock
    GradingService gradingService;
    @Autowired
    PlatformTransactionManager txManager;

    ProblemSetService service;
    @Mock
    SolutionSupplier<RuntimeException> supplier;

    int tokenId;
    int problemSetId;

    @BeforeEach
    void setUp() {
        var tx = txManager.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        service = new ProblemSetService(repo, authorRepo, tokenRepo,
                submissionRepo, gradingService, txManager);

        var user = userRepo.save(new User("user", "password", "User"));
        var token = tokenRepo.save(new AccessToken(user, HOST, "token"));
        tokenId = token.getId();

        var course = new Course("OOPI2", new Term(2021, "FS"), "");
        var projectConfig = new ProjectConfig("", MAVEN, null, emptyList());
        var gradingConfig = new ImplGradingConfig("", new GradingOptions(ECLIPSE,
                7, Duration.ofSeconds(5), Duration.ofSeconds(10), true));
        var problemSet = new ProblemSet(course, "Test", projectConfig,
                gradingConfig, now(), WITH_SHORTENED_NAMES);
        problemSetId = repo.save(problemSet).getId();
        txManager.commit(tx);
    }

    @Test
    @DirtiesContext
    void registerSolutionsAllNew() throws GitAPIException {
        when(supplier.get(emptyList())).thenReturn(List.of(
                new NewSolution(HOST + "/repo0.git", "branch0",
                        Set.of("author0"), emptySet(), "hash0"),
                new NewSolution(HOST + "/repo1.git", "branch1",
                        Set.of("author1"), emptySet(), "hash1")));

        var tx = txManager.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        service.registerSolutions(problemSetId, tokenId, supplier); // not async, since instantiated directly

        // because latestSubmission() uses Hibernate @Formula, we need to commit
        // the changes to the database before loading the problem set
        txManager.commit(tx);

        var problemSet = repo.findById(problemSetId).orElseThrow();
        var solutions = problemSet.getSolutions();
        assertEquals(2, solutions.size());
        for (int i = 0; i < 2; i++) {
            var sol = solutions.get(i);
            assertEquals(HOST + "/repo" + i + ".git", sol.getRepoUrl());
            assertEquals("branch" + i, sol.getBranch());

            var authors = sol.getAuthors();
            assertEquals(1, authors.size());
            assertEquals("author" + i, authors.iterator().next().getUsername());

            var submission = sol.latestSubmission();
            assertFalse(submission.hasResult());
            assertFalse(submission.isGradingStarted());
            assertEquals("hash" + i, submission.getCommitHash());
        }

        var captor = ArgumentCaptor.forClass(Submission.class);
        verify(gradingService, times(2)).grade(captor.capture());
        var gradedSubmissions = captor.getAllValues();
        assertEquals("hash0", gradedSubmissions.get(0).getCommitHash());
        assertEquals("hash1", gradedSubmissions.get(1).getCommitHash());
    }

    @Test
    @DirtiesContext
    void registerSolutionsWithExisting() throws GitAPIException {
        var tx = txManager.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        var problemSet = repo.findById(problemSetId).orElseThrow();
        var token = tokenRepo.findById(tokenId).orElseThrow();

        var author = new Author("existing-author");
        var existingSol = new Solution(problemSet, "https://example.com/existing.git",
                "main", token, Set.of(author), emptySet());
        var submission = new Submission(existingSol, "hash", now());
        submission.setGradingStarted(true);
        submission.setResult(new OutdatedResult("easiest way to have a result"));
        existingSol.getSubmissions().add(submission);
        problemSet = repo.save(problemSet);
        var existingSolId = problemSet.getSolutions().getFirst().getId();

        when(supplier.get(any())).thenReturn(List.of(
                new NewSolution(HOST + "/new.git", "new-branch",
                        Set.of("new-author"), emptySet(), "new-hash")));

        service.registerSolutions(problemSetId, tokenId, supplier); // not async, since instantiated directly

        // see above
        txManager.commit(tx);

        problemSet = repo.findById(problemSetId).orElseThrow();
        var solutions = problemSet.getSolutions();
        assertEquals(2, solutions.size());
        existingSol = solutions.getFirst();
        assertEquals(existingSolId, existingSol.getId());
        assertTrue(existingSol.latestSubmission().isGradingStarted());
        assertTrue(existingSol.latestSubmission().hasResult());

        var newSolution = solutions.get(1);
        assertFalse(newSolution.latestSubmission().hasResult());
        assertFalse(newSolution.latestSubmission().isGradingStarted());

        var captor = ArgumentCaptor.forClass(Submission.class);
        verify(gradingService).grade(captor.capture());
        var gradedSubmission = captor.getValue();
        assertEquals("new-hash", gradedSubmission.getCommitHash());
    }
}
