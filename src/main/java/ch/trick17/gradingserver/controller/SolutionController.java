package ch.trick17.gradingserver.controller;

import ch.trick17.gradingserver.service.SolutionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/courses/*/problem-sets/*/solutions")
public class SolutionController {

    private final SolutionService service;

    public SolutionController(SolutionService service) {
        this.service = service;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id) {
        var solution = service.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        service.delete(solution);
        return "redirect:../../";
    }
}
