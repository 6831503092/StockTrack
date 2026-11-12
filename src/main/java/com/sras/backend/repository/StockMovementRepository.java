package com.sras.backend.repository;

import com.sras.backend.entity.StockMovement;
import com.sras.backend.enums.MovementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {
    List<StockMovement> findByStatus(MovementStatus status);
    Optional<StockMovement> findByRequestRequestId(Integer requestId);
}
