package com.sras.backend.config;

import com.sras.backend.entity.Customer;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.Supplier;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.Role;
import com.sras.backend.repository.CustomerRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.SupplierRepository;
import com.sras.backend.repository.UserRepository;
import com.sras.backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    private record ProductSeed(String name, double cost, double price, Zone zone) {}

    @Override
    public void run(String... args) {
        seedUserIfMissing(503092, "admin", "1234", Role.ADMIN);
        seedUserIfMissing(503069, "manager", "1234", Role.MANAGER);
        seedUserIfMissing(503080, "staff", "1234", Role.STAFF);
        seedUserIfMissing(503107, "owner", "1234", Role.OWNER);

        for (int i = 1; i <= 4; i++) {
            seedZoneIfMissing("Z" + i, "Zone " + i);
        }

        seedProductsIfEmpty();
        seedSuppliersIfEmpty();
        seedCustomersIfEmpty();
    }

    private void seedCustomersIfEmpty() {
        List.of(
                "RWTcomp.",
                "Office Pro Thailand",
                "Bangkok Modern Living",
                "Siam Retail Group"
        ).forEach(this::seedCustomerIfMissing);
    }

    private void seedCustomerIfMissing(String customerName) {
        if (!customerRepository.existsByCustomerName(customerName)) {
            customerRepository.save(Customer.builder()
                    .customerName(customerName)
                    .isActive(true)
                    .build());
        }
    }

    private void seedSuppliersIfEmpty() {
        seedSupplier("JRScomp.", "Z1", "Tables");
        seedSupplier("Thai Wood Supply Co.", "Z2", "Chairs");
        seedSupplier("Modern Steel Ltd.", "Z3", "Beds");
        seedSupplier("Decor Fabric Asia", "Z4", "Cabinets");
    }

    private void seedSupplier(String companyName, String zoneCode, String category) {
        Supplier s = supplierRepository.findByCompanyName(companyName)
                .orElseGet(() -> Supplier.builder()
                        .companyName(companyName)
                        .isActive(true)
                        .build());
        s.setZoneCode(zoneCode);
        s.setCategory(category);
        supplierRepository.save(s);
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

        List<ProductSeed> seeds = List.of(
                // 1. Tables (Zone Z1)
                new ProductSeed("Office Desk", 1000.0, 1500.0, z1),
                new ProductSeed("Dining Table", 2000.0, 3000.0, z1),
                new ProductSeed("Coffee Table", 500.0, 1000.0, z1),
                new ProductSeed("Computer Desk", 1500.0, 2500.0, z1),
                new ProductSeed("Meeting Table", 4000.0, 6000.0, z1),
                // 2. Chairs (Zone Z2)
                new ProductSeed("Office Chair", 1000.0, 1500.0, z2),
                new ProductSeed("Dining Chair", 500.0, 1000.0, z2),
                new ProductSeed("Gaming Chair", 2000.0, 3000.0, z2),
                new ProductSeed("Folding Chair", 300.0, 500.0, z2),
                new ProductSeed("Sofa Chair", 3000.0, 5000.0, z2),
                // 3. Beds (Zone Z3)
                new ProductSeed("Single Bed", 2000.0, 3000.0, z3),
                new ProductSeed("Queen Bed", 3000.0, 5000.0, z3),
                new ProductSeed("King Bed", 4000.0, 6000.0, z3),
                new ProductSeed("Bunk Bed", 3000.0, 5000.0, z3),
                new ProductSeed("Folding Bed", 1000.0, 1500.0, z3),
                // 4. Cabinets (Zone Z4)
                new ProductSeed("Wardrobe", 3000.0, 5000.0, z4),
                new ProductSeed("Shoe Cabinet", 1000.0, 1500.0, z4),
                new ProductSeed("Bookshelf", 1500.0, 2500.0, z4),
                new ProductSeed("TV Cabinet", 2000.0, 3000.0, z4),
                new ProductSeed("Drawer Cabinet", 1000.0, 2000.0, z4)
        );

        for (ProductSeed seed : seeds) {
            createProduct(seed.name(), seed.cost(), seed.price(), seed.zone(), random, usedIds);
        }
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
