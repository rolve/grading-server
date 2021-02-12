package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.util.RandomHexStringGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Entity
public class Author {

    @Id
    @GeneratedValue
    private int id;
    @Column(unique = true)
    private String name;
    private String accessToken;

    protected Author() {}

    public Author(String name) {
        setName(name);
        generateAccessToken();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException();
        }
        this.name = name;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void generateAccessToken() {
        this.accessToken = new RandomHexStringGenerator(32)
                .generate(i -> false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(name, ((Author) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
