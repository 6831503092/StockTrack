package com.sras.backend.enums;

import lombok.Getter;

@Getter
public enum AuditAction {
    RECEIVE_ACCEPT("Receive Items", "import", "Completed", "PO", "PO"),
    RECEIVE_REJECT("Receive Items", "import", "Rejected", "PO", "PO"),
    STOCK_OUT_CONFIRM("Stock Out", "export", "Completed", "SO", "SO"),
    STOCK_OUT_REJECT("Stock Out", "export", "Rejected", "SO", "SO"),
    TRANSFER_VERIFY("Transfer Product", "export", "Completed", "MOV", "MOVEMENT"),
    TRANSFER_REJECT("Transfer Product", "export", "Rejected", "MOV", "MOVEMENT"),
    PO_CREATE("Purchase Order", "import", "Pending", "PO", "PO"),
    SO_CREATE("Sales Order", "sale", "Pending", "SO", "SO"),
    REQUEST_APPROVE("Request Approved", "other", "Approved", "REQ", "REQUEST"),
    REQUEST_REJECT("Request Rejected", "other", "Rejected", "REQ", "REQUEST"),
    REQUEST_SUBMIT("Request Submitted", "other", "Pending", "REQ", "REQUEST"),
    REQUEST_CREATE("Request Submitted", "other", "Pending", "REQ", "REQUEST"),
    PRODUCT_UPDATE("Product Update", "other", "Completed", "PRD", "PRODUCT"),
    CUSTOMER_CREATE("Customer Created", "sale", "Completed", "CUST", "CUSTOMER"),
    CUSTOMER_UPDATE("Customer Updated", "sale", "Completed", "CUST", "CUSTOMER"),
    CUSTOMER_DELETE("Customer Deactivated", "sale", "Completed", "CUST", "CUSTOMER"),
    SUPPLIER_CREATE("Supplier Created", "import", "Completed", "SUP", "SUPPLIER"),
    SUPPLIER_UPDATE("Supplier Updated", "import", "Completed", "SUP", "SUPPLIER"),
    SUPPLIER_DELETE("Supplier Deactivated", "import", "Completed", "SUP", "SUPPLIER"),
    ZONE_CREATE("Zone Created", "other", "Completed", "ZN", "ZONE"),
    ZONE_UPDATE("Zone Updated", "other", "Completed", "ZN", "ZONE"),
    USER_CREATE("User Created", "other", "Completed", "USR", "USER"),
    ROLE_CHANGE("Role Changed", "other", "Completed", "USR", "USER"),
    UNKNOWN("Activity", "other", "Completed", "ACT", "OTHER");

    private final String activityName;
    private final String type;
    private final String status;
    private final String refPrefix;
    private final String defaultTargetType;

    AuditAction(String activityName, String type, String status, String refPrefix, String defaultTargetType) {
        this.activityName = activityName;
        this.type = type;
        this.status = status;
        this.refPrefix = refPrefix;
        this.defaultTargetType = defaultTargetType;
    }

    public static AuditAction fromString(String str) {
        if (str == null || str.isBlank()) {
            return UNKNOWN;
        }
        try {
            return AuditAction.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public String resolveType(String targetType, String detail) {
        if (this == REQUEST_APPROVE || this == REQUEST_REJECT || this == REQUEST_SUBMIT || this == REQUEST_CREATE) {
            if ("SO".equalsIgnoreCase(targetType) || (detail != null && detail.contains("SO"))) {
                return "sale";
            }
            if ("PO".equalsIgnoreCase(targetType) || (detail != null && detail.contains("PO"))) {
                return "import";
            }
        }
        return this.type;
    }
}
