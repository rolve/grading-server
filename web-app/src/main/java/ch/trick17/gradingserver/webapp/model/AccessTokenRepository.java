package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessTokenRepository extends JpaRepository<AccessToken, Integer> {
    int countByOwner(User owner);
    List<AccessToken> findByOwnerAndHost(User owner, String host);
}
