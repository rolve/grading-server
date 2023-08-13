package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.model.GradingResult;
import ch.trick17.gradingserver.util.StringListConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.*;
import static java.util.Objects.requireNonNull;
import static javax.persistence.CascadeType.*;

@Entity
public class Solution implements Serializable {

    @Id
    @GeneratedValue
    private int id;
    @ManyToOne(cascade = {PERSIST, MERGE})
    private ProblemSet problemSet;
    private String repoUrl;
    @ManyToOne(cascade = {PERSIST, MERGE})
    private AccessToken accessToken;
    @ManyToMany(cascade = {PERSIST, MERGE})
    private final Set<Author> authors = new HashSet<>();
    @Lob
    @Convert(converter = StringListConverter.class)
    private final List<String> ignoredPushers = new ArrayList<>();

    @OneToMany(mappedBy = "solution", cascade = ALL, orphanRemoval = true)
    private final List<Submission> submissions = new ArrayList<>();

    protected Solution() {}

    public Solution(ProblemSet problemSet, String repoUrl, AccessToken accessToken,
                    Collection<Author> authors, Collection<String> ignoredPushers) {
        this.problemSet = requireNonNull(problemSet);
        this.repoUrl = requireNonNull(repoUrl);
        this.accessToken = accessToken;
        this.authors.addAll(authors);
        this.ignoredPushers.addAll(ignoredPushers);
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

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public List<String> getIgnoredPushers() {
        return ignoredPushers;
    }

    public List<Submission> getSubmissions() {
        return submissions;
    }

    public Submission latestSubmission() {
        return submissions.stream()
                .max(comparingInt(Submission::getId)).orElse(null);
    }

    public GradingResult latestResult() {
        return submissions.stream()
                .filter(Submission::hasResult)
                .max(comparingInt(Submission::getId))
                .map(Submission::getResult)
                .orElse(null);
    }

    public static Comparator<Solution> byResult() {
        // totally unreadable, but the following sorts first by number of passed
        // tests (higher -> "less") and then by received date (earlier -> "less")
        return comparing(Solution::latestSubmission, nullsLast(
                comparing(Submission::getResult, nullsLast(
                        comparing(GradingResult::getPassedTests, nullsLast(
                                reverseOrder(comparingInt(List::size))))))
                        .thenComparing(Submission::getReceivedDate)));
    }

    public static Comparator<Solution> byCommitHash() {
        return comparing(Solution::latestSubmission, nullsLast(
                comparing(Submission::getCommitHash)));
    }
}
