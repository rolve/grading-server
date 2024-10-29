package ch.trick17.gradingserver.model;

import ch.trick17.gradingserver.util.StringListConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JoinFormula;

import java.util.*;

import static jakarta.persistence.CascadeType.*;
import static java.util.Comparator.*;
import static java.util.Objects.requireNonNull;

@Entity
public class Solution {

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

    @ManyToOne
    @JoinFormula("""
            (SELECT s.id
            FROM submission s
            WHERE s.solution_id = id
            ORDER BY s.received_time DESC
            LIMIT 1)""")
    private Submission latestSubmission;

    protected Solution() {
    }

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
        return latestSubmission;
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof Solution && id == ((Solution) o).id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public static Comparator<Solution> byResult() {
        return comparing(Solution::latestSubmission, nullsLast(
                    comparing(Submission::getStatus)
                    .thenComparing(Submission::getResult, nullsLast(GradingResult.betterFirst()))
                    .thenComparing(Submission::getReceivedTime)))
                .thenComparing(Solution::getAuthors,
                    comparing(authors -> authors.stream()
                            .map(a -> a.getDisplayName().toLowerCase())
                            .sorted()
                            .findFirst().orElse(null),
                        nullsLast(naturalOrder())));
    }

    public static Comparator<Solution> byCommitHash() {
        return comparing(Solution::latestSubmission, nullsLast(
                comparing(Submission::getCommitHash)));
    }
}
