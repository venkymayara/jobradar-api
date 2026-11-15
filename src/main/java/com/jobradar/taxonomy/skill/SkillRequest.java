package com.jobradar.taxonomy.skill;import jakarta.validation.constraints.*;public record SkillRequest(@NotBlank @Size(max=100)String skillName,@Size(max=120)String slug){}
