package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPanelController {

    private final UserRepository userRepo;

    public AdminPanelController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/")
    public String adminPanel(Model model) {
        var users = userRepo.findAll();
        model.addAttribute("users", users);
        return "admin";
    }
}
