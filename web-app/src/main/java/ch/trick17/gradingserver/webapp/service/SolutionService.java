package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.model.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;

import static java.util.stream.Collectors.toSet;

@Service
public class SolutionService {

    private static final Logger logger = LoggerFactory.getLogger(SolutionService.class);

    private final SolutionRepository repo;
    private final ProblemSetRepository problemSetRepo;
    private final AuthorRepository authorRepo;
    private final SubmissionService fetcher;
    private final PlatformTransactionManager txManager;

    public SolutionService(SolutionRepository repo, ProblemSetRepository problemSetRepo,
                           AuthorRepository authorRepo, SubmissionService fetcher,
                           PlatformTransactionManager txManager) {
        this.repo = repo;
        this.problemSetRepo = problemSetRepo;
        this.authorRepo = authorRepo;
        this.fetcher = fetcher;
        this.txManager = txManager;
    }

    @Async
    public <E extends Exception> void registerSolutions(int problemSetId,
                                                        SolutionSupplier<E> supplier)
            throws E, GitAPIException {
        var tx = txManager.getTransaction(new DefaultTransactionDefinition());
        var problemSet = problemSetRepo.findById(problemSetId).get();
        logger.info("Registering solutions for {} from {}", problemSet.getName(), supplier);
        var sols = new ArrayList<Solution>();
        try {
            var existingSols = problemSet.getSolutions().stream()
                    .map(Solution::getRepoUrl)
                    .collect(toSet());

            for (var info : supplier.get()) {
                if (existingSols.contains(info.repoUrl())) {
                    continue;
                }
                var authors = new ArrayList<Author>();
                for (var name : info.authorNames()) {
                    var existing = authorRepo.findByName(name);
                    if (existing.isPresent()) {
                        authors.add(existing.get());
                    } else {
                        authors.add(new Author(name));
                    }
                }
                var sol = new Solution(problemSet, info.repoUrl(), authors, info.ignoredInitialCommit());
                sol.setFetchingSubmission(true);
                repo.save(sol);
                sols.add(sol);
            }
        } finally {
            problemSet.setRegisteringSolutions(false);
            problemSetRepo.save(problemSet);
            txManager.commit(tx); // need to commit before calling fetch below
        }
        logger.info("{} solutions registered", sols.size());

        for (var sol : sols) {
            fetcher.fetchSubmission(sol.getId());
        }
    }
}
