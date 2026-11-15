package com.sras.backend.repository;

import com.sras.backend.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Integer> {
    List<SalesOrderItem> findBySalesOrderSoId(Integer soId);
}
