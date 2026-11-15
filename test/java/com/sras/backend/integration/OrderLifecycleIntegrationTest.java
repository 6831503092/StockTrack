package com.sras.backend.integration;

import com.sras.backend.dto.PurchaseOrderCreateRequest;
import com.sras.backend.dto.PurchaseOrderDto;
import com.sras.backend.dto.PurchaseOrderItemRequest;
import com.sras.backend.dto.SalesOrderCreateRequest;
import com.sras.backend.dto.SalesOrderDto;
import com.sras.backend.dto.SalesOrderItemRequest;
import com.sras.backend.dto.StaffCheckRequest;
import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.entity.Customer;
import com.sras.backend.entity.Product;
import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.entity.SalesOrder;
import com.sras.backend.entity.Supplier;
import com.sras.backend.entity.User;
import com.sras.backend.entity.Zone;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.PurchaseOrderStatus;
import com.sras.backend.enums.Role;
import com.sras.backend.enums.SalesOrderStatus;
import com.sras.backend.repository.ApprovalRequestRepository;
import com.sras.backend.repository.CustomerRepository;
import com.sras.backend.repository.ProductRepository;
import com.sras.backend.repository.PurchaseOrderRepository;
import com.sras.backend.repository.SalesOrderRepository;
import com.sras.backend.repository.SupplierRepository;
import com.sras.backend.repository.UserRepository;
import com.sras.backend.repository.ZoneRepository;
import com.sras.backend.service.ApprovalRequestService;
import com.sras.backend.service.PurchaseOrderService;
import com.sras.backend.service.ReceivingService;
import com.sras.backend.service.SalesOrderService;
import com.sras.backend.service.StockOutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class OrderLifecycleIntegrationTest {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private ApprovalRequestService approvalRequestService;

    @Autowired
    private ReceivingService receivingService;

    @Autowired
    private StockOutService stockOutService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private ApprovalRequestRepository approvalRequestRepository;

    private User admin;
    private User manager;
    private User staff;
    private Zone zone;
    private Product product;
    private Supplier supplier;
    private Customer customer;

    private User createAndSaveUser(Integer id, String name, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setFullName(name);
        u.setPasswordHash("hash");
        u.setRole(role);
        u.setIsActive(true);
        return userRepository.save(u);
    }

    @BeforeEach
    void setupTestData() {
        admin = createAndSaveUser(503901, "Test Admin", Role.ADMIN);
        manager = createAndSaveUser(503902, "Test Manager", Role.MANAGER);
        staff = createAndSaveUser(503903, "Test Staff", Role.STAFF);

        Zone z = new Zone();
        z.setZoneCode("ZX");
        z.setZoneName("Zone X");
        z.setCapacity(1000);
        zone = zoneRepository.save(z);

        Product p = new Product();
        p.setProductName("Modular Executive Desk");
        p.setZone(zone);
        p.setCurrentStock(10);
        p.setDefaultCost(BigDecimal.valueOf(1000));
        p.setSellingPrice(BigDecimal.valueOf(2500));
        p.setMinStockAlert(5);
        p.setIsActive(true);
        product = productRepository.save(p);

        supplier = supplierRepository.save(Supplier.builder()
                .companyName("Elite Furniture Supply")
                .zoneCode("ZX")
                .isActive(true)
                .build());

        customer = customerRepository.save(Customer.builder()
                .customerName("Corporate Offices Ltd")
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("Main Flow 1: PO Create -> Approve -> Receive Stock Adjustment")
    void purchaseOrderLifecycleFlow() {
        int initialStock = product.getCurrentStock();
        int orderQty = 50;

        // 1. Admin creates Purchase Order
        PurchaseOrderItemRequest itemReq = new PurchaseOrderItemRequest();
        itemReq.setProductId(product.getProductId());
        itemReq.setQuantity(orderQty);
        itemReq.setUnitCost(BigDecimal.valueOf(1000));

        PurchaseOrderCreateRequest poCreateReq = new PurchaseOrderCreateRequest();
        poCreateReq.setSupplierId(supplier.getSupplierId());
        poCreateReq.setItems(List.of(itemReq));
        poCreateReq.setNote("Restock for Q4");

        PurchaseOrderDto createdPo = purchaseOrderService.createPurchaseOrder(poCreateReq, admin);
        assertNotNull(createdPo);
        assertEquals("Pending", createdPo.getStatus());

        Integer poId = createdPo.getId();
        PurchaseOrder poInDb = purchaseOrderRepository.findById(poId).orElseThrow();
        assertEquals(PurchaseOrderStatus.PENDING, poInDb.getStatus());
        assertNotNull(poInDb.getRequest());
        Integer requestId = poInDb.getRequest().getRequestId();

        // 2. Manager approves the PO Create Request
        approvalRequestService.approveRequest(requestId, manager, "Approved for inventory replenishment");

        ApprovalRequest approvedReq = approvalRequestRepository.findById(requestId).orElseThrow();
        assertEquals(ApprovalStatus.APPROVED, approvedReq.getStatus());

        PurchaseOrder approvedPo = purchaseOrderRepository.findById(poId).orElseThrow();
        assertEquals(PurchaseOrderStatus.APPROVED, approvedPo.getStatus());

        // 3. Staff accepts delivery & verifies receiving
        StaffCheckRequest receiveReq = new StaffCheckRequest();
        receiveReq.setNotes("Delivered in good condition");

        receivingService.acceptReceive(poId, staff, receiveReq);

        PurchaseOrder receivedPo = purchaseOrderRepository.findById(poId).orElseThrow();
        assertEquals(PurchaseOrderStatus.RECEIVED, receivedPo.getStatus());

        // 4. Verify stock adjustment in database
        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals(initialStock + orderQty, updatedProduct.getCurrentStock());
    }

    @Test
    @DisplayName("Main Flow 2: SO Create -> Approve -> Stock-out Stock Adjustment")
    void salesOrderLifecycleFlow() {
        // Pre-condition: replenish stock to 100 units
        product.setCurrentStock(100);
        productRepository.save(product);

        int initialStock = product.getCurrentStock();
        int saleQty = 30;

        // 1. Admin creates Sales Order
        SalesOrderItemRequest itemReq = new SalesOrderItemRequest();
        itemReq.setProductId(product.getProductId());
        itemReq.setQuantity(saleQty);
        itemReq.setUnitPrice(BigDecimal.valueOf(2500));

        SalesOrderCreateRequest soCreateReq = new SalesOrderCreateRequest();
        soCreateReq.setCustomerId(customer.getCustomerId());
        soCreateReq.setItems(List.of(itemReq));
        soCreateReq.setNote("Urgent delivery order");

        SalesOrderDto createdSo = salesOrderService.createSalesOrder(soCreateReq, admin);
        assertNotNull(createdSo);
        assertEquals("Pending", createdSo.getStatus());

        Integer soId = createdSo.getId();
        SalesOrder soInDb = salesOrderRepository.findById(soId).orElseThrow();
        assertEquals(SalesOrderStatus.PENDING, soInDb.getStatus());
        assertNotNull(soInDb.getRequest());
        Integer requestId = soInDb.getRequest().getRequestId();

        // 2. Manager approves the SO Create Request
        approvalRequestService.approveRequest(requestId, manager, "Customer credit verified and approved");

        ApprovalRequest approvedReq = approvalRequestRepository.findById(requestId).orElseThrow();
        assertEquals(ApprovalStatus.APPROVED, approvedReq.getStatus());

        SalesOrder approvedSo = salesOrderRepository.findById(soId).orElseThrow();
        assertEquals(SalesOrderStatus.APPROVED, approvedSo.getStatus());

        // 3. Staff confirms stock-out inspection
        StaffCheckRequest stockOutReq = new StaffCheckRequest();
        stockOutReq.setNotes("Items picked and dispatched via courier");

        stockOutService.confirmStockOut(soId, staff, stockOutReq);

        SalesOrder completedSo = salesOrderRepository.findById(soId).orElseThrow();
        assertEquals(SalesOrderStatus.STOCK_OUT, completedSo.getStatus());

        // 4. Verify stock adjustment in database
        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertEquals(initialStock - saleQty, updatedProduct.getCurrentStock());
    }
}
