package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.GradingResult;
import ch.trick17.gradingserver.util.RandomHexStringGenerator;
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
    @ManyToOne(cascade = PERSIST)
    private ProblemSet problemSet;
    private String repoUrl;
    @ManyToMany(cascade = {PERSIST, MERGE})
    private final Set<Author> authors = new HashSet<>();
    @Lob
    @Convert(converter = StringListConverter.class)
    private final List<String> ignoredPushers = new ArrayList<>();
    private String accessToken;

    @OneToMany(mappedBy = "solution", cascade = ALL, orphanRemoval = true)
    private final List<Submission> submissions = new ArrayList<>();

    protected Solution() {}

    public Solution(ProblemSet problemSet, String repoUrl, Collection<Author> authors,
                    Collection<String> ignoredPushers) {
        this.problemSet = requireNonNull(problemSet);
        this.repoUrl = requireNonNull(repoUrl);
        this.authors.addAll(authors);
        this.ignoredPushers.addAll(ignoredPushers);
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
