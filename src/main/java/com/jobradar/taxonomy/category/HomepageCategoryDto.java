package com.jobradar.taxonomy.category;

public record HomepageCategoryDto(Integer categoryId, String categoryName, Integer parentCategoryId,
        String slug, String description, String icon, long opportunityCount) {}
