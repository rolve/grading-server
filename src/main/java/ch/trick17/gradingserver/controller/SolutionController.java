package ch.trick17.gradingserver.controller;

import ch.trick17.gradingserver.model.SolutionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/courses/*/problem-sets/*/solutions")
public class SolutionController {

    private final SolutionRepository repo;

    public SolutionController(SolutionRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id) {
        repo.deleteById(id);
        return "redirect:../../";
    }
}
