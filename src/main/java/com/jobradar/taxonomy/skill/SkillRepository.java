package com.jobradar.taxonomy.skill;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillRepository extends JpaRepository<SkillEntity, Integer> {
    @Query("select s from SkillEntity s where s.active=true and (:q is null or lower(s.name) like lower(concat('%',:q,'%')))")
    Page<SkillEntity> browse(@Param("q") String q, Pageable pageable);

    @Query("select s from SkillEntity s where (:q is null or lower(s.name) like lower(concat('%',:q,'%'))) ")
    Page<SkillEntity> adminBrowse(@Param("q") String q, Pageable pageable);

    @Query(value = """
        SELECT s.id AS skillId, s.name AS skillName, s.slug AS slug,
               COUNT(DISTINCT o.id) AS opportunityCount
        FROM dbo.skills s
        LEFT JOIN dbo.OpportunitySkills skill_link ON skill_link.skill_id = s.id
        LEFT JOIN dbo.Opportunities o ON o.id = skill_link.opportunity_id AND o.status = :status
        WHERE s.is_active = 1
        GROUP BY s.id, s.name, s.slug
        ORDER BY COUNT(DISTINCT o.id) DESC, s.name ASC
        """, nativeQuery = true)
    List<HomepageSkillProjection> findHomepagePopular(@Param("status") String status, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Integer id);
}
