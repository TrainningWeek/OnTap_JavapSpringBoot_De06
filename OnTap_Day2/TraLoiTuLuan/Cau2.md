# Câu 2 (3 điểm): Các tác vụ TanStack Query (React Query) tự động hóa

---

## 1. Các tác vụ TanStack Query tự động hóa thay vì viết tay bằng `useEffect`

Thay vì phải tự viết `useEffect`, `useState`, `try/catch` thủ công, TanStack Query tự động hóa hoàn toàn các công việc sau:

1. **Quản lý Bộ nhớ đệm (Caching):** Lưu kết quả API trong bộ nhớ tạm theo `queryKey` để tái sử dụng giữa các component.
2. **Quản lý Trạng thái Bất đồng bộ (Async Statuses):** Tự động cung cấp các cờ `isPending` (hoặc `isLoading`), `isError`, `isSuccess`, `isFetching`, `error`, `data`.
3. **Đồng bộ Dữ liệu ngầm (Background Revalidation):** Tự động làm mới dữ liệu bị cũ mà không ngắt đoạn trải nghiệm người dùng.
4. **Gộp Yêu cầu Trùng lặp (Request Deduplication):** Tự động gộp nhiều request giống nhau thành 1 HTTP request duy nhất.
5. **Thử lại khi lỗi (Automatic Retry):** Tự động gọi lại API khi bị lỗi mạng hoặc lỗi server.
6. **Quản lý Bộ nhớ (Garbage Collection):** Tự động xóa bớt cache không còn sử dụng để giải phóng RAM.

---

## 2. Chi tiết 4 tính năng nổi bật của TanStack Query

### 1. Caching & Stale-While-Revalidate (SWR)
- **Cơ chế:**
  - TanStack Query phân chia dữ liệu thành 2 trạng thái: **`fresh` (mới)** và **`stale` (cũ)**.
  - Khi dữ liệu được gọi lại (ví dụ khi user chuyển trang rồi quay lại), TanStack Query ngay lập tức trả về dữ liệu từ Cache để UI hiển thị tức thì (không có nhấp nháy loading).
  - Đồng thời, nó âm thầm kích hoạt một request ngầm (background refetch) để lấy dữ liệu mới nhất từ Server. Khi response trả về, Cache và UI tự động cập nhật mượt mà.

### 2. Automatic Retry (Tự động thử lại khi thất bại)
- **Cơ chế:**
  - Khi một query thất bại (ví dụ: lỗi mạng chập chờn hoặc Server lỗi 5xx), TanStack Query sẽ không lập tức báo lỗi ra UI.
  - Thay vào đó, nó tự động thử lại (mặc định 3 lần) với thuật toán **Exponential Backoff** (thời gian chờ giữa các lần thử tăng dần: 1s, 2s, 4s...) trước khi chuyển `isError = true`.

### 3. Refetch on Window Focus (Tự động cập nhật khi active lại cửa sổ)
- **Cơ chế:**
  - Khi người dùng chuyển sang ứng dụng khác (hoặc tab trình duyệt khác) rồi quay lại tab ứng dụng, sự kiện `visibilitychange` / `focus` kích hoạt.
  - TanStack Query tự động đánh dấu các query bị `stale` và refetch lại dữ liệu ngầm, giúp thông tin hiển thị luôn mới nhất mà người dùng không cần bấm F5.

### 4. Garbage Collection (`gcTime` / `cacheTime`)
- **Cơ chế:**
  - Khi một query không còn bất kỳ component nào sử dụng (chuyển sang trạng thái `unmounted` / `inactive`), TanStack Query sẽ bật bộ đếm thời gian (mặc định 5 phút).
  - Nếu sau khoảng thời gian `gcTime` mà không có component nào dùng lại query đó, dữ liệu trong Cache sẽ bị xóa hoàn toàn khỏi bộ nhớ RAM để tránh rò rỉ bộ nhớ (Memory Leak).
