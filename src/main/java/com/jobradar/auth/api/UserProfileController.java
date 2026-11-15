package com.jobradar.auth.api;

import java.util.List;

import com.jobradar.common.api.ApiResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(JwtAuthenticationToken authentication) {
        var jwt = authentication.getToken();
        Number id = jwt.getClaim("userId");
        String displayName = jwt.getClaimAsString("fullName");
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList();
        return ApiResponse.success("Profile retrieved successfully",
                new UserResponse(id.intValue(), displayName == null ? "" : displayName,
                        jwt.getSubject(), roles));
    }
}
