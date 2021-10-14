package ch.trick17.gradingserver.webapp.model;

import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

import static java.util.Arrays.asList;

public enum Role implements GrantedAuthority {
    LECTURER,
    ADMIN(LECTURER);

    public Set<Role> includedRoles;

    Role(Role... includedRoles) {
        this.includedRoles = Set.copyOf(asList(includedRoles));
    }

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }

    public static String hierarchyString() {
        var result = new StringBuilder();
        for (var role : values()) {
            for (var included : role.includedRoles) {
                result.append(role.getAuthority()).append(" > ").append(included.getAuthority()).append("\n");
            }
        }
        return result.toString();
    }
}
