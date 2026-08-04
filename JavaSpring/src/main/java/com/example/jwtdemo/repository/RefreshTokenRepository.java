package com.example.jwtdemo.repository;

import com.example.jwtdemo.entity.RefreshToken;
import com.example.jwtdemo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Xóa tất cả refresh token của một user (dùng khi logout hoặc đổi mật khẩu).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    void deleteAllByUser(User user);

    /**
     * Xóa các token đã hết hạn (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteAllExpiredBefore(Instant now);
}
