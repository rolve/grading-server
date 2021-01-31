package ch.trick17.gradingserver.gradingservice;

import ch.trick17.gradingserver.gradingservice.model.Credentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class GradingServiceApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void contextLoads() {}

    @Test
    void createAndRetrieveCredentials() {
        var response = rest.exchange(host() + "/api/v1/credentials", GET, null,
                new ParameterizedTypeReference<List<Credentials>>() {});
        assertEquals(emptyList(), response.getBody());

        var credentials = new Credentials("localhost", "user", "secret");
        rest.postForObject(host() + "/api/v1/credentials", credentials, String.class);

        response = rest.exchange(host() + "/api/v1/credentials", GET, null,
                new ParameterizedTypeReference<List<Credentials>>() {});
        assertEquals(List.of(credentials), response.getBody());
    }

    @Test
    void incompleteCredentials() {
        var credentials = new Credentials() {};
        var response = rest.postForEntity(host() + "/api/v1/credentials", credentials, String.class);
        assertEquals(BAD_REQUEST, response.getStatusCode());
    }

    private String host() {
        return "http://localhost:" + port;
    }
}
