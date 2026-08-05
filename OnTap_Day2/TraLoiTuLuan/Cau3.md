# Câu 3 (2 điểm): Khái niệm "Deduplication" trong TanStack Query

---

## 1. Ý nghĩa của "Deduplication" (Gộp yêu cầu trùng lặp)

**Deduplication** (Request Deduplication) trong TanStack Query là cơ chế tự động phát hiện và gộp nhiều request API giống nhau (có cùng `queryKey` và tham số) phát ra tại cùng một thời điểm thành **DUY NHẤT 1 request HTTP thực tế** gửi tới Server.

---

## 2. Kịch bản minh họa hoạt động

Giả sử trong giao diện Dashboard của bạn có 3 component độc lập được render cùng lúc trên màn hình:
1. `NavbarUserAvatar` – Cần lấy thông tin user hiện tại (`/api/users/me`).
2. `SidebarUserProfile` – Cần lấy thông tin user hiện tại (`/api/users/me`).
3. `MainContentHeader` – Cần lấy thông tin user hiện tại (`/api/users/me`).

 Cả 3 component đều gọi:
```javascript
useQuery({ queryKey: ['currentUser'], queryFn: fetchCurrentUser });
```

### ⚙️ Cách TanStack Query xử lý Deduplication:
1. **Component thứ nhất** render và yêu cầu dữ liệu `['currentUser']`. TanStack Query khởi tạo một promise fetch API (In-flight request) và lưu promise này lại.
2. **Component thứ hai & thứ ba** render ngay sau đó (trong cùng chu kỳ render). TanStack Query nhận diện được rằng query `['currentUser']` đang có một request HTTP đang chạy (`in-flight`).
3. **Gộp request:** TanStack Query **KHÔNG** gửi thêm 2 request HTTP mới. Nó bắt 2 component này lắng nghe chung kết quả từ request HTTP duy nhất đang chạy đó.
4. **Trả kết quả:** Khi Server phản hồi, dữ liệu được phân phối đồng thời cho cả 3 component.

---

## 3. Lợi ích giảm tải cho Server và Network

### 🌐 Về phía Network (Client / Browser):
- **Tránh nghẽn băng thông (Network Congestion):** Giảm thiểu số lượng kết nối HTTP/HTTPS phải mở song song trên trình duyệt.
- **Tiết kiệm tài nguyên thiết bị:** Giảm bớt số lượng công việc mở/đóng socket, serialize/deserialize JSON trên máy client.

### 🖥️ Về phía Server (Backend & Database):
- **Giảm số lượng Request:** Giảm từ $N$ request trùng lặp xuống còn $1$ request ($N \rightarrow 1$). Trong ứng dụng lớn có hàng trăm component, tính năng này tiết kiệm hàng nghìn request mỗi phút.
- **Giảm tải cho Database:** Tránh việc Server phải thực thi cùng một câu lệnh SQL (`SELECT * FROM users WHERE...`) nhiều lần liên tiếp trong cùng một mili-giây.
- **Phòng chống nghẽn Server:** Giúp hệ thống hoạt động ổn định, tránh nguy cơ sập Server do hiện tượng "thỉnh cầu thảm họa" (Thundering Herd Problem hoặc self-inflicted DDoS) khi một trang phức tạp được tải.
