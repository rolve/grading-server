package ch.trick17.gradingserver.webapp.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static java.util.Objects.requireNonNull;

@Entity
public class HostAccessToken {

    @Id
    @GeneratedValue
    private int id;
    private String host;
    private String accessToken;

    protected HostAccessToken() {}

    public HostAccessToken(String host, String accessToken) {
        this.host = requireNonNull(host);
        this.accessToken = requireNonNull(accessToken);
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
