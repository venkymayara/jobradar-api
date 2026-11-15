package com.jobradar.opportunity;

import java.math.BigDecimal;
import java.util.List;

public record WalkInFilters(String keyword, String location, String role, List<String> skills,
        String company, Integer minExperience, Integer maxExperience,
        BigDecimal minSalary, BigDecimal maxSalary, String date) {}
