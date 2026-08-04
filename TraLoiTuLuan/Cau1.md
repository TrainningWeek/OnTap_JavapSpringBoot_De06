# Câu 1 (5 điểm): Lỗ hổng bảo mật trong filter

> **Yêu cầu:** Chỉ ra ít nhất HAI lỗ hổng bảo mật hoặc vấn đề trong filter xác thực. Giải thích cách khai thác và hậu quả.

---

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
