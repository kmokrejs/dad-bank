package dev.dadbank.config;

import dev.dadbank.user.Role;
import dev.dadbank.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates the god-mode admin user on first start if it does not exist yet. */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserService userService;
    private final DadBankProperties props;

    public AdminSeeder(UserService userService, DadBankProperties props) {
        this.userService = userService;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        var admin = props.admin();
        if (userService.existsByUsernameOrEmail(admin.username(), admin.email())) {
            log.info("Admin user '{}' already exists, skipping seed", admin.username());
            return;
        }
        userService.register(admin.email(), admin.username(), admin.password(), Role.ADMIN);
        log.info("Seeded admin user '{}' ({})", admin.username(), admin.email());
    }
}
