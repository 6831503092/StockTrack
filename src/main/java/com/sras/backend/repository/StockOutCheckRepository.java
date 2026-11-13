package com.sras.backend.repository;

import com.sras.backend.entity.StockOutCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockOutCheckRepository extends JpaRepository<StockOutCheck, Integer> {
    Optional<StockOutCheck> findBySalesOrderSoId(Integer soId);
}
