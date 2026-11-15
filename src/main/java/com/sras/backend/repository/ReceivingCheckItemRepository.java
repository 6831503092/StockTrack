package com.sras.backend.repository;

import com.sras.backend.entity.ReceivingCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceivingCheckItemRepository extends JpaRepository<ReceivingCheckItem, Integer> {
    List<ReceivingCheckItem> findByReceivingCheckCheckId(Integer checkId);
}
