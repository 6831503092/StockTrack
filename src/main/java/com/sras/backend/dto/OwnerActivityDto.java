package com.sras.backend.dto;

import com.sras.backend.entity.AuditLog;
import com.sras.backend.enums.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerActivityDto {
    private static final DateTimeFormatter DISPLAY_DT_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern REF_PATTERN = Pattern.compile("(?:PO|SO|Request|movement|product ID|customer|supplier|zone)\\s*#?([0-9]+|[A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);

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

    public static OwnerActivityDto fromEntity(AuditLog al) {
        if (al == null) return null;

        AuditAction meta = AuditAction.fromString(al.getActionType());
        String detail = al.getDetail() != null ? al.getDetail() : "";
        String targetType = al.getTargetType();
        String targetId = al.getTargetId();

        String activityName = meta.getActivityName();
        String type = meta.resolveType(targetType, detail);
        String status = meta.getStatus();
        String refPrefix = meta.getRefPrefix();

        if (targetType != null && !targetType.isBlank()) {
            if ("PO".equalsIgnoreCase(targetType)) refPrefix = "PO";
            else if ("SO".equalsIgnoreCase(targetType)) refPrefix = "SO";
            else if ("MOVEMENT".equalsIgnoreCase(targetType) || "MOV".equalsIgnoreCase(targetType)) refPrefix = "MOV";
            else if ("PRODUCT".equalsIgnoreCase(targetType) || "PRD".equalsIgnoreCase(targetType)) refPrefix = "PRD";
            else if ("CUSTOMER".equalsIgnoreCase(targetType) || "CUST".equalsIgnoreCase(targetType)) refPrefix = "CUST";
            else if ("SUPPLIER".equalsIgnoreCase(targetType) || "SUP".equalsIgnoreCase(targetType)) refPrefix = "SUP";
            else if ("ZONE".equalsIgnoreCase(targetType) || "ZN".equalsIgnoreCase(targetType)) refPrefix = "ZN";
            else if ("USER".equalsIgnoreCase(targetType) || "USR".equalsIgnoreCase(targetType)) refPrefix = "USR";
        }

        String reference;
        if (targetId != null && !targetId.isBlank()) {
            reference = refPrefix + "-" + targetId;
        } else {
            // Fallback for legacy records without targetId
            Matcher m = REF_PATTERN.matcher(detail);
            if (m.find()) {
                reference = refPrefix + "-" + m.group(1);
            } else {
                reference = refPrefix + "-" + al.getLogId();
            }
        }

        String dtStr = al.getActionDatetime() != null ? al.getActionDatetime().format(DISPLAY_DT_FORMATTER) : "—";
        String isoDate = al.getActionDatetime() != null ? al.getActionDatetime().format(ISO_DATE_FORMATTER) : "";

        String actorName = al.getUser() != null ? al.getUser().getFullName() : "System";
        String actorRole = al.getRole() != null ? al.getRole().name() : "";

        return OwnerActivityDto.builder()
                .id(al.getLogId())
                .activityName(activityName)
                .type(type)
                .description(detail)
                .reference(reference)
                .dateTime(dtStr)
                .isoDate(isoDate)
                .status(status)
                .actorName(actorName)
                .actorRole(actorRole)
                .build();
    }
}
