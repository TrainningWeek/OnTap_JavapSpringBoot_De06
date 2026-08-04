package com.example.jwtdemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller demo các endpoint được bảo vệ bởi JWT.
 * Dùng để test xác thực và phân quyền.
 */
@RestController
@RequestMapping("/api")
public class DemoController {

    /**
     * Endpoint công khai – không cần token.
     * GET /api/public
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "Đây là endpoint công khai, không cần token!",
                "status", "OK"
        ));
    }

    /**
     * Endpoint yêu cầu đăng nhập (bất kỳ role nào).
     * GET /api/user
     * Header: Authorization: Bearer <access_token>
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userEndpoint(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "message", "Xin chào " + userDetails.getUsername() + "! Bạn đã xác thực thành công.",
                "roles", userDetails.getAuthorities().toString()
        ));
    }

    /**
     * Endpoint chỉ dành cho ADMIN.
     * GET /api/admin
     * Header: Authorization: Bearer <access_token> (token của admin)
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "Chào Admin! Bạn có quyền truy cập trang quản trị.",
                "data", "Dữ liệu nhạy cảm chỉ admin mới thấy."
        ));
    }
}
