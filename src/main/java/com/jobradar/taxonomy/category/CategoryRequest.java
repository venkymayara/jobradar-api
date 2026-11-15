package com.jobradar.taxonomy.category;
import jakarta.validation.constraints.*;
public record CategoryRequest(@NotBlank @Size(max=120) String categoryName,@Size(max=140) String slug,Integer parentCategoryId,@Size(max=500) String description,@Size(max=80) String icon) {}
