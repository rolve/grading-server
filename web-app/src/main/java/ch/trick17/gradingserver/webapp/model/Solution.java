package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.RandomHexStringGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import static java.util.Objects.requireNonNull;

@Entity
public class Solution {

    @Id
    @ManyToOne
    private ProblemSet problemSet;
    @Id
    @ManyToOne
    private Author author;
    @Column
    private String accessToken;

    protected Solution() {}

    public Solution(ProblemSet problemSet, Author author) {
        this.problemSet = requireNonNull(problemSet);
        this.author = requireNonNull(author);
        generateAccessToken();
    }

    public ProblemSet getProblemSet() {
        return problemSet;
    }

    public Author getAuthor() {
        return author;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void generateAccessToken() {
        this.accessToken = new RandomHexStringGenerator(32)
                .generate(i -> false);
    }
}
