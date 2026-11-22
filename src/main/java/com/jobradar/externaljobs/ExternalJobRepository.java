package com.jobradar.externaljobs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExternalJobRepository extends JpaRepository<ExternalJobEntity, String> {
    @Query("""
            select job from ExternalJobEntity job
            where (:keyword = '' or locate(:keyword, lower(job.title)) > 0
                or locate(:keyword, lower(job.description)) > 0 or locate(:keyword, lower(job.companyName)) > 0)
              and (:company = '' or locate(:company, lower(job.companyName)) > 0)
              and (:location = '' or locate(:location, lower(job.location)) > 0)
              and (:mode = '' or lower(job.workMode) = :mode)
            order by job.postedAt desc, job.externalId
            """)
    Page<ExternalJobEntity> search(@Param("keyword") String keyword, @Param("company") String company,
            @Param("location") String location, @Param("mode") String workMode, Pageable pageable);

    long countByProvider(String provider);
}
