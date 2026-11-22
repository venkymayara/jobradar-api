package com.jobradar.opportunity;

import com.jobradar.notification.NotificationService;
import com.jobradar.notification.NotificationType;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityExpirationService {
    private final OpportunityRepository opportunities;
    private final OpportunityStatusHistoryRepository history;
    private final NotificationService notifications;
    private final Clock clock;
    private final boolean enabled;
    public OpportunityExpirationService(OpportunityRepository opportunities, OpportunityStatusHistoryRepository history,
            NotificationService notifications, Clock clock, @Value("${app.opportunities.expiry-enabled:true}") boolean enabled) {
        this.opportunities = opportunities; this.history = history; this.notifications = notifications; this.clock = clock; this.enabled = enabled;
    }
    @Scheduled(initialDelayString = "${app.opportunities.expiry-initial-delay-ms:60000}", fixedDelayString = "${app.opportunities.expiry-scan-interval-ms:3600000}")
    @Transactional
    public int expireDueOpportunities() {
        if (!enabled) return 0;
        int changed = 0;
        for (OpportunityEntity opportunity : opportunities.findByStatusAndApplicationDeadlineBefore(OpportunityStatus.ACTIVE, LocalDate.now(clock))) {
            OpportunityStatus previous = opportunity.getStatus();
            opportunity.transition(OpportunityStatus.EXPIRED);
            history.save(new OpportunityStatusHistoryEntity(opportunity, previous, OpportunityStatus.EXPIRED, "SYSTEM"));
            notifications.create(opportunity.getPublisher(), "Opportunity expired", "Your opportunity \"" + opportunity.getTitle() + "\" has expired.", NotificationType.OPPORTUNITY_EXPIRED, opportunity.getId());
            changed++;
        }
        return changed;
    }
}
