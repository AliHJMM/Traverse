package com.traverse.travel.repository;

import com.traverse.travel.entity.Report;
import com.traverse.travel.entity.ReportStatus;
import com.traverse.travel.entity.ReportSubjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByReporterId(Long reporterId);

    List<Report> findAllByOrderByCreatedAtDesc();

    long countBySubjectTypeAndSubjectId(ReportSubjectType subjectType, Long subjectId);

    long countByStatus(ReportStatus status);
}
