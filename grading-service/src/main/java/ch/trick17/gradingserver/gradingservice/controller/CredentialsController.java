package ch.trick17.gradingserver.gradingservice.controller;

import ch.trick17.gradingserver.gradingservice.model.Credentials;
import ch.trick17.gradingserver.gradingservice.model.CredentialsRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/v1/credentials")
public class CredentialsController {

    private final CredentialsRepository repo;

    public CredentialsController(CredentialsRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public Iterable<Credentials> list() {
        return repo.findAll();
    }

    @PostMapping
    public void create(@RequestBody Credentials credentials) {
        repo.save(credentials);
    }
}
