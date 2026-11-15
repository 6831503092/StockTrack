package com.sras.backend.repository;

import com.sras.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    List<Customer> findByIsActiveTrue();
    boolean existsByCustomerName(String customerName);
    Optional<Customer> findByCustomerName(String customerName);
}
