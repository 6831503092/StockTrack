package com.sras.backend.config;

import com.sras.backend.entity.AuditLog;
import com.sras.backend.entity.Customer;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.Supplier;
import com.sras.backend.entity.User;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.Role;
import com.sras.backend.repository.AuditLogRepository;
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
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogRepository auditLogRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    private record ProductSeed(int productId, String name, double cost, double price, int stock, Zone zone) {}

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

        // Fixed initial products with deterministic IDs and varied stock (<= 100)
        List<ProductSeed> seeds = List.of(
                // 1. Tables (Zone Z1)
                new ProductSeed(10001, "Office Desk", 1000.0, 1500.0, 45, z1),
                new ProductSeed(10002, "Dining Table", 2000.0, 3000.0, 18, z1),
                new ProductSeed(10003, "Coffee Table", 500.0, 1000.0, 85, z1),
                new ProductSeed(10004, "Computer Desk", 1500.0, 2500.0, 60, z1),
                new ProductSeed(10005, "Meeting Table", 4000.0, 6000.0, 12, z1),
                // 2. Chairs (Zone Z2)
                new ProductSeed(10006, "Office Chair", 1000.0, 1500.0, 95, z2),
                new ProductSeed(10007, "Dining Chair", 500.0, 1000.0, 100, z2),
                new ProductSeed(10008, "Gaming Chair", 2000.0, 3000.0, 32, z2),
                new ProductSeed(10009, "Folding Chair", 300.0, 500.0, 78, z2),
                new ProductSeed(10010, "Sofa Chair", 3000.0, 5000.0, 15, z2),
                // 3. Beds (Zone Z3)
                new ProductSeed(10011, "Single Bed", 2000.0, 3000.0, 55, z3),
                new ProductSeed(10012, "Queen Bed", 3000.0, 5000.0, 40, z3),
                new ProductSeed(10013, "King Bed", 4000.0, 6000.0, 22, z3),
                new ProductSeed(10014, "Bunk Bed", 3000.0, 5000.0, 8, z3),
                new ProductSeed(10015, "Folding Bed", 1000.0, 1500.0, 68, z3),
                // 4. Cabinets (Zone Z4)
                new ProductSeed(10016, "Wardrobe", 3000.0, 5000.0, 25, z4),
                new ProductSeed(10017, "Shoe Cabinet", 1000.0, 1500.0, 50, z4),
                new ProductSeed(10018, "Bookshelf", 1500.0, 2500.0, 90, z4),
                new ProductSeed(10019, "TV Cabinet", 2000.0, 3000.0, 14, z4),
                new ProductSeed(10020, "Drawer Cabinet", 1000.0, 2000.0, 70, z4)
        );

        for (ProductSeed seed : seeds) {
            createProduct(seed.productId(), seed.name(), seed.cost(), seed.price(), seed.stock(), seed.zone());
        }
    }

    private void createProduct(int productId, String name, double cost, double price, int stock, Zone zone) {
        if (productRepository.existsById(productId) || productRepository.existsByProductName(name)) {
            return;
        }

        jdbcTemplate.update(
            "INSERT INTO products (product_id, product_name, default_cost, selling_price, current_stock, min_stock_alert, po_min_qty, po_max_qty, so_min_qty, so_max_qty, zone_id, is_active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            productId, name, BigDecimal.valueOf(cost), BigDecimal.valueOf(price),
            stock, 20, 1, 100, 1, 50, zone.getZoneId(), true
        );
    }

    private void seedAuditLogsIfEmpty() {
        if (auditLogRepository.count() > 0) {
            return;
        }

        User admin = userRepository.findById(503092).orElse(null);
        User manager = userRepository.findById(503069).orElse(null);
        User staff = userRepository.findById(503080).orElse(null);

        if (admin == null || manager == null || staff == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<AuditLog> initialLogs = List.of(
                AuditLog.builder().user(staff).role(Role.STAFF).actionType(com.sras.backend.enums.AuditAction.RECEIVE_ACCEPT.name()).targetType("PO").targetId("1001").detail("Accepted Receive for PO #1001 into warehouse (45 Office Desk to Zone Z1)").actionDatetime(now.minusHours(2)).build(),
                AuditLog.builder().user(staff).role(Role.STAFF).actionType(com.sras.backend.enums.AuditAction.STOCK_OUT_CONFIRM.name()).targetType("SO").targetId("2001").detail("Confirmed Stock Out for SO #2001 (15 Gaming Chair shipped)").actionDatetime(now.minusHours(4)).build(),
                AuditLog.builder().user(manager).role(Role.MANAGER).actionType(com.sras.backend.enums.AuditAction.REQUEST_APPROVE.name()).targetType("PO").targetId("102").detail("Approved PO_CREATE Request #102 for Supplier Thai Wood Supply Co.").actionDatetime(now.minusHours(7)).build(),
                AuditLog.builder().user(admin).role(Role.ADMIN).actionType(com.sras.backend.enums.AuditAction.SO_CREATE.name()).targetType("SO").targetId("2002").detail("Created Sales Order #2002 (Customer: RWTcomp., 3 items)").actionDatetime(now.minusHours(11)).build(),
                AuditLog.builder().user(staff).role(Role.STAFF).actionType(com.sras.backend.enums.AuditAction.TRANSFER_VERIFY.name()).targetType("MOVEMENT").targetId("3001").detail("Verified transfer for movement #3001 (20 Coffee Table from Z1 to Z2)").actionDatetime(now.minusDays(1).minusHours(1)).build(),
                AuditLog.builder().user(admin).role(Role.ADMIN).actionType(com.sras.backend.enums.AuditAction.PO_CREATE.name()).targetType("PO").targetId("1001").detail("Created Purchase Order #1001 (Supplier: Thai Wood Supply Co.)").actionDatetime(now.minusDays(1).minusHours(4)).build(),
                AuditLog.builder().user(manager).role(Role.MANAGER).actionType(com.sras.backend.enums.AuditAction.PRODUCT_UPDATE.name()).targetType("PRODUCT").targetId("10008").detail("Updated product ID 10008: Gaming Chair (Selling Price updated to 3000.00)").actionDatetime(now.minusDays(1).minusHours(8)).build(),
                AuditLog.builder().user(staff).role(Role.STAFF).actionType(com.sras.backend.enums.AuditAction.RECEIVE_ACCEPT.name()).targetType("PO").targetId("998").detail("Accepted Receive for PO #998 into warehouse (30 Bookshelf to Zone Z4)").actionDatetime(now.minusDays(2)).build(),
                AuditLog.builder().user(staff).role(Role.STAFF).actionType(com.sras.backend.enums.AuditAction.STOCK_OUT_CONFIRM.name()).targetType("SO").targetId("1995").detail("Confirmed Stock Out for SO #1995 (10 Dining Table shipped)").actionDatetime(now.minusDays(2).minusHours(3)).build(),
                AuditLog.builder().user(staff).role(Role.STAFF).actionType(com.sras.backend.enums.AuditAction.STOCK_OUT_REJECT.name()).targetType("SO").targetId("1994").detail("Rejected Stock Out for SO #1994. Reason: Damaged packaging on inspection").actionDatetime(now.minusDays(3)).build()
        );

        auditLogRepository.saveAll(initialLogs);
    }
}
