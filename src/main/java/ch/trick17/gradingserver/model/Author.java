package ch.trick17.gradingserver.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Locale;
import java.util.Objects;

@Entity
public class Author {

    @Id
    @GeneratedValue
    private int id;
    @Column(unique = true)
    private String username;

    protected Author() {}

    public Author(String username) {
        setUsername(username);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException();
        }
        this.username = username;
    }

    public String getDisplayName() {
        // for now
        return username;
    }

    public String getShortenedDisplayName() {
        // for now
        var parts = username.split("\\.");
        if (parts.length == 1) {
            return parts[0];
        } else {
            return parts[0].toUpperCase().charAt(0) + parts[0].substring(1)
                   + " " + parts[1].toUpperCase().charAt(0) + ".";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(username, ((Author) o).username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
