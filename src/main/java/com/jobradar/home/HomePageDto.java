package com.jobradar.home;

import com.jobradar.opportunity.OpportunityDto;
import com.jobradar.opportunity.WalkInDto;
import com.jobradar.taxonomy.category.HomepageCategoryDto;
import com.jobradar.taxonomy.skill.HomepageSkillDto;
import java.util.List;

public record HomePageDto(List<HomepageCategoryDto> categories,
        List<OpportunityDto> latestOpportunities, List<WalkInDto> upcomingWalkIns,
        List<HomepageSkillDto> popularSkills) {}
