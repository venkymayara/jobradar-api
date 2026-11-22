package com.jobradar.externaljobs;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class ExternalJobRoleMatcher {
    private static final List<Rule> RULES = List.of(
            rule("testing", "qa|quality assurance|test automation|tester|sdet|selenium|playwright|cypress"),
            rule("full-stack-development", "full stack|fullstack|mern|mean stack"),
            rule("cloud-and-devops", "devops|site reliability|cloud engineer|cloud architect|kubernetes|terraform|ci cd"),
            rule("cybersecurity", "cybersecurity|cyber security|security analyst|penetration testing|incident response|siem"),
            rule("data-engineering", "data engineer|data engineering|etl|data pipeline|apache spark|airflow|dbt"),
            rule("database", "database administrator|database administration|dba|database tuning"),
            rule("mobile-development", "mobile developer|mobile development|android|ios|swiftui|flutter|react native"),
            rule("frontend-development", "front end|frontend|web ui|angular|react|vue"),
            rule("backend-development", "back end|backend|java developer|python developer|spring boot|django|fastapi|node js|asp net"));

    private ExternalJobRoleMatcher() {}

    static String category(String title, List<String> skills, String summary, boolean technologyFallback) {
        String match = match(title);
        if (match == null) match = match(String.join(" ", skills));
        if (match == null) match = match(summary);
        return match == null && technologyFallback ? "technology" : match;
    }

    private static String match(String text) {
        if (text == null) return null;
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ");
        return RULES.stream().filter(rule -> rule.terms().matcher(normalized).find())
                .map(Rule::category).findFirst().orElse(null);
    }

    private static Rule rule(String category, String terms) {
        return new Rule(category, Pattern.compile("\\b(?:" + terms + ")\\b"));
    }

    private record Rule(String category, Pattern terms) {}
}
