package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.AuthorRepository;
import ch.trick17.gradingserver.model.Solution;
import ch.trick17.gradingserver.model.SolutionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.List.copyOf;

@Service
public class SolutionService {

    private final SolutionRepository repo;
    private final AuthorRepository authorRepo;

    public SolutionService(SolutionRepository repo, AuthorRepository authorRepo) {
        this.repo = repo;
        this.authorRepo = authorRepo;
    }

    public Optional<Solution> findById(Integer id) {
        return repo.findById(id);
    }

    public void delete(Solution solution) {
        var authors = copyOf(solution.getAuthors());
        repo.delete(solution);

        for (var author : authors) {
            if (repo.countByAuthorsContains(author) == 0) {
                authorRepo.delete(author);
            }
        }
    }
}
