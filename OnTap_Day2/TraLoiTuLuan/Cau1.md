# Câu 1 (5 điểm): Phân biệt Client State & Server State và Khó khăn khi chỉ dùng useState

---

## 1. Phân biệt "Client State" và "Server State"

| Tiêu chí | Client State | Server State |
| :--- | :--- | :--- |
| **Khái niệm** | Là dữ liệu nằm hoàn toàn ở Client (trình duyệt), do giao diện người dùng sở hữu và quản lý. | Là dữ liệu nằm ở Remote Server, Client chỉ sở hữu một bản sao tạm thời (snapshot) tại một thời điểm. |
| **Tính chất** | **Synchronous (Đồng bộ)**, dữ liệu luôn có sẵn ngay lập tức. | **Asynchronous (Bất đồng bộ)**, cần thời gian gọi qua mạng (Network Latency) để lấy dữ liệu. |
| **Quyền sở hữu** | Do duy nhất Client làm chủ và quyết định. | Do Server làm chủ. Có thể bị thay đổi bởi người dùng khác hoặc hệ thống khác mà Client không hề hay biết. |
| **Yêu cầu xử lý** | Đơn giản, không cần quản lý Cache hay Retry. | Phức tạp: cần quản lý Cache, Revalidation, Stale Data, Error Handling, Optimistic Update. |
| **Ví dụ** | Trạng thái bật/tắt Modal, Tab đang chọn, Theme (Dark/Light mode), giá trị ô input. | Danh sách người dùng (`/api/users`), chi tiết sản phẩm, giỏ hàng, thông tin tài khoản. |

---

## 2. Tại sao chỉ dùng `useState` + `useEffect` cho Server State lại gây khó khăn?

Đoạn code ban đầu:
```javascript
useEffect(() => {
  const load = async () => {
    const res = await fetch("/api/users");
    setUsers(await res.json());
  };
  load();
}, []);
```

### 🔴 Khó khăn 1: Khi quay lại trang (Back / Navigation)
- **Vấn đề:** `useState` gắn liền với vòng đời (lifecycle) của Component. Khi người dùng chuyển sang trang khác, Component bị unmount và toàn bộ state bị giải phóng khỏi RAM.
- **Hậu quả:** Khi nhấn nút Back hoặc quay lại trang cũ, Component mount lại $\rightarrow$ `useEffect` kích hoạt lại $\rightarrow$ **phải fetch API từ đầu**. Người dùng phải chờ màn hình loading xuất hiện lại, gây nhấp nháy UI (Layout Shift) và lãng phí băng thông mạng.

---

### 🔴 Khó khăn 2: Khi nhiều Component cần cùng một dữ liệu (Tính nhất quán)
- **Vấn đề:** Nếu 3-4 component độc lập (ví dụ: `HeaderUserAvatar`, `SidebarUser`, `UserProfile`) đều cần dữ liệu `/api/users`:
  - Mỗi component phải tự viết `useEffect` để fetch API $\rightarrow$ bắn 3-4 HTTP requests trùng lặp lên Server tại cùng một thời điểm.
  - Khi 1 component thực hiện mutation (thêm/sửa/xóa user), 3 component còn lại không hề biết để cập nhật theo $\rightarrow$ **dữ liệu bị bất đồng bộ trên giao diện**.
- **Giải pháp thủ công rất phức tạp:** Phải nâng state lên Component cha (Prop Drilling) hoặc dùng Redux/Context API với rất nhiều boilerplate code để lưu trữ và phân phối dữ liệu.

---

### 🔴 Khó khăn 3: Phải tự viết tay các trạng thái loading/error/cache
- **Boilerplate Code lặp đi lặp lại:** Với mỗi API, lập trình viên phải tự quản lý thủ công hàng loạt state:
  ```javascript
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  ```
- **Thiếu cơ chế an toàn:**
  - Không có xử lý **Race Condition** (khi 2 request gửi liên tiếp, request cũ về sau đè dữ liệu request mới).
  - Phải tự viết `AbortController` để hủy request khi component unmount.
  - Tự viết cơ chế Cache, Retry khi rớt mạng, hoặc dọn dẹp bộ nhớ (Garbage Collection) bằng tay là vô cùng phức tạp và rất dễ phát sinh lỗi.
