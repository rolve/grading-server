package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface HostCredentialsRepository extends CrudRepository<HostCredentials, Integer> {
    List<HostCredentials> findByHost(String host);
}
