package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.AccessTokenRepository;
import ch.trick17.gradingserver.webapp.model.Role;
import ch.trick17.gradingserver.webapp.model.User;
import ch.trick17.gradingserver.webapp.model.UserRepository;
import ch.trick17.gradingserver.webapp.service.PasswordService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;

import static ch.trick17.gradingserver.webapp.model.Role.ADMIN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Controller
@RequestMapping("/admin")
public class AdminPanelController {

    private final UserRepository userRepo;
    private final AccessTokenRepository tokenRepo;
    private final PasswordService passwordService;

    public AdminPanelController(UserRepository userRepo, AccessTokenRepository tokenRepo,
                                PasswordService passwordService) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.passwordService = passwordService;
    }

    @GetMapping("/")
    public String adminPanel(Model model) {
        var users = userRepo.findAll();
        var tokenCounts = users.stream()
                .collect(toMap(identity(), tokenRepo::countByOwner));
        var undeletable = new HashSet<User>();
        if (userRepo.countByRolesContaining(ADMIN) < 2) {
            users.stream()
                    .filter(user -> user.getRoles().contains(ADMIN))
                    .forEach(undeletable::add);
        }
        model.addAttribute("users", users);
        model.addAttribute("tokenCounts", tokenCounts);
        model.addAttribute("undeletable", undeletable);
        return "admin/admin";
    }

    @GetMapping("/create-user")
    public String addUser(Model model) {
        model.addAttribute("possibleRoles", asList(Role.values()));
        return "admin/create-user";
    }

    @PostMapping("/create-user")
    public String addUser(@RequestParam String username,
                          @RequestParam String displayName,
                          @RequestParam(required = false) Set<Role> roles,
                          Model model) {
        if (userRepo.existsByUsername(username)) {
            model.addAttribute("possibleRoles", asList(Role.values()));
            model.addAttribute("username", username);
            model.addAttribute("roles", roles);
            model.addAttribute("error", "Username already taken");
            return "admin/create-user";
        }
        var password = passwordService.generateSecurePassword();
        userRepo.save(new User(username, passwordService.encode(password),
                displayName, roles == null ? emptySet() : roles));
        model.addAttribute("username", username);
        model.addAttribute("password", password);
        return "admin/user-created";
    }

    @PostMapping("/delete-user")
    @Transactional
    public String delete(@RequestParam int userId) {
        var user = userRepo.findById(userId).orElseThrow();
        if (user.getRoles().contains(ADMIN)
                && userRepo.countByRolesContaining(ADMIN) < 2) {
            throw new RuntimeException("cannot delete last ADMIN");
        }
        tokenRepo.deleteByOwner(user);
        userRepo.delete(user);
        return "redirect:.";
    }
}
