package ch.trick17.gradingserver.webapp.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;

import static org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder;

@Service
public class PasswordService {

    private final SecureRandom random = new SecureRandom();
    private final PasswordEncoder encoder = createDelegatingPasswordEncoder();

    public String generateSecurePassword() {
        return new BigInteger(128, random).toString(32);
    }

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}
