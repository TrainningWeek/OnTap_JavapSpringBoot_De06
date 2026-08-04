package com.example.jwtdemo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity lưu Refresh Token trong database.
 * Khi logout hoặc hết hạn, token sẽ bị xóa khỏi DB → thu hồi token (revocation).
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Lưu hash của refresh token (không lưu raw token để bảo mật).
     */
    @Column(nullable = false, unique = true)
    private String tokenHash;

    /**
     * Liên kết với user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Thời điểm hết hạn của refresh token.
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * Thời điểm tạo token.
     */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * Kiểm tra token có hết hạn chưa.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
