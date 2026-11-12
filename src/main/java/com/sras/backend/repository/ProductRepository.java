package com.sras.backend.repository;

import com.sras.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByIsActiveTrue();
    List<Product> findByZoneZoneIdAndIsActiveTrue(Integer zoneId);
    List<Product> findByZoneZoneCodeAndIsActiveTrue(String zoneCode);
    boolean existsByProductName(String productName);
    long countByIsActiveTrue();
    long countByZoneZoneCodeAndIsActiveTrue(String zoneCode);
}
