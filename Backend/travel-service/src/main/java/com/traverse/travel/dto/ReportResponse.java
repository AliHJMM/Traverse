package com.traverse.travel.dto;

import com.traverse.travel.entity.ReportStatus;
import com.traverse.travel.entity.ReportSubjectType;

import java.time.Instant;

public record ReportResponse(
        Long id,
        Long reporterId,
        ReportSubjectType subjectType,
        Long subjectId,
        Long travelId,
        String reason,
        ReportStatus status,
        Instant createdAt
) {
}
