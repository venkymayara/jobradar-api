package com.jobradar.saved;

import com.jobradar.opportunity.OpportunityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedOpportunityRepository extends JpaRepository<SavedOpportunityEntity, SavedOpportunityId> {
    @Query("select s from SavedOpportunityEntity s "
            + "where s.id.userId = :userId and s.opportunity.status = :status")
    @EntityGraph(attributePaths = {"opportunity", "opportunity.company", "opportunity.location", "opportunity.walkInDetails"})
    Page<SavedOpportunityEntity> findAvailableOpportunities(@Param("userId") Integer userId,
            @Param("status") OpportunityStatus status, Pageable pageable);
}
