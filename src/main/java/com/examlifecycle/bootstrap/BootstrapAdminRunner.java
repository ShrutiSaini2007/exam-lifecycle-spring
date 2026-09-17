package com.examlifecycle.bootstrap;

import com.examlifecycle.domain.Role;
import com.examlifecycle.domain.User;
import com.examlifecycle.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mirrors the Python prototype's ensure_bootstrap_admin(): if there are no
 * users at all, create exactly one ADMIN account so the system is never
 * permanently locked out on a fresh database. This is what makes "no
 * public sign-up" workable — there's always exactly one door in.
 */
@Component
public class BootstrapAdminRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredPassword;

    public BootstrapAdminRunner(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 @Value("${app.bootstrap-admin-password:}") String configuredPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        String password = (configuredPassword != null && !configuredPassword.isBlank())
                ? configuredPassword
                : randomPassword();

        User admin = new User("admin", passwordEncoder.encode(password), Role.ADMIN);
        userRepository.save(admin);

        System.out.println("=".repeat(64));
        System.out.println("First run: created a bootstrap ADMIN account.");
        System.out.println("  username: admin");
        System.out.println("  password: " + password);
        System.out.println("Save this now — it will not be shown again. Sign in, then");
        System.out.println("use the admin panel to create Paper Setter / Moderator /");
        System.out.println("Exam Officer accounts with their own credentials.");
        System.out.println("=".repeat(64));
    }

    private String randomPassword() {
        byte[] bytes = new byte[9];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
