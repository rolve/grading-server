package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.model.User;
import ch.trick17.gradingserver.webapp.model.UserRepository;
import org.slf4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import static ch.trick17.gradingserver.webapp.model.Role.ADMIN;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder;

@Service
public class AdminUserCreator {

    private static final Logger logger = getLogger(AdminUserCreator.class);

    private final UserRepository userRepo;
    private final PasswordService passwordService;

    public AdminUserCreator(UserRepository userRepo, PasswordService passwordService) {
        this.userRepo = userRepo;
        this.passwordService = passwordService;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent ignored) {
        createInitialUser();
    }

    public void createInitialUser() {
        if (userRepo.findByUsername("admin").isEmpty()) {
            var password = passwordService.generateSecurePassword();
            var admin = new User("admin", passwordService.encode(password), ADMIN);
            userRepo.save(admin);
            logger.info("Created user 'admin' with password '{}'", password);
        }
    }
}
