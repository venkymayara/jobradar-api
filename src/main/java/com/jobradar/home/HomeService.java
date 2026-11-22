package com.jobradar.home;

import com.jobradar.opportunity.OpportunityService;
import com.jobradar.opportunity.OpportunityStatus;
import com.jobradar.opportunity.WalkInService;
import com.jobradar.taxonomy.category.CategoryRepository;
import com.jobradar.taxonomy.category.HomepageCategoryDto;
import com.jobradar.taxonomy.skill.SkillRepository;
import com.jobradar.taxonomy.skill.HomepageSkillDto;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HomeService {
    private static final int CATEGORY_LIMIT = 10;
    private static final int POPULAR_SKILL_LIMIT = 10;

    private final CategoryRepository categories;
    private final SkillRepository skills;
    private final OpportunityService opportunities;
    private final WalkInService walkIns;
    private final Clock clock;

    public HomeService(CategoryRepository categories, SkillRepository skills,
            OpportunityService opportunities, WalkInService walkIns, Clock clock) {
        this.categories = categories;
        this.skills = skills;
        this.opportunities = opportunities;
        this.walkIns = walkIns;
        this.clock = clock;
    }

    public HomePageDto getHomepage() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, clock.getZone());
        String status = OpportunityStatus.ACTIVE.name();
        return new HomePageDto(
                categories.findHomepageCategories(status, today, now, PageRequest.of(0, CATEGORY_LIMIT))
                        .stream().map(category -> new HomepageCategoryDto(category.getCategoryId(), category.getCategoryName(),
                                category.getParentCategoryId(), category.getSlug(), category.getDescription(), category.getIcon(),
                                category.getOpportunityCount() == null ? 0 : category.getOpportunityCount())).toList(),
                opportunities.latest(today, now),
                walkIns.upcomingForHomepage(today, now),
                skills.findHomepagePopular(status, today, now, PageRequest.of(0, POPULAR_SKILL_LIMIT))
                        .stream().map(skill -> new HomepageSkillDto(skill.getSkillId(), skill.getSkillName(), skill.getSlug(),
                                skill.getOpportunityCount() == null ? 0 : skill.getOpportunityCount())).toList());
    }
}
