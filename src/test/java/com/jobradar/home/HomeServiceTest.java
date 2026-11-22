package com.jobradar.home;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jobradar.opportunity.OpportunityDto;
import com.jobradar.opportunity.OpportunityService;
import com.jobradar.opportunity.OpportunityStatus;
import com.jobradar.opportunity.WalkInDto;
import com.jobradar.opportunity.WalkInService;
import com.jobradar.taxonomy.category.CategoryRepository;
import com.jobradar.taxonomy.category.HomepageCategoryProjection;
import com.jobradar.taxonomy.skill.HomepageSkillProjection;
import com.jobradar.taxonomy.skill.SkillRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class HomeServiceTest {
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final SkillRepository skills = mock(SkillRepository.class);
    private final OpportunityService opportunities = mock(OpportunityService.class);
    private final WalkInService walkIns = mock(WalkInService.class);
    private final Instant now = Instant.parse("2026-11-22T12:00:00Z");
    private final LocalDate today = LocalDate.of(2026, 11, 22);
    private final HomeService service = new HomeService(categories, skills, opportunities, walkIns,
            Clock.fixed(now, ZoneOffset.UTC));

    @Test void composesBoundedPublicHomepageSectionsWithDynamicCounts() {
        HomepageCategoryProjection category = mock(HomepageCategoryProjection.class);
        when(category.getCategoryId()).thenReturn(2);
        when(category.getCategoryName()).thenReturn("Technology");
        when(category.getSlug()).thenReturn("technology");
        when(category.getOpportunityCount()).thenReturn(17L);
        when(categories.findHomepageCategories(eq("ACTIVE"), eq(today), eq(now), any(Pageable.class)))
                .thenReturn(List.of(category));

        HomepageSkillProjection skill = mock(HomepageSkillProjection.class);
        when(skill.getSkillId()).thenReturn(4);
        when(skill.getSkillName()).thenReturn("Java");
        when(skill.getSlug()).thenReturn("java");
        when(skill.getOpportunityCount()).thenReturn(8L);
        when(skills.findHomepagePopular(eq("ACTIVE"), eq(today), eq(now), any(Pageable.class)))
                .thenReturn(List.of(skill));

        OpportunityDto opportunity = mock(OpportunityDto.class);
        WalkInDto walkIn = mock(WalkInDto.class);
        when(opportunities.latest(today, now)).thenReturn(List.of(opportunity));
        when(walkIns.upcomingForHomepage(today, now)).thenReturn(List.of(walkIn));

        HomePageDto response = service.getHomepage();

        assertEquals(17, response.categories().getFirst().opportunityCount());
        assertEquals(8, response.popularSkills().getFirst().opportunityCount());
        assertSame(opportunity, response.latestOpportunities().getFirst());
        assertSame(walkIn, response.upcomingWalkIns().getFirst());
        verify(categories).findHomepageCategories(eq(OpportunityStatus.ACTIVE.name()), eq(today), eq(now),
                argThat(page -> page.getPageSize() == 10));
        verify(skills).findHomepagePopular(eq(OpportunityStatus.ACTIVE.name()), eq(today), eq(now),
                argThat(page -> page.getPageSize() == 10));
        verify(opportunities).latest(today, now);
        verify(walkIns).upcomingForHomepage(today, now);
        verifyNoInteractionsExceptDependencies();
    }

    private void verifyNoInteractionsExceptDependencies() {
        verifyNoMoreInteractions(categories, skills, opportunities, walkIns);
    }
}
