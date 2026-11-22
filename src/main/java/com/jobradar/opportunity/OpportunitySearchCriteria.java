package com.jobradar.opportunity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

record OpportunitySearchCriteria(String keyword, String role, Integer categoryId, List<String> skills, String company,
        String location, Integer minExperience, Integer maxExperience, WorkMode workMode,
        EmploymentType employmentType, OpportunityType opportunityType, BigDecimal minSalary,
        BigDecimal maxSalary, Instant postedAfter, Instant driveStartsAfter,
        Instant driveStartsBefore, Instant driveEndsAfter) {}
