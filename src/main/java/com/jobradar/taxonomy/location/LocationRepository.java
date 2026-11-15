package com.jobradar.taxonomy.location;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocationRepository extends JpaRepository<LocationEntity,Integer> {
    @Query("select l from LocationEntity l where l.active=true and (:q is null or lower(l.city) like lower(concat('%',:q,'%')) or lower(l.state) like lower(concat('%',:q,'%')) or lower(l.country) like lower(concat('%',:q,'%')))")
    Page<LocationEntity> browse(@Param("q")String q, Pageable p);
    @Query("select l from LocationEntity l where (:q is null or lower(l.city) like lower(concat('%',:q,'%')) or lower(l.state) like lower(concat('%',:q,'%')) or lower(l.country) like lower(concat('%',:q,'%')))")
    Page<LocationEntity> adminBrowse(@Param("q")String q, Pageable p);
    Optional<LocationEntity> findByIdAndActiveTrue(Integer id);
    @Query("select count(l)>0 from LocationEntity l where lower(l.city)=lower(:city) and lower(coalesce(l.state,''))=lower(coalesce(:state,'')) and lower(l.country)=lower(:country) and (:id is null or l.id<>:id)")
    boolean duplicate(@Param("city")String city,@Param("state")String state,@Param("country")String country,@Param("id")Integer id);
}
