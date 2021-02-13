package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.Credentials;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static java.util.Objects.requireNonNull;

@Entity
public class HostCredentials {

    @Id
    @GeneratedValue
    private int id;
    private String host;
    private Credentials credentials;

    protected HostCredentials() {}

    public HostCredentials(String host, Credentials credentials) {
        this.host = requireNonNull(host);
        this.credentials = requireNonNull(credentials);
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
