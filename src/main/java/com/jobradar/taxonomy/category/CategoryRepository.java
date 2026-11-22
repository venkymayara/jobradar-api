package com.jobradar.taxonomy.category;
import com.jobradar.opportunity.OpportunityRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface CategoryRepository extends JpaRepository<CategoryEntity,Integer> {
 @Query(value = """
  SELECT c.id AS categoryId, c.name AS categoryName, p.id AS parentCategoryId,
         c.slug AS slug, c.description AS description, c.icon AS icon,
         COUNT(DISTINCT o.id) AS opportunityCount
  FROM dbo.categories c
  LEFT JOIN dbo.categories p ON p.id = c.parent_category_id
  LEFT JOIN dbo.job_roles r ON r.category_id = c.id AND r.is_active = 1
  LEFT JOIN dbo.OpportunityRoles role_link ON role_link.job_role_id = r.id
  LEFT JOIN dbo.Opportunities o ON o.id = role_link.opportunity_id AND
  """ + OpportunityRepository.PUBLIC_WINDOW_SQL + """
  WHERE c.is_active = 1
  GROUP BY c.id, c.name, p.id, c.slug, c.description, c.icon
  ORDER BY COUNT(DISTINCT o.id) DESC, c.name ASC, c.id ASC
  """, nativeQuery = true)
 List<HomepageCategoryProjection> findHomepageCategories(@Param("status") String status,
         @Param("today") LocalDate today, @Param("now") Instant now, Pageable pageable);
 @Query("select c from CategoryEntity c where c.active=true and (:search is null or lower(c.name) like lower(concat('%',:search,'%'))) and (:parent is null or c.parent.id=:parent)") Page<CategoryEntity> browse(@Param("search") String search,@Param("parent") Integer parent,Pageable pageable);
 @Query("select c from CategoryEntity c where (:search is null or lower(c.name) like lower(concat('%',:search,'%'))) and (:parent is null or c.parent.id=:parent)") Page<CategoryEntity> adminBrowse(@Param("search") String search,@Param("parent") Integer parent,Pageable pageable);
 boolean existsByNameIgnoreCase(String name); boolean existsBySlugIgnoreCase(String slug);
 boolean existsByNameIgnoreCaseAndIdNot(String name,Integer id); boolean existsBySlugIgnoreCaseAndIdNot(String slug,Integer id);
 List<CategoryEntity> findAllByParent_Id(Integer id);
 List<CategoryEntity> findAllByActiveTrue();
}
