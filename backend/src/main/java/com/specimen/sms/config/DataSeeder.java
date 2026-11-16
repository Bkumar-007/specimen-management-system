package com.specimen.sms.config;

import com.specimen.sms.domain.Client;
import com.specimen.sms.domain.ClientRepository;
import com.specimen.sms.domain.Role;
import com.specimen.sms.domain.TestCatalog;
import com.specimen.sms.domain.TestCatalogRepository;
import com.specimen.sms.domain.UserAccount;
import com.specimen.sms.domain.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds fictional demo users, clients and the test catalog (idempotent). */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserAccountRepository users;
    private final ClientRepository clients;
    private final TestCatalogRepository tests;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedUsers;

    public DataSeeder(
            UserAccountRepository users,
            ClientRepository clients,
            TestCatalogRepository tests,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.demo-users:true}") boolean seedUsers) {
        this.users = users;
        this.clients = clients;
        this.tests = tests;
        this.passwordEncoder = passwordEncoder;
        this.seedUsers = seedUsers;
    }

    @Override
    public void run(String... args) {
        if (seedUsers) {
            seedUser("admin", "admin123", Role.LAB_ADMIN);
            seedUser("tech", "tech123", Role.LAB_TECH);
        }
        seedClient("NRTH", "Northside Family Clinic");
        seedClient("RIVR", "Riverbend Urgent Care");
        seedClient("HGLN", "Highland Community Hospital");

        seedTest("CBC", "Complete Blood Count", "Lavender (EDTA)");
        seedTest("CMP", "Comprehensive Metabolic Panel", "Gold (SST)");
        seedTest("BMP", "Basic Metabolic Panel", "Gold (SST)");
        seedTest("TSH", "Thyroid Stimulating Hormone", "Gold (SST)");
        seedTest("LIPID", "Lipid Panel", "Gold (SST)");
        seedTest("HBA1C", "Hemoglobin A1c", "Lavender (EDTA)");
        seedTest("UA", "Urinalysis", "Urine cup");
        seedTest("PT-INR", "Prothrombin Time / INR", "Light blue (citrate)");
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

    private void seedClient(String code, String name) {
        if (clients.existsByCodeIgnoreCase(code)) {
            return;
        }
        Client c = new Client();
        c.setCode(code);
        c.setName(name);
        clients.save(c);
    }

    private void seedTest(String code, String name, String tubeType) {
        if (tests.existsByCodeIgnoreCase(code)) {
            return;
        }
        TestCatalog t = new TestCatalog();
        t.setCode(code);
        t.setName(name);
        t.setTubeType(tubeType);
        tests.save(t);
    }
}
