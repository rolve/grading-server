package ch.trick17.gradingserver.gradingservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Entity
public class Credentials {

    @Id
    @GeneratedValue
    private int id;
    @Column
    private String host;
    @Column
    private String username;
    @Column
    private String password;

    protected Credentials() {}

    @JsonCreator
    public Credentials(String host, String username, String password) {
        this.host = requireNonNull(host);
        this.username = requireNonNull(username);
        this.password = requireNonNull(password);
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Credentials) obj;
        return Objects.equals(this.host, that.host) &&
                Objects.equals(this.username, that.username) &&
                Objects.equals(this.password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, username, password);
    }

    @Override
    public String toString() {
        return "Credentials[" +
                "host=" + host + ", " +
                "username=" + username + "]";
    }
}
