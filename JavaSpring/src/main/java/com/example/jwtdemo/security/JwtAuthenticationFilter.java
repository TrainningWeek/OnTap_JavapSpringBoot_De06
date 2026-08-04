package com.example.jwtdemo.security;

import com.example.jwtdemo.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter - phiên bản ĐÃ SỬA các lỗ hổng bảo mật.
 *
 * Các cải tiến so với code đề thi:
 * 1. Kiểm tra signature + expiration TRƯỚC khi xử lý token.
 * 2. KHÔNG nuốt exception – trả về HTTP 401 và dừng request.
 * 3. Load UserDetails từ DB để kiểm tra trạng thái account.
 * 4. Xử lý riêng từng loại exception để log rõ ràng.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        final String authHeader = request.getHeader("Authorization");

        // ✅ FIX 1: Kiểm tra header đúng format "Bearer <token>"
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response); // Không có token, tiếp tục (endpoint public có thể không cần token)
            return;
        }

        final String token = authHeader.substring(7);

        try {
            // ✅ FIX 2: Extract username - nếu token sai signature hoặc hết hạn sẽ ném exception ngay
            final String username = jwtUtil.extractUsername(token);

            // Chỉ xử lý nếu chưa có Authentication trong context (tránh xử lý lại)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // ✅ FIX 3: Load UserDetails từ DB để kiểm tra account có còn active không
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // ✅ FIX 4: Validate toàn bộ token (signature + expiration + username khớp)
                jwtUtil.validateToken(token, userDetails);

                // ✅ FIX 5: Kiểm tra trạng thái account trong DB
                if (!userDetails.isEnabled()) {
                    log.warn("Tài khoản '{}' đã bị vô hiệu hóa.", username);
                    sendUnauthorized(response, "Account is disabled");
                    return; // DỪNG, không cho request đi tiếp
                }

                if (!userDetails.isAccountNonLocked()) {
                    log.warn("Tài khoản '{}' đang bị khóa.", username);
                    sendUnauthorized(response, "Account is locked");
                    return; // DỪNG, không cho request đi tiếp
                }

                // Đặt Authentication vào SecurityContext
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Xác thực thành công cho user: '{}'", username);
            }

        } catch (ExpiredJwtException e) {
            // ✅ FIX 6: Xử lý riêng token hết hạn – trả 401, KHÔNG cho request đi tiếp
            log.warn("Token đã hết hạn cho request: {}", request.getRequestURI());
            sendUnauthorized(response, "Token has expired. Please use refresh token.");
            return;

        } catch (SignatureException e) {
            // ✅ FIX 7: Chữ ký sai – có thể là token giả mạo
            log.error("Phát hiện chữ ký JWT không hợp lệ (có thể bị giả mạo): {}", e.getMessage());
            sendUnauthorized(response, "Invalid token signature");
            return;

        } catch (MalformedJwtException e) {
            // ✅ FIX 8: Token không đúng định dạng
            log.error("Token JWT không đúng định dạng: {}", e.getMessage());
            sendUnauthorized(response, "Malformed token");
            return;

        } catch (UsernameNotFoundException e) {
            // ✅ FIX 9: Username trong token không tồn tại trong DB
            log.warn("Username trong token không tồn tại: {}", e.getMessage());
            sendUnauthorized(response, "User not found");
            return;

        } catch (Exception e) {
            // ✅ FIX 10: Lỗi khác – log và trả 500, KHÔNG bỏ qua
            log.error("Lỗi xác thực JWT không xác định: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Helper method gửi response 401 Unauthorized với JSON body.
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"error\": \"Unauthorized\", \"message\": \"" + message + "\"}"
        );
    }
}
