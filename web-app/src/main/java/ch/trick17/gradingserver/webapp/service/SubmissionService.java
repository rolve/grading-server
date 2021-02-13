package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.Credentials;
import ch.trick17.gradingserver.webapp.model.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.regex.Pattern;

import static java.util.Comparator.comparingInt;
import static java.util.regex.Pattern.compile;

@Service
public class SubmissionService {

    private static final Pattern HOST_PATTERN = compile("https?://([^/]+)/.*");

    private final SubmissionRepository repo;
    private final HostCredentialsRepository credRepo;
    private final SolutionRepository solRepo;
    private final LatestCommitFetcher fetcher;

    public SubmissionService(SubmissionRepository repo, HostCredentialsRepository credRepo,
                             SolutionRepository solRepo, LatestCommitFetcher fetcher) {
        this.repo = repo;
        this.credRepo = credRepo;
        this.solRepo = solRepo;
        this.fetcher = fetcher;
    }

    @Async
    @Transactional
    public void fetchSubmission(int solId) throws GitAPIException {
        var sol = solRepo.findById(solId).get();
        try {
            var matcher = HOST_PATTERN.matcher(sol.getRepoUrl());
            if (!matcher.matches()) {
                throw new AssertionError("invalid repo url");
            }
            var token = credRepo.findByHost(matcher.group(1)).stream()
                    .max(comparingInt(HostCredentials::getId))
                    .map(HostCredentials::getCredentials)
                    .map(Credentials::getPassword)
                    .orElse(null);
            fetcher.fetchLatestCommit(sol.getRepoUrl(), token).ifPresent(commitHash -> {
                if (!repo.existsByCommitHash(commitHash)) {
                    repo.save(new Submission(sol, commitHash));
                }
            });
        } finally {
            sol.setFetchingSubmission(false);
            solRepo.save(sol);
        }
        // TODO: start grading
    }
}
