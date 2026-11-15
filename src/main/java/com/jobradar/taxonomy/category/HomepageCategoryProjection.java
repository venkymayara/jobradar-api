package com.jobradar.taxonomy.category;

public interface HomepageCategoryProjection {
    Integer getCategoryId();
    String getCategoryName();
    Integer getParentCategoryId();
    String getSlug();
    String getDescription();
    String getIcon();
    Long getOpportunityCount();
}
