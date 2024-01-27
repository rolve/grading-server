package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.Objects;

import static java.time.ZonedDateTime.now;

@Service
public class ProblemSetService {

    private static final Logger logger = LoggerFactory.getLogger(ProblemSetService.class);

    private final ProblemSetRepository repo;
    private final AuthorRepository authorRepo;
    private final AccessTokenRepository tokenRepo;
    private final GradingService gradingService;
    private final PlatformTransactionManager txManager;

    public ProblemSetService(ProblemSetRepository repo, AuthorRepository authorRepo,
                             AccessTokenRepository tokenRepo, GradingService gradingService,
                             PlatformTransactionManager txManager) {
        this.repo = repo;
        this.authorRepo = authorRepo;
        this.tokenRepo = tokenRepo;
        this.gradingService = gradingService;
        this.txManager = txManager;
    }

    @Async
    public <E extends Exception> void registerSolutions(int problemSetId, int tokenId,
                                                        SolutionSupplier<E> supplier)
            throws E, GitAPIException {
        var tx = txManager.getTransaction(new DefaultTransactionDefinition());
        var problemSet = repo.findById(problemSetId).orElseThrow();
        var token = tokenRepo.findById(tokenId).orElseThrow();
        var prevSols = problemSet.getSolutions().size();
        logger.info("Registering solutions for {} from {}", problemSet.getName(), supplier);
        try {
            var existingSols = problemSet.getSolutions();
            for (var newSol : supplier.get(existingSols)) {
                var authors = new ArrayList<Author>();
                for (var name : newSol.authorNames()) {
                    var existing = authorRepo.findByName(name);
                    if (existing.isPresent()) {
                        authors.add(existing.get());
                    } else {
                        authors.add(new Author(name));
                    }
                }
                var sol = new Solution(problemSet, newSol.repoUrl(), newSol.branch(), token, authors, newSol.ignoredPushers());
                if (newSol.latestCommitHash() != null) {
                    var subm = new Submission(sol, newSol.latestCommitHash(), now());
                    sol.getSubmissions().add(subm);
                }
            }
            // make sure submissions are loaded before transaction commits,
            // otherwise 'latestSubmission' below fails (LazyInitializationException)
            existingSols.forEach(Solution::latestSubmission);
        } finally {
            problemSet.setRegisteringSolutions(false);
            repo.save(problemSet);
            txManager.commit(tx); // need to commit the new solutions before starting to grade
        }

        problemSet.getSolutions().stream()
                .map(Solution::latestSubmission)
                .filter(Objects::nonNull)
                .forEach(gradingService::grade); // async

        logger.info("{} solutions registered", problemSet.getSolutions().size() - prevSols);
    }
}
