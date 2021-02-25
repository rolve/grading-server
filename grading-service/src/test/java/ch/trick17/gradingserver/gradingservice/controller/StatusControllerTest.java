package ch.trick17.gradingserver.gradingservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class StatusControllerTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void status() {
        var response = rest.getForEntity("/api/v1/status", String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }
}
