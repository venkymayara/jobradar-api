package com.jobradar.company;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<CompanyEntity, Integer> {
    @Query("select c from CompanyEntity c where c.active=true and (:search is null or lower(c.companyName) like lower(concat('%',:search,'%')) or lower(c.industry) like lower(concat('%',:search,'%')) or lower(c.headquarters) like lower(concat('%',:search,'%')))")
    Page<CompanyEntity> searchPublic(@Param("search") String search, Pageable pageable);

    @Query("select c from CompanyEntity c where (:search is null or lower(c.companyName) like lower(concat('%',:search,'%')) or lower(c.industry) like lower(concat('%',:search,'%')) or lower(c.headquarters) like lower(concat('%',:search,'%')))")
    Page<CompanyEntity> searchAdmin(@Param("search") String search, Pageable pageable);

    Optional<CompanyEntity> findByIdAndActiveTrue(Integer id);
    Optional<CompanyEntity> findByCompanyNameIgnoreCase(String companyName);
    boolean existsByCompanyNameIgnoreCase(String companyName);
    boolean existsByCompanyNameIgnoreCaseAndIdNot(String companyName, Integer id);
}
