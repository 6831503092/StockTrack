package com.sras.backend.config;

import com.sras.backend.entity.Product;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.Role;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.UserRepository;
import com.sras.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUserIfMissing(503092, "admin", "1234", Role.ADMIN);
        seedUserIfMissing(503069, "manager", "1234", Role.MANAGER);
        seedUserIfMissing(503080, "staff", "1234", Role.STAFF);
        seedUserIfMissing(503107, "owner", "1234", Role.OWNER);

        seedZoneIfMissing("Z1", "Zone 1");
        seedZoneIfMissing("Z2", "Zone 2");
        seedZoneIfMissing("Z3", "Zone 3");
        seedZoneIfMissing("Z4", "Zone 4");

        seedProductsIfEmpty();
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

    private void seedProductsIfEmpty() {
        if (productRepository.count() > 0) {
            return;
        }

        Zone z1 = zoneRepository.findByZoneCode("Z1").orElse(null);
        Zone z2 = zoneRepository.findByZoneCode("Z2").orElse(null);
        Zone z3 = zoneRepository.findByZoneCode("Z3").orElse(null);
        Zone z4 = zoneRepository.findByZoneCode("Z4").orElse(null);

        if (z1 == null || z2 == null || z3 == null || z4 == null) {
            return;
        }

        Random random = new Random();
        Set<Integer> usedIds = new HashSet<>();

        // 1. Tables (Zone Z1)
        createProduct("Office Desk", 1000.0, 1500.0, z1, random, usedIds);
        createProduct("Dining Table", 2000.0, 3000.0, z1, random, usedIds);
        createProduct("Coffee Table", 500.0, 1000.0, z1, random, usedIds);
        createProduct("Computer Desk", 1500.0, 2500.0, z1, random, usedIds);
        createProduct("Meeting Table", 4000.0, 6000.0, z1, random, usedIds);

        // 2. Chairs (Zone Z2)
        createProduct("Office Chair", 1000.0, 1500.0, z2, random, usedIds);
        createProduct("Dining Chair", 500.0, 1000.0, z2, random, usedIds);
        createProduct("Gaming Chair", 2000.0, 3000.0, z2, random, usedIds);
        createProduct("Folding Chair", 300.0, 500.0, z2, random, usedIds);
        createProduct("Sofa Chair", 3000.0, 5000.0, z2, random, usedIds);

        // 3. Beds (Zone Z3)
        createProduct("Single Bed", 2000.0, 3000.0, z3, random, usedIds);
        createProduct("Queen Bed", 3000.0, 5000.0, z3, random, usedIds);
        createProduct("King Bed", 4000.0, 6000.0, z3, random, usedIds);
        createProduct("Bunk Bed", 3000.0, 5000.0, z3, random, usedIds);
        createProduct("Folding Bed", 1000.0, 1500.0, z3, random, usedIds);

        // 4. Cabinets (Zone Z4)
        createProduct("Wardrobe", 3000.0, 5000.0, z4, random, usedIds);
        createProduct("Shoe Cabinet", 1000.0, 1500.0, z4, random, usedIds);
        createProduct("Bookshelf", 1500.0, 2500.0, z4, random, usedIds);
        createProduct("TV Cabinet", 2000.0, 3000.0, z4, random, usedIds);
        createProduct("Drawer Cabinet", 1000.0, 2000.0, z4, random, usedIds);
    }

    private void createProduct(String name, double cost, double price, Zone zone, Random random, Set<Integer> usedIds) {
        if (productRepository.existsByProductName(name)) {
            return;
        }

        // Random 5-digit Product ID (10000 - 99999)
        int productId;
        do {
            productId = 10000 + random.nextInt(90000);
        } while (usedIds.contains(productId) || productRepository.existsById(productId));
        usedIds.add(productId);

        // Random stock between 5 and 100 (values < 20 will trigger low stock alert)
        int stock = random.nextInt(96) + 5;

        jdbcTemplate.update(
            "INSERT INTO products (product_id, product_name, default_cost, selling_price, current_stock, min_stock_alert, po_min_qty, po_max_qty, so_min_qty, so_max_qty, zone_id, is_active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            productId, name, BigDecimal.valueOf(cost), BigDecimal.valueOf(price),
            stock, 20, 1, 100, 1, 50, zone.getZoneId(), true
        );
    }
}
