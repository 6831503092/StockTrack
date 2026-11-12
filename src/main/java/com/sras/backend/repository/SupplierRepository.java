package com.sras.backend.repository;

import com.sras.backend.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    List<Supplier> findByIsActiveTrue();
    boolean existsByCompanyName(String companyName);
    Optional<Supplier> findByCompanyName(String companyName);
}
