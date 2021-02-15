package ch.trick17.gradingserver;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.Embeddable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Embeddable
public class Credentials {

    private String username;
    private String password;

    protected Credentials() {}

    @JsonCreator
    public Credentials(String username, String password) {
        this.username = username == null || username.isBlank() ? "" : username;
        this.password = requireNonNull(password);
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
        return Objects.equals(this.username, that.username) &&
                Objects.equals(this.password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}
