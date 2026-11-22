package com.jobradar.opportunity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OpportunityStatusHistoryRepository extends JpaRepository<OpportunityStatusHistoryEntity, Long> {
    List<OpportunityStatusHistoryEntity> findByOpportunityIdOrderByChangedAtAscIdAsc(Integer opportunityId);
}
