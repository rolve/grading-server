package ch.trick17.gradingserver.gradingservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Credentials extends ch.trick17.gradingserver.Credentials {

    @Id
    @GeneratedValue
    private int id;

    protected Credentials() {}

    @JsonCreator
    public Credentials(String host, String username, String password) {
        super(host, username, password);
    }

    public int getId() {
        return id;
    }
}
