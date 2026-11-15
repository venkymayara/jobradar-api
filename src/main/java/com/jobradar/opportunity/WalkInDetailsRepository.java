package com.jobradar.opportunity;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalkInDetailsRepository extends JpaRepository<WalkInDetailsEntity, Integer> {
    Optional<WalkInDetailsEntity> findByOpportunityId(Integer opportunityId);
    void deleteByOpportunityId(Integer opportunityId);
    @Query("select d from WalkInDetailsEntity d where d.driveEndAtUtc <= :now and d.opportunity.status=:status and d.opportunity.opportunityType=:type order by d.driveEndAtUtc")
    java.util.List<WalkInDetailsEntity> findExpired(@Param("now") Instant now,
            @Param("status") OpportunityStatus status, @Param("type") OpportunityType type, Pageable pageable);
}
