package com.jobradar.auth.api;

import java.util.List;

import com.jobradar.auth.user.UserEntity;

public record UserResponse(int userId, String displayName, String email, String phoneNumber, String location, String professionalHeadline, String skills, Integer experienceYears, String linkedinUrl, String githubUrl, String avatarUrl, List<String> roles) {
    public static UserResponse from(UserEntity user) {
        String displayName = user.getFullName() == null ? "" : user.getFullName();
        return new UserResponse(user.getId(), displayName, user.getEmail(), user.getPhoneNumber(), user.getLocation(), user.getProfessionalHeadline(), user.getSkills(), user.getExperienceYears(), user.getLinkedinUrl(), user.getGithubUrl(), user.getAvatarUrl(),
                user.getRoles().stream().map(role -> role.getRole()).sorted().toList());
    }
}
