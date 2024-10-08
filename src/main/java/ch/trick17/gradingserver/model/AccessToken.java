package ch.trick17.gradingserver.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static java.util.Objects.requireNonNull;

@Entity
public class AccessToken {

    @Id
    @GeneratedValue
    private int id;
    @ManyToOne
    private User owner;
    private String host;
    private String token;

    protected AccessToken() {}

    public AccessToken(User owner, String host, String token) {
        this.owner = requireNonNull(owner);
        this.host = requireNonNull(host);
        this.token = requireNonNull(token);
    }

    public int getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getHost() {
        return host;
    }

    public String getToken() {
        return token;
    }
}
