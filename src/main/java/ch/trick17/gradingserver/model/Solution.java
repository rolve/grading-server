package ch.trick17.gradingserver.model;

import ch.trick17.gradingserver.util.StringListConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

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
    private String branch;
    @ManyToOne(cascade = {PERSIST, MERGE})
    private AccessToken accessToken;
    @ManyToMany(cascade = {PERSIST, MERGE})
    private final Set<Author> authors = new HashSet<>();
    // TODO: Convert the following to JSON
    @Lob
    @Convert(converter = StringListConverter.class)
    private final List<String> ignoredPushers = new ArrayList<>();

    @OneToMany(mappedBy = "solution", cascade = ALL, orphanRemoval = true)
    private final List<Submission> submissions = new ArrayList<>();

    protected Solution() {}

    public Solution(ProblemSet problemSet, String repoUrl, String branch,
                    AccessToken accessToken, Collection<Author> authors,
                    Collection<String> ignoredPushers) {
        this.problemSet = requireNonNull(problemSet);
        this.repoUrl = requireNonNull(repoUrl);
        this.branch = requireNonNull(branch);
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

    public String getBranch() {
        return branch;
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
        return comparing(Solution::latestSubmission, nullsLast(
                    comparing(Submission::getStatus)
                    .thenComparing(Submission::getResult, nullsLast(GradingResult.betterFirst()))
                    .thenComparing(Submission::getReceivedDate)))
                .thenComparing(Solution::getAuthors,
                    comparing((Set<Author> authors) -> !authors.isEmpty())
                    .thenComparing(authors -> authors.iterator().next().getDisplayName()));
    }

    public static Comparator<Solution> byCommitHash() {
        return comparing(Solution::latestSubmission, nullsLast(
                comparing(Submission::getCommitHash)));
    }
}
