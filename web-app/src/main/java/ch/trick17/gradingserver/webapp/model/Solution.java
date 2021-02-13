package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.util.RandomHexStringGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.CascadeType.PERSIST;

@Entity
public class Solution implements Serializable {

    @Id
    @GeneratedValue
    private int id;
    @ManyToOne
    private ProblemSet problemSet;
    private String repoUrl;
    @ManyToMany(cascade = PERSIST)
    private Set<Author> authors = new HashSet<>();
    private String accessToken;

    @OneToMany(mappedBy = "solution", cascade = ALL, orphanRemoval = true)
    private List<Submission> submissions = new ArrayList<>();
    private boolean fetchingSubmission = false;

    protected Solution() {}

    public Solution(ProblemSet problemSet, String repoUrl, Collection<Author> authors) {
        this.problemSet = requireNonNull(problemSet);
        this.repoUrl = requireNonNull(repoUrl);
        this.authors.addAll(authors);
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
}
