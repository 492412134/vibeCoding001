package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.AuthService;
import com.example.common.result.Result;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final long REFRESH_TOKEN_EXPIRY = 7 * 24 * 60 * 60;

    private final AuthService authService;

    @PostMapping("/login")
    public Result<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        TokenResponse tokenResponse = authService.login(request);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return Result.success(tokenResponse);
    }

    @PostMapping("/register")
    public Result<TokenResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        TokenResponse tokenResponse = authService.register(request);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return Result.success(tokenResponse);
    }

    @PostMapping("/refresh")
    public Result<TokenResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            return Result.error(401, "刷新令牌无效");
        }
        TokenResponse tokenResponse = authService.refreshToken(refreshToken);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return Result.success(tokenResponse);
    }

    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request,
            HttpServletResponse response) {
        String accessToken = authHeader.replace("Bearer ", "");
        String refreshToken = extractRefreshToken(request);
        authService.logout(accessToken, refreshToken);
        clearRefreshTokenCookie(response);
        return Result.success();
    }

    @GetMapping("/health")
    public Result<HealthInfo> health() {
        return Result.success(new HealthInfo("UP", "vibe-auth"));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge((int) REFRESH_TOKEN_EXPIRY);
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public record HealthInfo(String status, String service) {}
}
