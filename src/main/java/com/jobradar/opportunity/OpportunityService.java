package com.jobradar.opportunity;

import com.jobradar.auth.user.UserEntity;
import com.jobradar.auth.user.UserRepository;
import com.jobradar.common.api.PaginatedResponse;
import com.jobradar.common.exception.BadRequestException;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.exception.ForbiddenException;
import com.jobradar.common.exception.ResourceNotFoundException;
import com.jobradar.common.pagination.PaginationParameters;
import com.jobradar.company.CompanyEntity;
import com.jobradar.company.CompanyRepository;
import com.jobradar.taxonomy.location.LocationEntity;
import com.jobradar.taxonomy.location.LocationRepository;
import com.jobradar.taxonomy.role.RoleEntity;
import com.jobradar.taxonomy.role.RoleRepository;
import com.jobradar.taxonomy.skill.SkillEntity;
import com.jobradar.taxonomy.skill.SkillRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.jobradar.notification.NotificationService;
import com.jobradar.notification.NotificationType;

@Service
@Transactional(readOnly = true)
public class OpportunityService {
    private static final Set<String> SORTS = Set.of("publishedAt", "title", "minExperience", "maxExperience", "minSalary");
    private final OpportunityRepository opportunities;
    private final OpportunityStatusHistoryRepository history;
    private final WalkInDetailsRepository walkInDetails;
    private final CompanyRepository companies;
    private final LocationRepository locations;
    private final RoleRepository roles;
    private final SkillRepository skills;
    private final UserRepository users;
    private final NotificationService notifications;
    private final Clock clock;

    @Autowired
    public OpportunityService(OpportunityRepository opportunities, OpportunityStatusHistoryRepository history,
            WalkInDetailsRepository walkInDetails,
            CompanyRepository companies, LocationRepository locations, RoleRepository roles,
            SkillRepository skills, UserRepository users, NotificationService notifications, Clock clock) {
        this.opportunities = opportunities; this.history = history; this.walkInDetails = walkInDetails; this.companies = companies;
        this.locations = locations; this.roles = roles; this.skills = skills; this.users = users; this.notifications = notifications;
        this.clock = clock;
    }
    public OpportunityService(OpportunityRepository opportunities, OpportunityStatusHistoryRepository history,
            WalkInDetailsRepository walkInDetails, CompanyRepository companies, LocationRepository locations,
            RoleRepository roles, SkillRepository skills, UserRepository users) {
        this(opportunities, history, walkInDetails, companies, locations, roles, skills, users, null, Clock.systemUTC());
    }
    public PaginatedResponse<OpportunityDto> search(String keyword, String role, List<String> skillNames,
            String company, String location, Integer minExperience, Integer maxExperience,
            WorkMode workMode, EmploymentType employmentType, OpportunityType opportunityType,
            BigDecimal minSalary, BigDecimal maxSalary, String postedWithin, PaginationParameters pagination) {
        return search(keyword, role, null, skillNames, company, location, minExperience, maxExperience, workMode,
                employmentType, opportunityType, minSalary, maxSalary, postedWithin, pagination);
    }

    public PaginatedResponse<OpportunityDto> search(String keyword, String role, Integer categoryId, List<String> skillNames,
            String company, String location, Integer minExperience, Integer maxExperience,
            WorkMode workMode, EmploymentType employmentType, OpportunityType opportunityType,
            BigDecimal minSalary, BigDecimal maxSalary, String postedWithin,
            PaginationParameters pagination) {
        validateRanges(minExperience, maxExperience, minSalary, maxSalary);
        String requestedSort = pagination.getSort();
        String requestedDirection = pagination.getDirection();
        if (requestedSort.contains(",")) {
            String[] parts = requestedSort.split(",", -1);
            if (parts.length != 2 || !parts[1].matches("(?i)ASC|DESC"))
                throw new BadRequestException("sort must use field,direction format");
            requestedSort = parts[0].trim();
            requestedDirection = parts[1].trim();
        }
        String sortName = requestedSort.equals("createdAt") ? "publishedAt" : requestedSort;
        if (!SORTS.contains(sortName)) throw new BadRequestException("Unsupported opportunity sort field");
        Sort.Direction direction = requestedSort.equals("createdAt") && pagination.getSort().equals("createdAt")
                ? Sort.Direction.DESC : Sort.Direction.fromString(requestedDirection);
        Pageable pageable = PageRequest.of(pagination.getPage(), pagination.getSize(), Sort.by(direction, sortName));
        List<String> normalizedSkills = skillNames == null ? List.of() : skillNames.stream()
                .filter(value -> value != null && !value.isBlank()).map(String::trim).distinct().toList();
        OpportunitySearchCriteria criteria = new OpportunitySearchCriteria(clean(keyword), clean(role), categoryId, normalizedSkills,
                clean(company), clean(location), minExperience, maxExperience, workMode, employmentType,
                opportunityType, minSalary, maxSalary, postedAfter(postedWithin), null, null, null);
        Page<OpportunityDto> result = opportunities.findAll(OpportunitySpecifications.publicSearch(criteria), pageable)
                .map(entity -> OpportunityDto.from(entity, List.of()));
        return PaginatedResponse.from(result);
    }

    public List<OpportunityDto> latest() {
        Instant now = clock.instant();
        return latest(LocalDate.ofInstant(now, clock.getZone()), now);
    }

    public List<OpportunityDto> latest(LocalDate today, Instant now) {
        return opportunities.findLatestPublic(OpportunityStatus.ACTIVE, today, now, PageRequest.of(0, 10))
                .stream().map(OpportunityDto::publicPreview).toList();
    }

    public PaginatedResponse<OpportunityDto> reviewQueue(PaginationParameters pagination, boolean admin) {
        return reviewQueue(pagination, admin ? null : OpportunityStatus.PENDING);
    }

    public PaginatedResponse<OpportunityDto> adminReviewQueue(PaginationParameters pagination, OpportunityStatus status) {
        if (status != OpportunityStatus.PENDING && status != OpportunityStatus.NEEDS_DETAILS)
            throw new BadRequestException("Unsupported review queue status");
        return reviewQueue(pagination, status);
    }

    private PaginatedResponse<OpportunityDto> reviewQueue(PaginationParameters pagination, OpportunityStatus status) {
        String field = pagination.getSort().equals("createdAt") ? "createdAt" : pagination.getSort();
        Set<String> reviewSorts = Set.of("createdAt", "title", "publishedAt");
        if (!reviewSorts.contains(field)) throw new BadRequestException("Unsupported opportunity sort field");
        Pageable pageable = PageRequest.of(pagination.getPage(), pagination.getSize(),
                Sort.by(Sort.Direction.fromString(pagination.getDirection()), field, "id"));
        Specification<OpportunityEntity> spec = (root, query, cb) -> status == null
                ? cb.notEqual(root.get("status"), OpportunityStatus.REMOVED)
                : cb.equal(root.get("status"), status);
        return PaginatedResponse.from(opportunities.findAll(spec, pageable)
                .map(entity -> OpportunityDto.from(entity, List.of())));
    }

    public OpportunityDto get(Integer id, Authentication authentication) {
        OpportunityEntity entity = findExpanded(id);
        boolean owner = isOwner(entity, authentication);
        boolean reviewer = isModerator(authentication);
        if (entity.getStatus() != OpportunityStatus.ACTIVE && !owner && !reviewer) notFound();
        return toDto(entity, owner || reviewer, owner && entity.getStatus() == OpportunityStatus.NEEDS_DETAILS);
    }

    public ReviewDetails getReview(Integer id, Authentication authentication) {
        requireAdministrator(authentication);
        OpportunityEntity entity = findExpanded(id);
        UserEntity publisher = entity.getPublisher();
        String submitter = clean(publisher.getFullName());
        return new ReviewDetails(toDto(entity, true, false),
                submitter == null ? publisher.getEmail() : submitter, entity.getCreatedAt());
    }

    @Transactional
    public OpportunityDto review(Integer id, OpportunityReviewDecision decision, String note, Authentication administrator) {
        requireAdministrator(administrator);
        OpportunityEntity entity = findExpanded(id);
        if (entity.getStatus() != OpportunityStatus.PENDING)
            throw new ConflictException("This submission is no longer awaiting review");
        String feedback = clean(note);
        if (decision != OpportunityReviewDecision.APPROVE && feedback == null)
            throw new BadRequestException("Enter a note explaining the review decision");
        if (decision == OpportunityReviewDecision.APPROVE && hasExpired(entity))
            throw new ConflictException("The schedule or application deadline for this submission has expired");

        OpportunityStatus next = switch (decision) {
            case APPROVE -> OpportunityStatus.ACTIVE;
            case REJECT -> OpportunityStatus.REJECTED;
            case REQUEST_DETAILS -> OpportunityStatus.NEEDS_DETAILS;
        };
        entity.transition(next);
        history.save(new OpportunityStatusHistoryEntity(entity, OpportunityStatus.PENDING, next,
                actorName(administrator), feedback));
        String title = switch (decision) {
            case APPROVE -> "Opportunity approved";
            case REJECT -> "Opportunity rejected";
            case REQUEST_DETAILS -> "More details requested";
        };
        NotificationType notification = switch (decision) {
            case APPROVE -> NotificationType.OPPORTUNITY_APPROVED;
            case REJECT -> NotificationType.OPPORTUNITY_REJECTED;
            case REQUEST_DETAILS -> NotificationType.OPPORTUNITY_DETAILS_REQUESTED;
        };
        if (notifications != null)
            notifications.createReviewUpdate(entity.getPublisher(), title,
                    feedback == null ? "Your opportunity has been approved and published." : feedback, notification, id);
        return toDto(entity, true, false);
    }

    @Transactional
    public OpportunityDto respondToReview(Integer id, String message, Authentication authentication) {
        OpportunityEntity entity = findExpanded(id);
        if (!isOwner(entity, authentication))
            throw new ResourceNotFoundException("Opportunity not found");
        if (entity.getStatus() != OpportunityStatus.NEEDS_DETAILS)
            throw new ConflictException("This submission is not awaiting more details");
        entity.transition(OpportunityStatus.PENDING);
        history.save(new OpportunityStatusHistoryEntity(entity, OpportunityStatus.NEEDS_DETAILS,
                OpportunityStatus.PENDING, actorName(authentication), message.strip()));
        notifyAdministrators(entity, "Opportunity ready for another review");
        return toDto(entity, true, false);
    }

    public record ReviewDetails(OpportunityDto opportunity, String submittedBy, Instant submittedAt) {}

    private boolean hasExpired(OpportunityEntity entity) {
        LocalDate deadline = entity.getApplicationDeadline();
        if (deadline != null && deadline.isBefore(LocalDate.now(clock))) return true;
        if (entity.getOpportunityType() != OpportunityType.WALK_IN) return false;
        WalkInDetailsEntity drive = entity.getWalkInDetails();
        return drive == null || !drive.getDriveEndAtUtc().isAfter(clock.instant());
    }

    @Transactional
    public OpportunityDto create(OpportunityRequest request, String email) {
        validateRequest(request);
        UserEntity publisher = user(email);
        OpportunityEntity entity = new OpportunityEntity(request, resolveCompany(request, email),
                location(request.locationId()), publisher, selectedRoles(request.roleIds()),
                selectedSkills(request.skillIds()), OpportunityStatus.PENDING);
        if (request.opportunityType() == OpportunityType.WALK_IN)
            entity.setWalkInDetails(new WalkInDetailsEntity(entity, request));
        OpportunityEntity saved = opportunities.save(entity);
        saveHistory(saved, null, OpportunityStatus.PENDING, email);
        notify(publisher, "Opportunity submitted", "Your opportunity was submitted for moderation.", NotificationType.OPPORTUNITY_SUBMITTED, saved.getId());
        notifyAdministrators(saved, "New opportunity to review");
        return toDto(saved);
    }

    @Transactional
    public OpportunityDto update(Integer id, OpportunityRequest request, Authentication authentication) {
        validateRequest(request);
        OpportunityEntity entity = opportunities.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
        requireOwnerOrModerator(entity, authentication);
        if (Set.of(OpportunityStatus.FILLED, OpportunityStatus.EXPIRED, OpportunityStatus.REMOVED)
                .contains(entity.getStatus())) throw new ConflictException("This opportunity can no longer be edited");
        OpportunityStatus oldStatus = entity.getStatus();
        entity.update(request, company(request.companyId()), location(request.locationId()),
                selectedRoles(request.roleIds()), selectedSkills(request.skillIds()));
        if (request.opportunityType() == OpportunityType.WALK_IN) {
            WalkInDetailsEntity details = entity.getWalkInDetails();
            if (details == null) details = new WalkInDetailsEntity(entity, request);
            else details.update(request);
            entity.setWalkInDetails(details);
        } else {
            entity.setWalkInDetails(null);
        }
        if (oldStatus == OpportunityStatus.ACTIVE || oldStatus == OpportunityStatus.REJECTED) {
            entity.transition(OpportunityStatus.PENDING);
            saveHistory(entity, oldStatus, OpportunityStatus.PENDING, actorName(authentication));
            notifyAdministrators(entity, "Updated opportunity to review");
        }
        return toDto(entity);
    }

    @Transactional
    public OpportunityDto changeStatus(Integer id, OpportunityStatus next, Authentication authentication) {
        OpportunityEntity entity = opportunities.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
        boolean moderator = isModerator(authentication);
        boolean owner = isOwner(entity, authentication);
        if (!owner && !moderator) throw new ForbiddenException("You cannot change this opportunity's status");
        OpportunityStatus previous = entity.getStatus();
        if (previous == next) return toDto(entity);
        if (moderator) {
            if (!moderatorTransitionAllowed(previous, next))
                throw new ConflictException("Invalid opportunity status transition");
        } else if (!ownerTransitionAllowed(previous, next)) {
            throw new ConflictException("Invalid opportunity status transition");
        }
        entity.transition(next);
        saveHistory(entity, previous, next, actorName(authentication));
        NotificationType type = switch (next) {
            case ACTIVE -> NotificationType.OPPORTUNITY_APPROVED;
            case REJECTED -> NotificationType.OPPORTUNITY_REJECTED;
            case PAUSED -> NotificationType.OPPORTUNITY_PAUSED;
            case FILLED -> NotificationType.OPPORTUNITY_FILLED;
            case EXPIRED -> NotificationType.OPPORTUNITY_EXPIRED;
            default -> null;
        };
        if (type != null) notify(entity.getPublisher(), "Opportunity status updated", "Your opportunity is now " + next.name().toLowerCase(Locale.ROOT) + ".", type, entity.getId());
        return toDto(entity);
    }

    @Transactional
    public OpportunityDto verify(Integer id, boolean verified, Authentication authentication) {
        if (!isModerator(authentication)) throw new ForbiddenException("Only moderators or administrators can verify opportunities");
        OpportunityEntity entity = opportunities.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
        if (verified && entity.getStatus() != OpportunityStatus.ACTIVE)
            throw new ConflictException("Only active opportunities can be verified");
        entity.setVerified(verified);
        return toDto(entity);
    }

    @Transactional
    public void remove(Integer id, Authentication authentication) {
        changeStatus(id, OpportunityStatus.REMOVED, authentication);
    }

    private OpportunityDto toDto(OpportunityEntity entity) {
        return toDto(entity, false, false);
    }

    private OpportunityDto toDto(OpportunityEntity entity, boolean includeNotes, boolean canRespond) {
        List<OpportunityStatusHistoryDto> entries = history.findByOpportunityIdOrderByChangedAtAscIdAsc(entity.getId())
                .stream().map(entry -> OpportunityStatusHistoryDto.from(entry, includeNotes)).toList();
        return OpportunityDto.detail(entity, entries, canRespond);
    }

    private void saveHistory(OpportunityEntity entity, OpportunityStatus previous, OpportunityStatus next, String actor) {
        history.save(new OpportunityStatusHistoryEntity(entity, previous, next, actor));
    }

    private OpportunityEntity findExpanded(Integer id) {
        return opportunities.findExpandedById(id).orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }

    private UserEntity user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User account not found"));
    }

    private CompanyEntity company(Integer id) {
        return companies.findByIdAndActiveTrue(id).orElseThrow(() -> new BadRequestException("Select an active company"));
    }

    private CompanyEntity resolveCompany(OpportunityRequest request, String email) {
        if (request.companyId() != null) return company(request.companyId());
        String name = request.companyName() == null ? "" : request.companyName().trim();
        return companies.findByCompanyNameIgnoreCase(name).orElseGet(() ->
                companies.save(new CompanyEntity(name, null, null, null, null, null, true, email)));
    }

    private LocationEntity location(Integer id) {
        return locations.findByIdAndActiveTrue(id).orElseThrow(() -> new BadRequestException("Select an active location"));
    }

    private Set<RoleEntity> selectedRoles(Set<Integer> ids) {
        List<RoleEntity> found = roles.findAllById(ids);
        if (found.size() != ids.size() || found.stream().anyMatch(role -> !role.isActive() || !role.getCategory().isActive()))
            throw new BadRequestException("Select active job roles");
        return Set.copyOf(found);
    }

    private Set<SkillEntity> selectedSkills(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        List<SkillEntity> found = skills.findAllById(ids);
        if (found.size() != ids.size() || found.stream().anyMatch(skill -> !skill.isActive()))
            throw new BadRequestException("Select active skills");
        return Set.copyOf(found);
    }

    private void requireOwnerOrModerator(OpportunityEntity entity, Authentication authentication) {
        if (!isOwner(entity, authentication) && !isModerator(authentication))
            throw new ForbiddenException("You cannot edit this opportunity");
    }

    private boolean isOwner(OpportunityEntity entity, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return users.findByEmailIgnoreCase(authentication.getName())
                .map(user -> user.getId().equals(entity.getPublisher().getId())).orElse(false);
    }

    private boolean isModerator(Authentication authentication) {
        return hasRole(authentication, "ROLE_MODERATOR") || canAdminister(authentication);
    }

    private void requireAdministrator(Authentication authentication) {
        if (!canAdminister(authentication))
            throw new ForbiddenException("Only administrators can review submissions here");
    }

    private boolean canAdminister(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_SUPER_ADMIN");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch(role::equals);
    }

    private String actorName(Authentication authentication) {
        return authentication == null ? "system" : authentication.getName();
    }
    private void notify(com.jobradar.auth.user.UserEntity user, String title, String message, NotificationType type, Integer id) {
        if (notifications != null) notifications.create(user, title, message, type, id);
    }

    private void notifyAdministrators(OpportunityEntity opportunity, String title) {
        if (notifications == null) return;
        for (UserEntity administrator : users.findAdministrators()) {
            notifications.createReviewUpdate(administrator, title, opportunity.getTitle(),
                    NotificationType.OPPORTUNITY_REVIEW_REQUESTED, opportunity.getId());
        }
    }

    private boolean ownerTransitionAllowed(OpportunityStatus from, OpportunityStatus to) {
        return from == OpportunityStatus.DRAFT && (to == OpportunityStatus.PENDING || to == OpportunityStatus.REMOVED)
                || from == OpportunityStatus.PENDING && to == OpportunityStatus.REMOVED
                || from == OpportunityStatus.NEEDS_DETAILS && to == OpportunityStatus.REMOVED
                || from == OpportunityStatus.REJECTED && (to == OpportunityStatus.PENDING || to == OpportunityStatus.REMOVED)
                || from == OpportunityStatus.ACTIVE && (to == OpportunityStatus.PAUSED || to == OpportunityStatus.FILLED || to == OpportunityStatus.REMOVED)
                || from == OpportunityStatus.PAUSED && (to == OpportunityStatus.FILLED || to == OpportunityStatus.REMOVED);
    }

    private boolean moderatorTransitionAllowed(OpportunityStatus from, OpportunityStatus to) {
        if (to == OpportunityStatus.REMOVED && from != OpportunityStatus.REMOVED) return true;
        return from == OpportunityStatus.DRAFT && to == OpportunityStatus.PENDING
                || from == OpportunityStatus.PENDING && (to == OpportunityStatus.ACTIVE || to == OpportunityStatus.REJECTED)
                || from == OpportunityStatus.ACTIVE && (to == OpportunityStatus.PAUSED || to == OpportunityStatus.FILLED || to == OpportunityStatus.EXPIRED)
                || from == OpportunityStatus.PAUSED && (to == OpportunityStatus.ACTIVE || to == OpportunityStatus.FILLED || to == OpportunityStatus.EXPIRED)
                || from == OpportunityStatus.REJECTED && to == OpportunityStatus.PENDING;
    }

    private void notFound() { throw new ResourceNotFoundException("Opportunity not found"); }

    private OpportunitySearchCriteria emptyCriteria() {
        return new OpportunitySearchCriteria(null, null, null, List.of(), null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    private Instant postedAfter(String postedWithin) {
        if (postedWithin == null || postedWithin.isBlank()) return null;
        String value = postedWithin.trim().toLowerCase(Locale.ROOT);
        Duration duration;
        try {
            if (value.matches("[0-9]+h")) duration = Duration.ofHours(Long.parseLong(value.substring(0, value.length() - 1)));
            else if (value.matches("[0-9]+d")) duration = Duration.ofDays(Long.parseLong(value.substring(0, value.length() - 1)));
            else duration = Duration.parse(postedWithin.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new BadRequestException("postedWithin must be a positive duration such as 24h, 7d, or P7D");
        }
        if (duration.compareTo(Duration.ofHours(1)) < 0 || duration.compareTo(Duration.ofDays(3650)) > 0)
            throw new BadRequestException("postedWithin must be between one hour and ten years");
        return Instant.now().minus(duration);
    }

    private void validateRequest(OpportunityRequest request) {
        validateRanges(request.minExperience(), request.maxExperience(), request.minSalary(), request.maxSalary());
        if (!request.isRangesValid()) throw new BadRequestException("Opportunity ranges or walk-in details are invalid");
    }

    private void validateRanges(Integer minExperience, Integer maxExperience, BigDecimal minSalary, BigDecimal maxSalary) {
        if (minExperience != null && maxExperience != null && minExperience > maxExperience)
            throw new BadRequestException("Minimum experience cannot exceed maximum experience");
        if (minSalary != null && maxSalary != null && minSalary.compareTo(maxSalary) > 0)
            throw new BadRequestException("Minimum salary cannot exceed maximum salary");
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
