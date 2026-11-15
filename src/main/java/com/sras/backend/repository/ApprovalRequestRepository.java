package com.sras.backend.repository;

import com.sras.backend.entity.ApprovalRequest;
import com.sras.backend.enums.ApprovalStatus;
import com.sras.backend.enums.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Integer> {
    List<ApprovalRequest> findByRequestedByUserIdOrderByRequestedDatetimeDesc(Integer userId);
    List<ApprovalRequest> findByRequestedByUserIdAndStatusOrderByRequestedDatetimeDesc(Integer userId, ApprovalStatus status);
    List<ApprovalRequest> findByStatusOrderByRequestedDatetimeDesc(ApprovalStatus status);
    List<ApprovalRequest> findAllByOrderByRequestedDatetimeDesc();
    List<ApprovalRequest> findByRequestTypeAndStatus(RequestType requestType, ApprovalStatus status);
    boolean existsByRequestTypeAndTargetIdAndStatus(RequestType requestType, Integer targetId, ApprovalStatus status);
}
