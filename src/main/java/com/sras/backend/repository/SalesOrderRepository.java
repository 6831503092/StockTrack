package com.sras.backend.repository;

import com.sras.backend.entity.SalesOrder;
import com.sras.backend.enums.SalesOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Integer> {
    List<SalesOrder> findByStatusOrderByOrderDatetimeDesc(SalesOrderStatus status);
    List<SalesOrder> findAllByOrderByOrderDatetimeDesc();
    Optional<SalesOrder> findByRequestRequestId(Integer requestId);
}
