package com.sras.backend.entity;

import com.sras.backend.enums.IssueCheckResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_id")
    private Integer checkId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "so_id", unique = true, nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by_user_id", nullable = false)
    private User checkedBy;

    @Column(name = "checked_datetime", nullable = false)
    private LocalDateTime checkedDatetime;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 10, nullable = false)
    private IssueCheckResult result;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;
}
