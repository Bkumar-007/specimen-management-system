package com.specimen.sms.config;

import com.specimen.sms.domain.Role;
import com.specimen.sms.domain.UserAccount;
import com.specimen.sms.domain.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUser("admin", "admin123", Role.LAB_ADMIN);
        seedUser("tech", "tech123", Role.LAB_TECH);
    }

    private void seedUser(String username, String rawPassword, Role role) {
        if (users.existsByUsername(username)) {
            return;
        }
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        users.save(user);
        log.info("Seeded user '{}' with role {}", username, role);
    }
}
