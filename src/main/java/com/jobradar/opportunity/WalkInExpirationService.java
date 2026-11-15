package com.jobradar.opportunity;

import java.time.Clock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalkInExpirationService {
    private static final int BATCH_SIZE = 100;
    private final WalkInDetailsRepository walkIns;
    private final OpportunityStatusHistoryRepository history;
    private final Clock clock;

    public WalkInExpirationService(WalkInDetailsRepository walkIns,
            OpportunityStatusHistoryRepository history, Clock clock) {
        this.walkIns = walkIns; this.history = history; this.clock = clock;
    }

    @Scheduled(initialDelayString = "${app.walk-ins.expiry-initial-delay-ms:60000}",
            fixedDelayString = "${app.walk-ins.expiry-scan-interval-ms:60000}")
    @Transactional
    public int expireDueDrives() {
        int changed = 0;
        while (true) {
            var expired = walkIns.findExpired(clock.instant(), OpportunityStatus.ACTIVE,
                    OpportunityType.WALK_IN, PageRequest.of(0, BATCH_SIZE));
            if (expired.isEmpty()) return changed;
            for (WalkInDetailsEntity details : expired) {
                OpportunityEntity opportunity = details.getOpportunity();
                OpportunityStatus previous = opportunity.getStatus();
                opportunity.transition(OpportunityStatus.EXPIRED);
                history.save(new OpportunityStatusHistoryEntity(opportunity, previous,
                        OpportunityStatus.EXPIRED, "SYSTEM"));
                changed++;
            }
        }
    }
}
