package ch.trick17.gradingserver.gradingservice.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Embeddable
public class Credentials {

    @Column
    private String host;
    @Column
    private String username;
    @Column
    private String password;

    protected Credentials() {}

    public Credentials(String host, String username, String password) {
        this.host = requireNonNull(host);
        this.username = requireNonNull(username);
        this.password = requireNonNull(password);
    }

    public String host() {
        return host;
    }

    public String username() {
        return username;
    }

    public String password() {
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
