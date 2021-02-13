package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.Credentials;
import ch.trick17.gradingserver.webapp.model.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Comparator.comparingInt;
import static java.util.Optional.empty;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toSet;

@Service
public class SolutionService {

    private static final Pattern HOST_PATTERN = compile("https?://([^/]+)/.*");

    private final LatestCommitFetcher commitFetcher;
    private final SolutionRepository repo;
    private final ProblemSetRepository problemSetRepo;
    private final SubmissionRepository submissionRepo;
    private final HostCredentialsRepository credRepo;
    private final AuthorRepository authorRepo;

    public SolutionService(SolutionRepository repo, ProblemSetRepository problemSetRepo,
                           SubmissionRepository submissionRepo, HostCredentialsRepository credRepo,
                           AuthorRepository authorRepo, LatestCommitFetcher commitFetcher) {
        this.repo = repo;
        this.problemSetRepo = problemSetRepo;
        this.submissionRepo = submissionRepo;
        this.credRepo = credRepo;
        this.authorRepo = authorRepo;
        this.commitFetcher = commitFetcher;
    }

    @Async
    @Transactional
    public <E extends Exception> void registerSolutions(int problemSetId,
                                                        SolutionSupplier<E> supplier)
            throws E, GitAPIException {
        var problemSet = problemSetRepo.findById(problemSetId).get();
        var existingSols = problemSet.getSolutions().stream()
                .map(Solution::getRepoUrl)
                .collect(toSet());

        var sols = new ArrayList<Solution>();
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
            var sol = new Solution(info.repoUrl(), authors);
            sol.setProblemSet(problemSet);
            repo.save(sol);
            sols.add(sol);
        }
        problemSet.setRegisteringSolutions(false);
        problemSetRepo.save(problemSet);

        for (var sol : sols) {
            var submission = fetchSubmission(sol);
            // TODO: start grading
        }
    }

    public Optional<Submission> fetchSubmission(Solution sol) throws GitAPIException {
        var matcher = HOST_PATTERN.matcher(sol.getRepoUrl());
        if (!matcher.matches()) {
            throw new AssertionError("invalid repo url");
        }
        var token = credRepo.findByHost(matcher.group(1)).stream()
                .max(comparingInt(HostCredentials::getId))
                .map(HostCredentials::getCredentials)
                .map(Credentials::getPassword)
                .orElse(null);
        var commitHash = commitFetcher.fetchLatestCommit(sol.getRepoUrl(), token).orElse(null);
        if (commitHash == null || submissionRepo.existsByCommitHash(commitHash)) {
            return empty();
        }

        var submission = new Submission(sol, commitHash);
        sol.getSubmissions().add(submission);
        submissionRepo.save(submission);
        return Optional.of(submission);
    }
}
