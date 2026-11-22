package com.jobradar.opportunity;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface OpportunityRepository extends JpaRepository<OpportunityEntity, Integer>, JpaSpecificationExecutor<OpportunityEntity> {
    String PUBLIC_WINDOW = """
            o.status = :status
            and (o.applicationDeadline is null or o.applicationDeadline >= :today)
            and (o.opportunityType <> com.jobradar.opportunity.OpportunityType.WALK_IN or d.driveEndAtUtc > :now)
            """;

    // SQL equivalent of PUBLIC_WINDOW for the native taxonomy count projections.
    String PUBLIC_WINDOW_SQL = """
            o.status = :status
            AND (o.application_deadline IS NULL OR o.application_deadline >= :today)
            AND (o.opportunity_type <> 'WALK_IN' OR EXISTS (
                SELECT 1 FROM dbo.WalkInDetails d
                WHERE d.opportunity_id = o.id AND d.drive_end_at_utc > :now
            ))
            """;

    long countByPublisherId(Integer publisherId);
    long countByPublisherIdAndStatus(Integer publisherId, OpportunityStatus status);
    @EntityGraph(attributePaths = {"company", "location", "publisher", "roles", "skills", "walkInDetails"})
    Page<OpportunityEntity> findByPublisherId(Integer publisherId, Pageable pageable);
    @EntityGraph(attributePaths = {"company", "location", "publisher", "roles", "skills", "walkInDetails"})
    List<OpportunityEntity> findByStatusAndApplicationDeadlineBefore(OpportunityStatus status, LocalDate deadline);
    @Query("select o from OpportunityEntity o left join o.walkInDetails d where " + PUBLIC_WINDOW
            + " order by o.publishedAt desc, o.id desc")
    @EntityGraph(attributePaths = {"company", "location", "walkInDetails"})
    List<OpportunityEntity> findLatestPublic(@Param("status") OpportunityStatus status,
            @Param("today") LocalDate today, @Param("now") java.time.Instant now, Pageable pageable);

    @Query("select o from OpportunityEntity o join o.walkInDetails d "
            + "where " + PUBLIC_WINDOW + " and o.opportunityType = :type "
            + "and d.driveStartAtUtc > :now order by d.driveStartAtUtc asc, o.id asc")
    @EntityGraph(attributePaths = {"company", "location", "walkInDetails"})
    List<OpportunityEntity> findUpcomingWalkInsForHomepage(@Param("status") OpportunityStatus status,
            @Param("type") OpportunityType type, @Param("today") LocalDate today,
            @Param("now") java.time.Instant now, Pageable pageable);
    @Override
    @EntityGraph(attributePaths = {"company", "location", "publisher", "walkInDetails"})
    Page<OpportunityEntity> findAll(Specification<OpportunityEntity> specification, Pageable pageable);

    @Query("select o from OpportunityEntity o where o.id=:id")
    @EntityGraph(attributePaths = {"company", "location", "publisher", "roles", "skills", "walkInDetails"})
    Optional<OpportunityEntity> findExpandedById(@Param("id") Integer id);
}
