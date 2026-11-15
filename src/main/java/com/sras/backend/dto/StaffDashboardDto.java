package com.sras.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDashboardDto {
    private long receiptCount;
    private long stockOutCount;
    private long transferCount;
    private long totalCount;
    private List<StaffActivityItemDto> recentActivities;
}
