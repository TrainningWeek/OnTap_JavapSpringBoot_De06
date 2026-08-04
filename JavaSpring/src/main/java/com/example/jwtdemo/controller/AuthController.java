package com.example.jwtdemo.controller;

import com.example.jwtdemo.dto.*;
import com.example.jwtdemo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller xử lý các API xác thực:
 * POST /api/auth/register  - Đăng ký
 * POST /api/auth/login     - Đăng nhập → nhận access_token + refresh_token
 * POST /api/auth/refresh   - Cấp lại access_token từ refresh_token
 * POST /api/auth/logout    - Đăng xuất (thu hồi refresh_token)
 * GET  /api/auth/me        - Thông tin user đang đăng nhập (cần Bearer token)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Đăng ký tài khoản mới.
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * Đăng nhập – trả về access_token và refresh_token.
     * POST /api/auth/login
     * Body: { "username": "admin", "password": "admin123" }
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Cấp lại access_token mới bằng refresh_token.
     * POST /api/auth/refresh
     * Body: { "refreshToken": "..." }
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Đăng xuất – thu hồi toàn bộ refresh token của user.
     * POST /api/auth/logout
     * Header: Authorization: Bearer <access_token>
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công. Tất cả phiên làm việc đã bị thu hồi."));
    }

    /**
     * Lấy thông tin user đang đăng nhập.
     * GET /api/auth/me
     * Header: Authorization: Bearer <access_token>
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "username", userDetails.getUsername(),
                "roles", userDetails.getAuthorities().toString(),
                "enabled", userDetails.isEnabled()
        ));
    }
}
