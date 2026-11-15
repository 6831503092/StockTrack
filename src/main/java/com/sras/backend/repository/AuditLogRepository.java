package com.sras.backend.repository;

import com.sras.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
    List<AuditLog> findAllByOrderByActionDatetimeDesc();
    List<AuditLog> findByUserUserIdOrderByActionDatetimeDesc(Integer userId);
}
