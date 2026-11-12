package com.sras.backend.repository;

import com.sras.backend.entity.IssueCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IssueCheckRepository extends JpaRepository<IssueCheck, Integer> {
    Optional<IssueCheck> findBySalesOrderSoId(Integer soId);
}
