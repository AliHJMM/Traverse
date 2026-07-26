package com.traverse.travel.controller;

import com.traverse.travel.dto.CreateReportRequest;
import com.traverse.travel.dto.ReportResponse;
import com.traverse.travel.security.AuthenticatedUser;
import com.traverse.travel.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/travels/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Any authenticated user can file a report against a manager or traveler. */
    @PostMapping
    public ResponseEntity<ReportResponse> file(@Valid @RequestBody CreateReportRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.file(principal.id(), request));
    }

    /** Admin reviews all filed reports. */
    @GetMapping
    public List<ReportResponse> all() {
        return reportService.all();
    }

    /** Reports the current user has filed. */
    @GetMapping("/mine")
    public List<ReportResponse> mine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return reportService.filedBy(principal.id());
    }

    /** Admin marks a report as reviewed. */
    @PatchMapping("/{id}/review")
    public ReportResponse review(@PathVariable Long id) {
        return reportService.markReviewed(id);
    }
}
