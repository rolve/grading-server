package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.model.User;
import ch.trick17.gradingserver.webapp.model.UserRepository;
import org.slf4j.Logger;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;

import static ch.trick17.gradingserver.webapp.model.Role.ADMIN;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder;

@Service
public class AdminUserCreator {

    private static final Logger logger = getLogger(AdminUserCreator.class);

    private final UserRepository userRepo;

    public AdminUserCreator(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent ignored) {
        createInitialUser();
    }

    public void createInitialUser() {
        if (userRepo.findByUsername("admin").isEmpty()) {
            var encoder = createDelegatingPasswordEncoder();
            var random = new SecureRandom();
            var password = new BigInteger(128, random).toString(32);
            var admin = new User("admin", encoder.encode(password), ADMIN);
            userRepo.save(admin);
            logger.info("Created user 'admin' with password '{}'", password);
        }
    }
}
