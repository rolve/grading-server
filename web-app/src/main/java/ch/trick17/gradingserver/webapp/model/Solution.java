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

    protected Solution() {}

    public Solution(String repoUrl, Collection<Author> authors) {
        this.repoUrl = requireNonNull(repoUrl);
        this.authors.addAll(authors);
        generateAccessToken();
    }

    public int getId() {
        return id;
    }

    public ProblemSet getProblemSet() {
        return problemSet;
    }

    public void setProblemSet(ProblemSet problemSet) {
        this.problemSet = requireNonNull(problemSet);
        problemSet.getSolutions().add(this);
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
                .max(comparingInt(Submission::getNumber)).orElse(null);
    }
}
