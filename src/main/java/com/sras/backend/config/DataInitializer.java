package com.sras.backend.config;

import com.sras.backend.entity.Zone;
import com.sras.backend.enums.Role;
import com.sras.backend.repository.UserRepository;
import com.sras.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUserIfMissing(503092, "admin", "1234", Role.ADMIN);
        seedUserIfMissing(503069, "manager", "1234", Role.MANAGER);
        seedUserIfMissing(503080, "staff", "1234", Role.STAFF);
        seedUserIfMissing(503107, "owner", "1234", Role.OWNER);

        seedZoneIfMissing("Z1", "Zone 1 (Z1)");
        seedZoneIfMissing("Z2", "Zone 2 (Z2)");
        seedZoneIfMissing("Z3", "Zone 3 (Z3)");
        seedZoneIfMissing("Z4", "Zone 4 (Z4)");
    }

    private void seedUserIfMissing(Integer userId, String fullName, String rawPassword, Role role) {
        if (!userRepository.existsById(userId)) {
            jdbcTemplate.update(
                "INSERT INTO users (user_id, full_name, password_hash, role, is_active) VALUES (?, ?, ?, ?, ?)",
                userId, fullName, passwordEncoder.encode(rawPassword), role.name(), true
            );
        }
    }

    private void seedZoneIfMissing(String zoneCode, String zoneName) {
        if (!zoneRepository.existsByZoneCode(zoneCode)) {
            Zone zone = new Zone();
            zone.setZoneCode(zoneCode);
            zone.setZoneName(zoneName);
            zoneRepository.save(zone);
        }
    }
}
