package com.sras.backend.repository;

import com.sras.backend.entity.StockOutCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockOutCheckItemRepository extends JpaRepository<StockOutCheckItem, Integer> {
    List<StockOutCheckItem> findByStockOutCheckCheckId(Integer checkId);
}
