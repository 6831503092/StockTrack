package com.sras.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "product_name", length = 50, nullable = false)
    private String productName;

    @Column(name = "default_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal defaultCost;

    @Column(name = "selling_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock = 0;

    @Column(name = "min_stock_alert", nullable = false)
    private Integer minStockAlert = 20;

    @Column(name = "po_min_qty", nullable = false)
    private Integer poMinQty = 1;

    @Column(name = "po_max_qty")
    private Integer poMaxQty;

    @Column(name = "so_min_qty", nullable = false)
    private Integer soMinQty = 1;

    @Column(name = "so_max_qty")
    private Integer soMaxQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

