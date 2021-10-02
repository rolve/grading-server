package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Comparator.comparingInt;
import static java.util.regex.Pattern.compile;

public interface HostAccessTokenRepository extends JpaRepository<HostAccessToken, Integer> {

    Pattern HOST_PATTERN = compile("https?://([^/]+)/.*");

    List<HostAccessToken> findByHost(String host);

    default Optional<String> findLatestForUrl(String url) {
        var matcher = HOST_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new AssertionError("invalid repo url");
        }
        return findByHost(matcher.group(1)).stream()
                .max(comparingInt(HostAccessToken::getId))
                .map(HostAccessToken::getAccessToken);
    }
}
