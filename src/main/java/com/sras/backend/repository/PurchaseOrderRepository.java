package com.sras.backend.repository;

import com.sras.backend.entity.PurchaseOrder;
import com.sras.backend.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {
    List<PurchaseOrder> findByStatusOrderByOrderDatetimeDesc(PurchaseOrderStatus status);
    List<PurchaseOrder> findAllByOrderByOrderDatetimeDesc();
    Optional<PurchaseOrder> findByRequestRequestId(Integer requestId);
}
