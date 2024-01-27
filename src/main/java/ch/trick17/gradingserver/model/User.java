package ch.trick17.gradingserver.model;

import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.Collection;
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

    private String displayName;

    @ElementCollection(fetch = EAGER)
    private final Set<Role> roles = new HashSet<>();

    protected User() {}

    public User(String username, String password, String displayName, Role... roles) {
        this(username, password, displayName, asList(roles));
    }

    public User(String username, String password, String displayName,
                Collection<Role> roles) {
        this.username = requireNonNull(username);
        this.password = requireNonNull(password);
        this.displayName = requireNonNull(displayName);
        this.roles.addAll(roles);
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

    public String getDisplayName() {
        return displayName;
    }

    public Set<Role> getRoles() {
        return roles;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof User)) {
            return false;
        }
        return id == ((User) o).id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
