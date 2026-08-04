package com.example.jwtdemo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class xử lý JWT:
 * - Tạo access token
 * - Validate token (kiểm tra signature + expiration)
 * - Extract claims từ token
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /**
     * Lấy signing key từ secret hex string.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ─── Generate Token ───────────────────────────────────────────────────────

    /**
     * Tạo access token từ UserDetails.
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Thêm roles vào claims
        claims.put("roles", userDetails.getAuthorities().toString());
        return buildToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ─── Extract Claims ───────────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // Đây sẽ ném exception nếu token sai signature hoặc hết hạn
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ─── Validate Token ───────────────────────────────────────────────────────

    /**
     * Validate đầy đủ: kiểm tra signature + expiration + username khớp UserDetails.
     * Trả về false nếu bất kỳ điều kiện nào không thỏa.
     *
     * @throws ExpiredJwtException   nếu token hết hạn
     * @throws SignatureException    nếu chữ ký sai
     * @throws MalformedJwtException nếu token không đúng format
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token đã hết hạn: {}", e.getMessage());
            throw e; // Ném lại để filter xử lý riêng
        } catch (SignatureException e) {
            log.error("Chữ ký JWT không hợp lệ: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("JWT token không đúng định dạng: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token không được hỗ trợ: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validate chỉ kiểm tra cấu trúc và signature (không cần UserDetails).
     * Dùng trong filter trước khi load UserDetails.
     */
    public boolean validateTokenStructure(String token) {
        try {
            extractAllClaims(token); // Throws nếu sai
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
