package com.sras.backend.config;

import com.sras.backend.enums.Role;
import com.sras.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        seedUserIfMissing(503092, "admin", "Admin User", "1234", Role.ADMIN);
        seedUserIfMissing(503069, "manager", "Manager User", "1234", Role.MANAGER);
        seedUserIfMissing(503080, "staff", "Staff User", "1234", Role.STAFF);
        seedUserIfMissing(503107, "owner", "Owner User", "1234", Role.OWNER);
    }

    private void seedUserIfMissing(Integer userId, String username, String fullName, String rawPassword, Role role) {
        if (!userRepository.existsById(userId) && !userRepository.existsByUsername(username)) {
            jdbcTemplate.update(
                "INSERT INTO users (user_id, username, full_name, password_hash, role, is_active) VALUES (?, ?, ?, ?, ?, ?)",
                userId, username, fullName, passwordEncoder.encode(rawPassword), role.name(), true
            );
        }
    }
}


