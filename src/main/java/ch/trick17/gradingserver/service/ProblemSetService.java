package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.time.Instant.now;

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

    public Optional<ProblemSet> findById(int id) {
        return repo.findById(id);
    }

    public ProblemSet save(ProblemSet problemSet) {
        return repo.save(problemSet);
    }

    public void prepareAndSave(ProblemSet problemSet, List<String> refTestSuite,
                               List<String> refImplementation) {
        problemSet = repo.save(problemSet);
        gradingService.prepare(problemSet, refTestSuite, refImplementation);
    }

    @Transactional
    public void setConfig(ProblemSet problemSet, GradingConfig config) {
        problemSet.setGradingConfig(config);
        repo.save(problemSet);
    }

    @Async
    public <E extends Exception> void registerSolutions(int problemSetId, int tokenId,
                                                        SolutionSupplier<E> supplier)
            throws E, GitAPIException {
        var tx = txManager.getTransaction(new DefaultTransactionDefinition());
        var problemSet = repo.findById(problemSetId).orElseThrow();
        logger.info("Registering solutions for {} from {}", problemSet.getName(), supplier);

        var newSubmissions = new ArrayList<Submission>();
        var newSolCount = 0;
        try {
            var token = tokenRepo.findById(tokenId).orElseThrow();
            var existingSolutions = problemSet.getSolutions();
            var newSolutions = supplier.get(existingSolutions);
            for (var newSol : newSolutions) {
                var authors = new ArrayList<Author>();
                for (var name : newSol.authorNames()) {
                    var existing = authorRepo.findByUsername(name);
                    if (existing.isPresent()) {
                        authors.add(existing.get());
                    } else {
                        authors.add(new Author(name));
                    }
                }
                var sol = new Solution(problemSet, newSol.repoUrl(), newSol.branch(),
                        token, authors, newSol.ignoredPushers());
                if (newSol.latestCommitHash() != null) {
                    var subm = new Submission(sol, newSol.latestCommitHash(), now());
                    sol.getSubmissions().add(subm);
                    newSubmissions.add(subm);
                }
                newSolCount++;
            }
        } finally {
            problemSet.setRegisteringSolutions(false);
            repo.save(problemSet);
            txManager.commit(tx); // need to commit the new solutions before starting to grade
        }

        newSubmissions.forEach(gradingService::grade); // async

        logger.info("{} new solutions registered", newSolCount);
    }

    public void delete(ProblemSet problemSet) {
        repo.delete(problemSet);
    }
}
