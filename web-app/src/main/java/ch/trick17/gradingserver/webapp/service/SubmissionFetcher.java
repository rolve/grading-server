package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.Credentials;
import ch.trick17.gradingserver.webapp.model.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Comparator.comparingInt;
import static java.util.Optional.empty;
import static java.util.regex.Pattern.compile;

@Service
public class SubmissionFetcher {

    private static final Pattern HOST_PATTERN = compile("https?://([^/]+)/.*");

    private final LatestCommitFetcher commitFetcher;
    private final SubmissionRepository repo;
    private final HostCredentialsRepository credRepo;

    public SubmissionFetcher(LatestCommitFetcher commitFetcher, SubmissionRepository repo,
                             HostCredentialsRepository credRepo) {
        this.commitFetcher = commitFetcher;
        this.repo = repo;
        this.credRepo = credRepo;
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
        if (commitHash == null || repo.existsByCommitHash(commitHash)) {
            return empty();
        }

        var submission = new Submission(sol, commitHash);
        sol.getSubmissions().add(submission);
        repo.save(submission);
        return Optional.of(submission);
    }
}
