package com.sras.backend.service;

import com.sras.backend.dto.OwnerActivityDto;
import com.sras.backend.dto.OwnerDashboardDto;
import com.sras.backend.dto.OwnerProductPerformanceDto;
import com.sras.backend.entity.AuditLog;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.SalesOrder;
import com.sras.backend.repository.AuditLogRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.SalesOrderItemRepository;
import com.sras.backend.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final ProductRepository productRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final AuditLogRepository auditLogRepository;

    private static final DateTimeFormatter DISPLAY_DT_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern REF_PATTERN = Pattern.compile("(?:PO|SO|Request|movement|product ID|customer|supplier|zone)\\s*#?([0-9]+|[A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);

    @Transactional(readOnly = true)
    public OwnerDashboardDto getDashboard() {
        // 1. Calculate real inventory value from actual database products
        List<Product> products = productRepository.findAll();
        BigDecimal calculatedInventory = BigDecimal.ZERO;
        for (Product p : products) {
            BigDecimal price = p.getSellingPrice() != null ? p.getSellingPrice() : BigDecimal.ZERO;
            int stock = p.getCurrentStock() != null ? p.getCurrentStock() : 0;
            calculatedInventory = calculatedInventory.add(price.multiply(BigDecimal.valueOf(stock)));
        }

        // 2. Aggregate sales orders (real DB orders + baseline mock)
        List<SalesOrder> salesOrders = salesOrderRepository.findAll();
        long realSoCount = salesOrders.size();
        BigDecimal realSoAmount = BigDecimal.ZERO;
        long realExportUnits = 0;

        for (SalesOrder so : salesOrders) {
            if (so.getTotalAmount() != null) {
                realSoAmount = realSoAmount.add(so.getTotalAmount());
            }
            realExportUnits += salesOrderItemRepository.findBySalesOrderSoId(so.getSoId()).stream()
                    .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .sum();
        }

        // Base mockup + real DB metrics
        BigDecimal baseRevenue = BigDecimal.valueOf(1485200);
        BigDecimal totalRevenue = baseRevenue.add(realSoAmount);

        long baseSales = 148;
        long totalSales = baseSales + realSoCount;

        long baseExport = 1890;
        long totalExport = baseExport + realExportUnits;

        // Realistic profit margin (~32.8%)
        BigDecimal totalProfit = totalRevenue.multiply(BigDecimal.valueOf(0.328)).setScale(0, RoundingMode.HALF_UP);

        // 3. Product Performance: Hot Sellers & Top Imports (aligned with seed catalog)
        List<OwnerProductPerformanceDto> hotSellers = List.of(
                OwnerProductPerformanceDto.builder().rank(1).productId(10008).productName("Gaming Chair").zoneName("Zone 2").quantity(86).amount("฿258,000").build(),
                OwnerProductPerformanceDto.builder().rank(2).productId(10001).productName("Office Desk").zoneName("Zone 1").quantity(64).amount("฿96,000").build(),
                OwnerProductPerformanceDto.builder().rank(3).productId(10004).productName("Computer Desk").zoneName("Zone 1").quantity(52).amount("฿130,000").build(),
                OwnerProductPerformanceDto.builder().rank(4).productId(10012).productName("Queen Bed").zoneName("Zone 3").quantity(38).amount("฿190,000").build(),
                OwnerProductPerformanceDto.builder().rank(5).productId(10018).productName("Bookshelf").zoneName("Zone 4").quantity(45).amount("฿112,500").build()
        );

        List<OwnerProductPerformanceDto> topImports = List.of(
                OwnerProductPerformanceDto.builder().rank(1).productId(10007).productName("Dining Chair").zoneName("Zone 2").quantity(120).amount("฿60,000").build(),
                OwnerProductPerformanceDto.builder().rank(2).productId(10003).productName("Coffee Table").zoneName("Zone 1").quantity(95).amount("฿47,500").build(),
                OwnerProductPerformanceDto.builder().rank(3).productId(10018).productName("Bookshelf").zoneName("Zone 4").quantity(90).amount("฿135,000").build(),
                OwnerProductPerformanceDto.builder().rank(4).productId(10009).productName("Folding Chair").zoneName("Zone 2").quantity(85).amount("฿25,500").build(),
                OwnerProductPerformanceDto.builder().rank(5).productId(10020).productName("Drawer Cabinet").zoneName("Zone 4").quantity(75).amount("฿75,000").build()
        );

        return OwnerDashboardDto.builder()
                .totalRevenue("฿" + String.format("%,d", totalRevenue.longValue()))
                .inventoryValue("฿" + String.format("%,d", calculatedInventory.longValue()))
                .totalSale(String.format("%,d", totalSales))
                .totalExport(String.format("%,d", totalExport))
                .totalProfit("฿" + String.format("%,d", totalProfit.longValue()))
                .hotSellers(hotSellers)
                .topImports(topImports)
                .build();
    }

    @Transactional(readOnly = true)
    public List<OwnerActivityDto> getActivities(String type, String search, String date) {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByActionDatetimeDesc();
        List<OwnerActivityDto> result = new ArrayList<>();

        for (AuditLog al : logs) {
            OwnerActivityDto dto = mapToActivityDto(al);

            // Filter by type if provided ("all", "sale", "import", "export")
            if (type != null && !type.isBlank() && !type.equalsIgnoreCase("all")) {
                if (!type.equalsIgnoreCase(dto.getType())) {
                    continue;
                }
            }

            // Filter by date if provided (YYYY-MM-DD)
            if (date != null && !date.isBlank()) {
                if (!date.equals(dto.getIsoDate())) {
                    continue;
                }
            }

            // Filter by search keyword
            if (search != null && !search.isBlank()) {
                String q = search.toLowerCase();
                boolean match = (dto.getActivityName() != null && dto.getActivityName().toLowerCase().contains(q)) ||
                                (dto.getDescription() != null && dto.getDescription().toLowerCase().contains(q)) ||
                                (dto.getReference() != null && dto.getReference().toLowerCase().contains(q)) ||
                                (dto.getActorName() != null && dto.getActorName().toLowerCase().contains(q));
                if (!match) {
                    continue;
                }
            }

            result.add(dto);
        }

        return result;
    }

    private OwnerActivityDto mapToActivityDto(AuditLog al) {
        String action = al.getActionType() != null ? al.getActionType() : "UNKNOWN";
        String detail = al.getDetail() != null ? al.getDetail() : "";
        String activityName = "Activity";
        String type = "other";
        String status = "Completed";
        String refPrefix = "ACT";

        if (action.startsWith("RECEIVE_")) {
            activityName = "Receive Items";
            type = "import";
            refPrefix = "RCV";
            status = action.endsWith("REJECT") ? "Rejected" : "Completed";
        } else if (action.startsWith("STOCK_OUT_")) {
            activityName = "Stock Out";
            type = "export";
            refPrefix = "SO";
            status = action.endsWith("REJECT") ? "Rejected" : "Completed";
        } else if (action.startsWith("TRANSFER_")) {
            activityName = "Transfer Product";
            type = "export";
            refPrefix = "MOV";
            status = action.endsWith("REJECT") ? "Rejected" : "Completed";
        } else if ("PO_CREATE".equalsIgnoreCase(action)) {
            activityName = "Purchase Order";
            type = "import";
            refPrefix = "PO";
            status = "Completed";
        } else if ("SO_CREATE".equalsIgnoreCase(action)) {
            activityName = "Sales Order";
            type = "sale";
            refPrefix = "SO";
            status = "Completed";
        } else if ("REQUEST_APPROVE".equalsIgnoreCase(action)) {
            activityName = "Request Approved";
            type = detail.contains("SO") ? "sale" : (detail.contains("PO") ? "import" : "other");
            refPrefix = "REQ";
            status = "Approved";
        } else if ("REQUEST_REJECT".equalsIgnoreCase(action)) {
            activityName = "Request Rejected";
            type = detail.contains("SO") ? "sale" : (detail.contains("PO") ? "import" : "other");
            refPrefix = "REQ";
            status = "Rejected";
        } else if ("REQUEST_SUBMIT".equalsIgnoreCase(action)) {
            activityName = "Request Submitted";
            type = detail.contains("SO") ? "sale" : (detail.contains("PO") ? "import" : "other");
            refPrefix = "REQ";
            status = "Pending";
        } else if ("PRODUCT_UPDATE".equalsIgnoreCase(action)) {
            activityName = "Product Update";
            type = "other";
            refPrefix = "PRD";
            status = "Completed";
        } else if (action.startsWith("CUSTOMER_")) {
            activityName = "Customer " + (action.endsWith("CREATE") ? "Created" : action.endsWith("UPDATE") ? "Updated" : "Deactivated");
            type = "sale";
            refPrefix = "CUST";
            status = "Completed";
        } else if (action.startsWith("SUPPLIER_")) {
            activityName = "Supplier " + (action.endsWith("CREATE") ? "Created" : action.endsWith("UPDATE") ? "Updated" : "Deactivated");
            type = "import";
            refPrefix = "SUP";
            status = "Completed";
        } else if (action.startsWith("ZONE_")) {
            activityName = "Zone " + (action.endsWith("CREATE") ? "Created" : "Updated");
            type = "other";
            refPrefix = "ZN";
            status = "Completed";
        } else if ("REQUEST_CREATE".equalsIgnoreCase(action)) {
            activityName = "Request Submitted";
            type = detail.contains("SO") ? "sale" : (detail.contains("PO") ? "import" : "other");
            refPrefix = "REQ";
            status = "Pending";
        }

        // Extract reference number from detail if available
        String reference = null;
        Matcher m = REF_PATTERN.matcher(detail);
        if (m.find()) {
            reference = refPrefix + "-" + m.group(1);
        } else {
            reference = refPrefix + "-" + al.getLogId();
        }

        String dtStr = al.getActionDatetime() != null ? al.getActionDatetime().format(DISPLAY_DT_FORMATTER) : "—";
        String isoDate = al.getActionDatetime() != null ? al.getActionDatetime().format(ISO_DATE_FORMATTER) : "";

        String actorName = al.getUser() != null ? al.getUser().getFullName() : "System";
        String actorRole = al.getRole() != null ? al.getRole().name() : "";

        return OwnerActivityDto.builder()
                .id(al.getLogId())
                .activityName(activityName)
                .type(type)
                .description(detail)
                .reference(reference)
                .dateTime(dtStr)
                .isoDate(isoDate)
                .status(status)
                .actorName(actorName)
                .actorRole(actorRole)
                .build();
    }
}
