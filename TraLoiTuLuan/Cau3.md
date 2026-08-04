# Câu 3 (2 điểm): Luồng cấp lại Access Token dùng Refresh Token

> **Yêu cầu:** Khi token hết hạn, bạn sẽ thiết kế luồng cấp lại access token dùng refresh token như thế nào? Mô tả vắn tắt.

---

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
