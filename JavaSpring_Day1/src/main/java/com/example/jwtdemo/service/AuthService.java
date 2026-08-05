package com.example.jwtdemo.service;

import com.example.jwtdemo.dto.*;
import com.example.jwtdemo.entity.RefreshToken;
import com.example.jwtdemo.entity.User;
import com.example.jwtdemo.repository.RefreshTokenRepository;
import com.example.jwtdemo.repository.UserRepository;
import com.example.jwtdemo.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service xử lý logic xác thực:
 * - Đăng nhập → cấp access_token + refresh_token
 * - Refresh → cấp access_token mới (với Refresh Token Rotation)
 * - Logout → xóa refresh_token khỏi DB
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       CustomUserDetailsService userDetailsService,
                       RefreshTokenRepository refreshTokenRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    /**
     * Xác thực user và cấp access_token + refresh_token.
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        try {
            // Spring Security xác thực username/password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User không tìm thấy"));

        // Tạo access token
        String accessToken = jwtUtil.generateAccessToken(userDetails);

        // Tạo refresh token và lưu vào DB
        String rawRefreshToken = createAndSaveRefreshToken(user);

        log.info("User '{}' đăng nhập thành công.", request.getUsername());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────

    /**
     * Cấp access_token mới từ refresh_token hợp lệ.
     * Áp dụng Refresh Token Rotation: cấp refresh_token mới, vô hiệu hóa token cũ.
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String rawToken = request.getRefreshToken();
        String tokenHash = hashToken(rawToken);

        // Bước 1: Tìm token trong DB
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ hoặc đã bị thu hồi"));

        // Bước 2: Kiểm tra hết hạn
        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken); // Xóa token hết hạn
            log.warn("Refresh token hết hạn cho user '{}'.", storedToken.getUser().getUsername());
            throw new RuntimeException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        User user = storedToken.getUser();

        // Bước 3: Xóa token cũ (Refresh Token Rotation – chống replay attack)
        refreshTokenRepository.delete(storedToken);

        // Bước 4: Tạo access token mới
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        // Bước 5: Tạo refresh token mới
        String newRawRefreshToken = createAndSaveRefreshToken(user);

        log.info("Cấp lại token thành công cho user '{}'.", user.getUsername());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    /**
     * Logout: xóa toàn bộ refresh token của user khỏi DB.
     */
    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tìm thấy"));
        refreshTokenRepository.deleteAllByUser(user);
        log.info("User '{}' đã logout, tất cả refresh token bị thu hồi.", username);
    }

    // ─── Register ─────────────────────────────────────────────────────────────

    /**
     * Đăng ký user mới.
     */
    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole("ROLE_USER");
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        userRepository.save(user);

        log.info("Đăng ký user mới: '{}'", request.getUsername());
        return "Đăng ký thành công!";
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Tạo raw refresh token, hash và lưu vào DB.
     *
     * @return raw refresh token (gửi về client)
     */
    private String createAndSaveRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiration))
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Hash token bằng SHA-256 để không lưu raw token trong DB.
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi hash token", e);
        }
    }
}
