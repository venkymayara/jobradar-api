package com.jobradar.saved;

import com.jobradar.auth.user.UserEntity;
import com.jobradar.auth.user.UserRepository;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.exception.ForbiddenException;
import com.jobradar.common.exception.ResourceNotFoundException;
import com.jobradar.common.pagination.PaginationParameters;
import com.jobradar.opportunity.OpportunityDto;
import com.jobradar.opportunity.OpportunityRepository;
import com.jobradar.opportunity.OpportunityStatus;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SavedOpportunityService {
    private static final Set<String> SORTS = Set.of("createdAt", "savedAt");

    private final SavedOpportunityRepository saved;
    private final UserRepository users;
    private final OpportunityRepository opportunities;

    public SavedOpportunityService(SavedOpportunityRepository saved, UserRepository users,
            OpportunityRepository opportunities) {
        this.saved = saved;
        this.users = users;
        this.opportunities = opportunities;
    }

    @Transactional
    public void save(Integer opportunityId, Authentication authentication) {
        UserEntity user = currentUser(authentication);
        var opportunity = opportunities.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
        if (opportunity.getStatus() != OpportunityStatus.ACTIVE)
            throw new ResourceNotFoundException("Opportunity is not available to save");

        SavedOpportunityId id = new SavedOpportunityId(user.getId(), opportunityId);
        if (saved.existsById(id)) throw new ConflictException("Opportunity is already saved");
        try {
            saved.saveAndFlush(new SavedOpportunityEntity(user, opportunity));
        } catch (DataIntegrityViolationException exception) {
            // The composite key also handles two simultaneous save requests safely.
            throw new ConflictException("Opportunity is already saved");
        }
    }

    @Transactional
    public void remove(Integer opportunityId, Authentication authentication) {
        UserEntity user = currentUser(authentication);
        saved.findById(new SavedOpportunityId(user.getId(), opportunityId)).ifPresent(saved::delete);
    }

    public PaginatedResponse<OpportunityDto> list(PaginationParameters pagination, Authentication authentication) {
        UserEntity user = currentUser(authentication);
        String sort = pagination.getSort();
        if (!SORTS.contains(sort)) throw new com.jobradar.common.exception.BadRequestException("Unsupported saved opportunity sort field");
        String sortProperty = "savedAt";
        var pageable = PageRequest.of(pagination.getPage(), pagination.getSize(),
                Sort.by(Sort.Direction.fromString(pagination.getDirection()), sortProperty));
        return PaginatedResponse.from(saved.findAvailableOpportunities(user.getId(), OpportunityStatus.ACTIVE, pageable)
                .map(entry -> OpportunityDto.from(entry.getOpportunity(), List.of())));
    }

    private UserEntity currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new ForbiddenException("Authentication is required");
        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
    }
}
