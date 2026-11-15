package com.jobradar.opportunity;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface OpportunityRepository extends JpaRepository<OpportunityEntity, Integer>, JpaSpecificationExecutor<OpportunityEntity> {
    @EntityGraph(attributePaths = {"company", "location", "publisher", "walkInDetails"})
    List<OpportunityEntity> findTop10ByStatusOrderByPublishedAtDesc(OpportunityStatus status);

    @Query("select o from OpportunityEntity o join o.walkInDetails d "
            + "where o.status = :status and o.opportunityType = :type "
            + "and d.driveStartAtUtc > :now and d.driveEndAtUtc > :now order by d.driveStartAtUtc asc")
    @EntityGraph(attributePaths = {"company", "location", "walkInDetails"})
    List<OpportunityEntity> findUpcomingWalkInsForHomepage(@Param("status") OpportunityStatus status,
            @Param("type") OpportunityType type, @Param("now") java.time.Instant now, Pageable pageable);
    @Override
    @EntityGraph(attributePaths = {"company", "location", "publisher", "walkInDetails"})
    Page<OpportunityEntity> findAll(Specification<OpportunityEntity> specification, Pageable pageable);

    @Query("select o from OpportunityEntity o where o.id=:id")
    @EntityGraph(attributePaths = {"company", "location", "publisher", "roles", "skills", "walkInDetails"})
    Optional<OpportunityEntity> findExpandedById(@Param("id") Integer id);
}
