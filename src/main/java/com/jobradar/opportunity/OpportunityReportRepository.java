package com.jobradar.opportunity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface OpportunityReportRepository extends JpaRepository<OpportunityReportEntity, Integer> {
    long countByReporter_IdAndStatus(Integer reporterId, OpportunityReportStatus status);
    @EntityGraph(attributePaths = {"opportunity"})
    Page<OpportunityReportEntity> findByReporter_Id(Integer reporterId, Pageable pageable);
    boolean existsByOpportunityIdAndReporterIdAndStatus(Integer opportunityId, Integer reporterId, OpportunityReportStatus status);
    long countByReporter_IdAndCreatedAtAfter(Integer reporterId, Instant createdAt);
    @EntityGraph(attributePaths = "opportunity")
    Page<OpportunityReportEntity> findAllByStatus(OpportunityReportStatus status, Pageable pageable);
}
