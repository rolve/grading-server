package ch.trick17.gradingserver.webapp.model;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface AuthorRepository extends PagingAndSortingRepository<Author, Integer> {
    Optional<Author> findByName(String name);
}
