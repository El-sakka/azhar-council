package eg.azhar.archive.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * When ADMIN_PASSWORD is set (e.g. on a public deployment), replaces the default "Admin 123"
 * password from the seed migration at startup. Without it, the seeded password is kept.
 */
@Component
public class AdminPasswordInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminPasswordInitializer.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String password;

    public AdminPasswordInitializer(JdbcTemplate jdbc, PasswordEncoder encoder,
                                    @Value("${app.admin-password:}") String password) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            return;
        }
        String current = jdbc.queryForObject("SELECT password_hash FROM app_user WHERE username = 'Admin'", String.class);
        if (current == null || !encoder.matches(password, current)) {
            jdbc.update("UPDATE app_user SET password_hash = ? WHERE username = 'Admin'", encoder.encode(password));
            log.info("Admin password updated from ADMIN_PASSWORD");
        }
    }
}
