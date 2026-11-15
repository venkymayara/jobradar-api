package com.jobradar.taxonomy.category;
import java.time.Instant;
public record CategoryDto(Integer categoryId,String categoryName,Integer parentCategoryId,String slug,String description,String icon,int opportunityCount,boolean active,Instant createdAt,Instant updatedAt) { static CategoryDto from(CategoryEntity e){return new CategoryDto(e.getId(),e.getName(),e.getParent()==null?null:e.getParent().getId(),e.getSlug(),e.getDescription(),e.getIcon(),e.getOpportunityCount(),e.isActive(),e.getCreatedAt(),e.getUpdatedAt());} }
