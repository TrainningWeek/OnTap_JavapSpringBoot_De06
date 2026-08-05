# Câu 2 (3 điểm): Đề xuất cải tiến cụ thể

> **Yêu cầu:** Đề xuất các cải tiến cụ thể để sửa các lỗ hổng đó (cần đề cập đến validation, exception handling, refresh token, v.v.).

---

### ✅ Code cải tiến hoàn chỉnh

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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
            chain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            // ✅ FIX 2: Extract username – nếu token sai signature/hết hạn sẽ ném exception ngay
            final String username = jwtUtil.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // ✅ FIX 3: Load UserDetails từ DB để kiểm tra account có còn active không
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // ✅ FIX 4: Validate toàn bộ token (signature + expiration + username khớp)
                jwtUtil.validateToken(token, userDetails);

                // ✅ FIX 5: Kiểm tra trạng thái account trong DB
                if (!userDetails.isEnabled()) {
                    sendUnauthorized(response, "Account is disabled");
                    return; // DỪNG, không cho request đi tiếp
                }

                if (!userDetails.isAccountNonLocked()) {
                    sendUnauthorized(response, "Account is locked");
                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (ExpiredJwtException e) {
            // ✅ FIX 6: Xử lý riêng token hết hạn – trả 401, KHÔNG cho request đi tiếp
            sendUnauthorized(response, "Token has expired. Please use refresh token.");
            return;

        } catch (SignatureException e) {
            // ✅ FIX 7: Chữ ký sai – có thể là token giả mạo
            sendUnauthorized(response, "Invalid token signature");
            return;

        } catch (MalformedJwtException e) {
            // ✅ FIX 8: Token không đúng định dạng
            sendUnauthorized(response, "Malformed token");
            return;

        } catch (UsernameNotFoundException e) {
            // ✅ FIX 9: Username trong token không tồn tại trong DB
            sendUnauthorized(response, "User not found");
            return;

        } catch (Exception e) {
            // ✅ FIX 10: Lỗi khác – log và trả 500, KHÔNG bỏ qua
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error");
            return;
        }

        chain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"error\": \"Unauthorized\", \"message\": \"" + message + "\"}"
        );
    }
}
```

### Bảng tóm tắt cải tiến

| # | Vấn đề gốc | Cải tiến |
|---|---|---|
| 1 | Không validate signature | Gọi `jwtUtil.validateToken()` kiểm tra cả signature lẫn expiration |
| 2 | Nuốt exception, cho request qua | Trả về `401 Unauthorized` và `return` ngay lập tức |
| 3 | Không kiểm tra trạng thái user | Load `UserDetails` từ DB, check `isEnabled()` và `isAccountNonLocked()` |
| 4 | Không phân biệt loại lỗi | Catch riêng `ExpiredJwtException`, `SignatureException`, `MalformedJwtException` |
| 5 | Không có Refresh Token | Thiết kế luồng Refresh Token Rotation (xem Câu 3) |
