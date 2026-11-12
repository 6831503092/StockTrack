package com.sras.backend.repository;

import com.sras.backend.entity.IssueCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueCheckItemRepository extends JpaRepository<IssueCheckItem, Integer> {
    List<IssueCheckItem> findByIssueCheckCheckId(Integer checkId);
}
