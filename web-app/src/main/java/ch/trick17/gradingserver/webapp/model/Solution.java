package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.GradingResult;
import ch.trick17.gradingserver.util.RandomHexStringGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.*;
import static java.util.Objects.requireNonNull;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.CascadeType.PERSIST;

@Entity
public class Solution implements Serializable {

    @Id
    @GeneratedValue
    private int id;
    @ManyToOne(cascade = PERSIST)
    private ProblemSet problemSet;
    private String repoUrl;
    @ManyToMany(cascade = PERSIST)
    private final Set<Author> authors = new HashSet<>();
    private String accessToken;
    private String ignoredInitialCommit;

    @OneToMany(mappedBy = "solution", cascade = ALL, orphanRemoval = true)
    private final List<Submission> submissions = new ArrayList<>();
    private boolean fetchingSubmission = false;

    protected Solution() {}

    public Solution(ProblemSet problemSet, String repoUrl, Collection<Author> authors,
                    String ignoredInitialCommit) {
        this.problemSet = requireNonNull(problemSet);
        this.repoUrl = requireNonNull(repoUrl);
        this.authors.addAll(authors);
        this.ignoredInitialCommit = ignoredInitialCommit;
        generateAccessToken();
        problemSet.getSolutions().add(this);
    }

    public int getId() {
        return id;
    }

    public ProblemSet getProblemSet() {
        return problemSet;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void generateAccessToken() {
        this.accessToken = new RandomHexStringGenerator(32)
                .generate(i -> false);
    }

    public String getIgnoredInitialCommit() {
        return ignoredInitialCommit;
    }

    public List<Submission> getSubmissions() {
        return submissions;
    }

    public Submission latestSubmission() {
        return submissions.stream()
                .max(comparingInt(Submission::getId)).orElse(null);
    }

    public boolean isFetchingSubmission() {
        return fetchingSubmission;
    }

    public void setFetchingSubmission(boolean fetchingSubmission) {
        this.fetchingSubmission = fetchingSubmission;
    }

    public static Comparator<Solution> byResult() {
        // totally unreadable, but the following sorts first by number of passed
        // tests and then by received date (earlier -> greater)
        return comparing(Solution::latestSubmission, nullsFirst(
                comparing(Submission::getResult, nullsFirst(
                        comparing(GradingResult::getPassedTests, nullsFirst(
                                comparing(List::size)))))
                        .thenComparing(Submission::getReceivedDate, reverseOrder())));
    }

    public static Comparator<Solution> byCommitHash() {
        return comparing(Solution::latestSubmission, nullsFirst(
                comparing(Submission::getCommitHash)));
    }
}
