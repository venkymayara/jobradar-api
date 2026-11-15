package com.jobradar.auth.api;

import com.jobradar.auth.service.AuthService;
import com.jobradar.auth.service.IssuedAuthSession;
import com.jobradar.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshCookieFactory cookieFactory;

    public AuthController(AuthService authService, RefreshCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account registered successfully", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthSession>> login(@Valid @RequestBody LoginRequest request) {
        return sessionResponse(authService.login(request), "Login successful");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthSession>> refresh(
            HttpServletRequest request) {
        return sessionResponse(authService.refresh(readRefreshToken(request)), "Session refreshed successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request) {
        String refreshToken = readRefreshToken(request);
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .body(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/csrf")
    public ApiResponse<CsrfResponse> csrf(CsrfToken csrfToken) {
        return ApiResponse.success("CSRF token issued", new CsrfResponse(csrfToken.getToken()));
    }

    private ResponseEntity<ApiResponse<AuthSession>> sessionResponse(IssuedAuthSession issued, String message) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.create(issued.refreshToken(), issued.refreshTokenTtl()).toString())
                .body(ApiResponse.success(message, issued.session()));
    }

    private String readRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieFactory.cookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public record CsrfResponse(String token) {
    }
}
