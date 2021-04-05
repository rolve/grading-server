package ch.trick17.gradingserver.webapp.model;

import ch.trick17.gradingserver.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Comparator.comparingInt;
import static java.util.regex.Pattern.compile;

public interface HostCredentialsRepository extends JpaRepository<HostCredentials, Integer> {

    Pattern HOST_PATTERN = compile("https?://([^/]+)/.*");

    List<HostCredentials> findByHost(String host);

    default Optional<Credentials> findLatestForUrl(String url) {
        var matcher = HOST_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new AssertionError("invalid repo url");
        }
        return findByHost(matcher.group(1)).stream()
                .max(comparingInt(HostCredentials::getId))
                .map(HostCredentials::getCredentials);
    }
}
