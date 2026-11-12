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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "issue_check_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueCheckItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_item_id")
    private Integer checkItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_id", nullable = false)
    private IssueCheck issueCheck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "so_item_id", nullable = false)
    private SalesOrderItem salesOrderItem;

    @Column(name = "expected_qty", nullable = false)
    private Integer expectedQty;

    @Column(name = "counted_qty", nullable = false)
    private Integer countedQty;

    @Column(name = "is_correct_product", nullable = false)
    private Boolean isCorrectProduct;
}
