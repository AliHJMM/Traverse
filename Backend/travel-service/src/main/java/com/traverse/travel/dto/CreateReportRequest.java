package com.traverse.travel.dto;

import com.traverse.travel.entity.ReportSubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull ReportSubjectType subjectType,
        @NotNull Long subjectId,
        Long travelId,
        @NotBlank @Size(max = 2000) String reason
) {
}
