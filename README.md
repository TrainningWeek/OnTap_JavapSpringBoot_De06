# ĐỀ THI TỰ LUẬN SỐ 6 – JWT Authentication Filter

**Thời gian làm bài:** 25 phút

---

## 📄 Đề bài

Hệ thống sử dụng **JWT** để xác thực API. Đoạn code dưới đây là filter kiểm tra token:

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                String username = jwtUtil.extractUsername(token);
                // Không kiểm tra token hết hạn hay signature
                // Đặt Authentication vào context
                SecurityContextHolder.getContext().setAuthentication(...);
            } catch (Exception e) {
                // Bỏ qua lỗi, vẫn cho request đi tiếp
            }
        }
        chain.doFilter(request, response);
    }
}
```

---

## Câu 1 (5 điểm): Lỗ hổng bảo mật trong filter

### 🔴 Lỗ hổng 1: Không kiểm tra chữ ký (Signature) và thời hạn (Expiration) của token

**Vị trí trong code:**
```java
String username = jwtUtil.extractUsername(token);
// Không kiểm tra token hết hạn hay signature
SecurityContextHolder.getContext().setAuthentication(...);
```

**Cách khai thác:**
- Kẻ tấn công có thể **giả mạo payload** của JWT (thay đổi `username`, `role`) rồi ký lại bằng key tùy ý hoặc đặt `alg: none`.
- Kẻ tấn công có thể dùng **token đã hết hạn** (stolen token) để tiếp tục truy cập hệ thống mà không bị từ chối.
- Tấn công **`alg=none` bypass**: Một số thư viện JWT cũ chấp nhận token không có chữ ký nếu không được cấu hình đúng.

**Hậu quả:**
- Chiếm quyền truy cập của bất kỳ user nào kể cả admin.
- Token bị đánh cắp có thể dùng mãi mãi không hết hạn.
- Leo thang đặc quyền (Privilege Escalation).

---

### 🔴 Lỗ hổng 2: Nuốt ngoại lệ (Swallowing Exception) – vẫn cho request đi tiếp

**Vị trí trong code:**
```java
} catch (Exception e) {
    // Bỏ qua lỗi, vẫn cho request đi tiếp
}
chain.doFilter(request, response); // Luôn luôn được gọi!
```

**Cách khai thác:**
- Khi token **không hợp lệ / bị giả mạo / hết hạn**, exception bị bắt và bỏ qua.
- `chain.doFilter()` vẫn được gọi → request đi tiếp đến controller.
- Nếu controller không tự kiểm tra `SecurityContext`, request **không có Authentication** nhưng vẫn được xử lý.
- Kẻ tấn công có thể gửi token rác/sai để bypass filter hoàn toàn.

**Hậu quả:**
- Bypass xác thực hoàn toàn.
- Hệ thống có thể lộ dữ liệu nhạy cảm cho người không được phép.

---

### 🔴 Lỗ hổng 3: Không kiểm tra trạng thái User từ Database

**Vị trí trong code:**
```java
// Chỉ extract username từ token, không load UserDetails từ DB
SecurityContextHolder.getContext().setAuthentication(...);
```

**Cách khai thác & hậu quả:**
- Nếu tài khoản đã bị **vô hiệu hóa / xóa / khóa** trong DB nhưng token chưa hết hạn, user vẫn truy cập được.
- Không có cơ chế thu hồi token (Token Revocation).
- Admin không thể kick user ra khỏi hệ thống ngay lập tức.

---

## Câu 2 (3 điểm): Đề xuất cải tiến cụ thể

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

---

## Câu 3 (2 điểm): Luồng cấp lại Access Token dùng Refresh Token

### Sơ đồ luồng

```
Client                              Server
  |                                   |
  |-- POST /api/auth/login ---------->|
  |                                   | Xác thực thành công
  |<-- access_token  (15 phút)  ------|
  |    refresh_token (7 ngày)         | Lưu HASH của refresh_token vào DB
  |                                   |
  |-- [Gọi API bình thường] -------->|
  |<-- 200 OK ------------------------|
  |                                   |
  |   [Access token hết hạn]          |
  |-- API call ---------------------->|
  |<-- 401 Token has expired ---------|
  |                                   |
  |-- POST /api/auth/refresh -------->| Body: { "refreshToken": "..." }
  |                                   | B1: Tìm hash(token) trong DB
  |                                   | B2: Kiểm tra còn hạn không
  |                                   | B3: Xóa token cũ (Rotation)
  |                                   | B4: Cấp access_token + refresh_token mới
  |<-- new access_token + new refresh |
  |                                   |
  |-- Retry API call ---------------->|
  |<-- 200 OK ------------------------|
  |                                   |
  |-- POST /api/auth/logout -------->|
  |                                   | Xóa tất cả refresh_token của user khỏi DB
  |<-- 200 Logged out ----------------|
```

### Các bước thiết kế chi tiết

**Bước 1 – Khi đăng nhập:**
- Cấp `access_token` thời hạn ngắn (15–30 phút), ký bằng HMAC-SHA256.
- Cấp `refresh_token` là UUID ngẫu nhiên, thời hạn dài (7–30 ngày).
- **Lưu `SHA-256(refresh_token)`** vào DB (không lưu raw để bảo mật).

**Bước 2 – Endpoint `POST /api/auth/refresh`:**
```java
public TokenResponse refresh(RefreshRequest request) {
    String tokenHash = sha256(request.getRefreshToken());

    // B1: Tìm trong DB
    RefreshToken stored = refreshTokenRepo.findByTokenHash(tokenHash)
        .orElseThrow(() -> new RuntimeException("Token không hợp lệ hoặc đã bị thu hồi"));

    // B2: Kiểm tra hết hạn
    if (stored.isExpired()) {
        refreshTokenRepo.delete(stored);
        throw new RuntimeException("Refresh token hết hạn, vui lòng đăng nhập lại");
    }

    // B3: Xóa token cũ (Refresh Token Rotation – chống replay attack)
    refreshTokenRepo.delete(stored);

    // B4: Cấp token mới
    String newAccessToken  = jwtUtil.generateAccessToken(stored.getUser());
    String newRefreshToken = createAndSaveRefreshToken(stored.getUser());

    return new TokenResponse(newAccessToken, newRefreshToken);
}
```

**Bước 3 – Các cơ chế bảo mật bổ sung:**

| Cơ chế | Mô tả |
|---|---|
| **Refresh Token Rotation** | Mỗi lần refresh → cấp token mới, vô hiệu token cũ → chống replay attack |
| **Token Revocation** | Logout → xóa tất cả refresh_token của user khỏi DB ngay lập tức |
| **Lưu Hash** | Chỉ lưu `SHA-256(token)` trong DB, không lưu raw token |
| **HttpOnly Cookie** | Gửi refresh_token qua `HttpOnly Cookie` thay vì body để chống XSS |
| **Cleanup Job** | Định kỳ xóa các refresh_token đã hết hạn khỏi DB |

---

## 🚀 Hướng dẫn chạy project

### Yêu cầu
- Java 17+
- Maven

### Chạy ứng dụng

```bash
cd JavaSpring
./mvnw spring-boot:run
```

Server: `http://localhost:8080`
H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:jwtdb`, username: `sa`)

### User mặc định (seed data)

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ROLE_ADMIN |
| `user` | `user123` | ROLE_USER |

### Danh sách API

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/api/auth/register` | Đăng ký tài khoản | Public |
| POST | `/api/auth/login` | Đăng nhập | Public |
| POST | `/api/auth/refresh` | Cấp lại access token | Public |
| POST | `/api/auth/logout` | Đăng xuất | Bearer Token |
| GET | `/api/auth/me` | Thông tin user hiện tại | Bearer Token |
| GET | `/api/public` | Endpoint công khai | Public |
| GET | `/api/user` | Endpoint cần đăng nhập | Bearer Token |
| GET | `/api/admin` | Chỉ dành cho Admin | Bearer Token (ADMIN) |

### Ví dụ sử dụng

**1. Đăng nhập:**
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**2. Gọi API có bảo vệ:**
```http
GET /api/user
Authorization: Bearer <access_token>
```

**3. Refresh khi token hết hạn:**
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<refresh_token>"
}
```

**4. Logout:**
```http
POST /api/auth/logout
Authorization: Bearer <access_token>
```
