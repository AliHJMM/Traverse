package com.traverse.travel.service;

import com.traverse.travel.dto.CreateReportRequest;
import com.traverse.travel.dto.ReportResponse;
import com.traverse.travel.entity.Report;
import com.traverse.travel.exception.ReportNotFoundException;
import com.traverse.travel.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public ReportResponse file(Long reporterId, CreateReportRequest request) {
        Report report = new Report(reporterId, request.subjectType(), request.subjectId(),
                request.travelId(), request.reason());
        return toResponse(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> all() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> filedBy(Long reporterId) {
        return reportRepository.findByReporterId(reporterId).stream().map(this::toResponse).toList();
    }

    public ReportResponse markReviewed(Long id) {
        Report report = reportRepository.findById(id).orElseThrow(() -> new ReportNotFoundException(id));
        report.markReviewed();
        return toResponse(reportRepository.save(report));
    }

    private ReportResponse toResponse(Report r) {
        return new ReportResponse(r.getId(), r.getReporterId(), r.getSubjectType(), r.getSubjectId(),
                r.getTravelId(), r.getReason(), r.getStatus(), r.getCreatedAt());
    }
}
