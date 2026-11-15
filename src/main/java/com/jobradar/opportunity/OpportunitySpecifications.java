package com.jobradar.opportunity;

import com.jobradar.taxonomy.skill.SkillEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class OpportunitySpecifications {
    private OpportunitySpecifications() {}

    static Specification<OpportunityEntity> publicSearch(OpportunitySearchCriteria filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), OpportunityStatus.ACTIVE));
            if (filters.keyword() != null) {
                String like = contains(filters.keyword());
                Join<Object, Object> company = root.join("company");
                Join<Object, Object> location = root.join("location");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(company.get("companyName")), like),
                        cb.like(cb.lower(location.get("city")), like),
                        cb.like(cb.lower(location.get("state")), like),
                        cb.like(cb.lower(location.get("country")), like)));
            }
            if (filters.role() != null) {
                Join<Object, Object> role = root.join("roles");
                String like = contains(filters.role());
                predicates.add(cb.or(cb.like(cb.lower(role.get("roleName")), like),
                        cb.equal(cb.lower(role.get("slug")), filters.role().toLowerCase(Locale.ROOT))));
                query.distinct(true);
            }
            for (String requestedSkill : filters.skills()) {
                String name = requestedSkill.trim().toLowerCase(Locale.ROOT);
                Subquery<Long> skillMatch = query.subquery(Long.class);
                Root<OpportunityEntity> correlated = skillMatch.correlate(root);
                Join<OpportunityEntity, SkillEntity> skills = correlated.join("skills");
                skillMatch.select(cb.countDistinct(skills.get("id")))
                        .where(cb.or(cb.equal(cb.lower(skills.get("name")), name),
                                cb.equal(cb.lower(skills.get("slug")), name)));
                predicates.add(cb.greaterThanOrEqualTo(skillMatch, 1L));
            }
            if (filters.company() != null)
                predicates.add(cb.like(cb.lower(root.join("company").get("companyName")), contains(filters.company())));
            if (filters.location() != null) {
                Join<Object, Object> location = root.join("location");
                String like = contains(filters.location());
                predicates.add(cb.or(cb.like(cb.lower(location.get("city")), like),
                        cb.like(cb.lower(location.get("state")), like),
                        cb.like(cb.lower(location.get("country")), like)));
            }
            if (filters.minExperience() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxExperience"), filters.minExperience()));
            if (filters.maxExperience() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("minExperience"), filters.maxExperience()));
            if (filters.workMode() != null) predicates.add(cb.equal(root.get("workMode"), filters.workMode()));
            if (filters.employmentType() != null) predicates.add(cb.equal(root.get("employmentType"), filters.employmentType()));
            if (filters.opportunityType() != null) predicates.add(cb.equal(root.get("opportunityType"), filters.opportunityType()));
            if (filters.opportunityType() == OpportunityType.WALK_IN || filters.driveStartsAfter() != null
                    || filters.driveStartsBefore() != null || filters.driveEndsAfter() != null) {
                Join<Object, Object> drive = root.join("walkInDetails");
                if (filters.driveStartsAfter() != null)
                    predicates.add(cb.greaterThan(drive.get("driveStartAtUtc"), filters.driveStartsAfter()));
                if (filters.driveStartsBefore() != null)
                    predicates.add(cb.lessThan(drive.get("driveStartAtUtc"), filters.driveStartsBefore()));
                if (filters.driveEndsAfter() != null)
                    predicates.add(cb.greaterThan(drive.get("driveEndAtUtc"), filters.driveEndsAfter()));
            }
            if (filters.minSalary() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxSalary"), filters.minSalary()));
            if (filters.maxSalary() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("minSalary"), filters.maxSalary()));
            if (filters.postedAfter() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishedAt"), filters.postedAfter()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String value) { return "%" + value.trim().toLowerCase(Locale.ROOT) + "%"; }
}
