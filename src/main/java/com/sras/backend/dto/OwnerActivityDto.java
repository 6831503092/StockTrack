package com.sras.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerActivityDto {
    private Integer id;
    private String activityName;
    private String type;         // "sale", "import", "export", "other"
    private String description;
    private String reference;
    private String dateTime;
    private String isoDate;      // "yyyy-MM-dd"
    private String status;       // "Completed", "Approved", "Rejected", "Pending"
    private String actorName;
    private String actorRole;
}
