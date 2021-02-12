package ch.trick17.gradingserver.gradingservice.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CredentialsRepository extends CrudRepository<Credentials, Integer> {
    List<Credentials> findByHost(String host);
}
