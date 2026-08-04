# JWT Authentication Demo – Đề 06

Spring Boot project minh họa JWT authentication đúng chuẩn bảo mật.

## 🚀 Công nghệ sử dụng

- Java 17
- Spring Boot 3.2.5
- Spring Security
- JJWT 0.11.5
- H2 In-Memory Database
- Lombok

## 📁 Cấu trúc project

```
src/main/java/com/example/jwtdemo/
├── config/
│   └── SecurityConfig.java         # Cấu hình Spring Security
├── controller/
│   ├── AuthController.java         # API xác thực (login, refresh, logout)
│   └── DemoController.java         # API demo (public, user, admin)
├── dto/
│   ├── LoginRequest.java
│   ├── RefreshRequest.java
│   ├── RegisterRequest.java
│   └── TokenResponse.java
├── entity/
│   ├── User.java                   # Entity người dùng
│   └── RefreshToken.java           # Entity lưu refresh token (hash)
├── exception/
│   └── GlobalExceptionHandler.java # Xử lý lỗi tập trung
├── repository/
│   ├── UserRepository.java
│   └── RefreshTokenRepository.java
├── security/
│   ├── JwtUtil.java                # Tạo & validate JWT
│   └── JwtAuthenticationFilter.java # Filter xác thực (ĐÃ SỬA lỗ hổng)
└── service/
    ├── AuthService.java            # Logic đăng nhập, refresh, logout
    └── CustomUserDetailsService.java
```

## ▶️ Chạy project

```bash
cd JavaSpring
./mvnw spring-boot:run
```

Server chạy tại: http://localhost:8080

H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:jwtdb`
- Username: `sa`, Password: (để trống)

## 📋 API Endpoints

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/api/auth/register` | Đăng ký | Public |
| POST | `/api/auth/login` | Đăng nhập | Public |
| POST | `/api/auth/refresh` | Cấp lại token | Public |
| POST | `/api/auth/logout` | Đăng xuất | Bearer Token |
| GET | `/api/auth/me` | Thông tin user | Bearer Token |
| GET | `/api/public` | Demo public | Public |
| GET | `/api/user` | Demo user | Bearer Token |
| GET | `/api/admin` | Demo admin | Bearer Token (ADMIN) |

## 🔑 User mặc định (seed data)

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ROLE_ADMIN |
| user | user123 | ROLE_USER |

## 🔒 Các lỗ hổng đã sửa (so với đề thi)

1. ✅ **Validate signature + expiration** trong `JwtUtil.validateToken()`
2. ✅ **Không nuốt exception** – trả về HTTP 401 và dừng request
3. ✅ **Load UserDetails từ DB** – kiểm tra account có bị khóa/xóa không
4. ✅ **Refresh Token Rotation** – chống replay attack
5. ✅ **Hash refresh token** trong DB – không lưu raw token

## 📖 Ví dụ sử dụng

### 1. Đăng nhập
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### 2. Gọi API có bảo vệ
```http
GET /api/user
Authorization: Bearer <access_token>
```

### 3. Refresh token khi hết hạn
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<refresh_token>"
}
```

### 4. Logout
```http
POST /api/auth/logout
Authorization: Bearer <access_token>
```
