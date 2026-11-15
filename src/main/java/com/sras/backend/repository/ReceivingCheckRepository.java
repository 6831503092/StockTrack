package com.sras.backend.repository;

import com.sras.backend.entity.ReceivingCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceivingCheckRepository extends JpaRepository<ReceivingCheck, Integer> {
    Optional<ReceivingCheck> findByPurchaseOrderPoId(Integer poId);
}
