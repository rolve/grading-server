package ch.trick17.gradingserver.webapp.model;

import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.persistence.FetchType.EAGER;

@Entity
public class User implements UserDetails {

    @Id
    @GeneratedValue
    private int id;

    @Column(unique = true)
    private String username;
    private String password;

    @ElementCollection(fetch = EAGER)
    private Set<Role> roles = new HashSet<>();

    protected User() {}

    public User(String username, String password, Role... roles) {
        this.username = requireNonNull(username);
        this.password = requireNonNull(password);
        this.roles.addAll(asList(roles));
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Set<Role> getAuthorities() {
        return roles;
    }
}
