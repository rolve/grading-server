package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.util.RandomHexStringGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static java.util.Objects.requireNonNull;

@Entity
public class Author {

    @Id
    @GeneratedValue
    private int id;
    private String name;
    private String accessToken;

    protected Author() {}

    public Author(String name) {
        this.name = requireNonNull(name);
        generateAccessToken();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireNonNull(name);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void generateAccessToken() {
        this.accessToken = new RandomHexStringGenerator(32)
                .generate(i -> false);
    }
}
