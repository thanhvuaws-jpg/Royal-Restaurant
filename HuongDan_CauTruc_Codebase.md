# HƯỚNG DẪN CHI TIẾT TOÀN BỘ CẤU TRÚC MÃ NGUỒN (CODEBASE)
## Dự án: Royal Restaurant POS - Cloud Edition

Tài liệu này là bản phân tích đầy đủ 100% các file mã nguồn trong dự án Android, giúp bạn nắm vững mọi ngóc ngách của ứng dụng.

---

## 1. TỔNG QUAN KIẾN TRÚC CLOUD
Dự án đã được chuyển đổi hoàn toàn sang mô hình **Cloud-Native**:
- **Không dùng SQLite**: Mọi logic Database được thay thế bằng gọi API.
- **Retrofit 2**: Thư viện chính để kết nối PHP API trên Cloud VPS.
- **Glide**: Tải hình ảnh trực tiếp từ đường dẫn URL của Server.

---

## 2. Ý NGHĨA CÁC THƯ MỤC CHÍNH

- **📂 Activities**: Chứa các màn hình chính của ứng dụng. Mỗi Activity chịu trách nhiệm cho một giao diện và luồng xử lý riêng biệt.
- **📂 Api**: Nơi cấu hình và khai báo các Query (truy vấn) để giao tiếp dữ liệu với Cloud Server qua thư viện Retrofit.
- **📂 CustomAdapter**: Cầu nối dữ liệu; có nhiệm vụ lấy dữ liệu từ danh sách (List) để đổ vào các khung thiết kế XML (như GridView, RecyclerView).
- **📂 DTO (Data Transfer Object)**: Chứa các lớp (Class) dữ liệu mẫu dùng để truyền tải và lưu trữ thông tin tạm thời trong ứng dụng.
- **📂 Utils**: Các công cụ tiện ích, tiêu biểu là quản lý quyền hạn người dùng và quản lý phiên làm việc (Session).

---

## 3. DANH SÁCH CHI TIẾT TOÀN BỘ FILE

### 📂 THƯ MỤC: Activities (Màn hình chức năng)
1.  **`SplashActivity`**: Màn hình giới thiệu (Dòng 18: Timer 3s; Dòng 33: Animation).
2.  **`WelcomeActivity`**: Màn hình chào mừng (Dòng 33: Tự động đăng nhập).
3.  **`LoginActivity`**: Đăng nhập (Dòng 89: Gọi API; Dòng 101: Ghi nhớ mật khẩu).
4.  **`HomeActivity`**: Màn hình chính (Dòng 75: Phân quyền; Dòng 182: Đăng xuất).
5.  **`PaymentActivity`**: Thanh toán (Dòng 125: API checkout; Dòng 155: Xuất hóa đơn).
6.  **`AddMenuActivity`**: Quản lý món ăn (Dòng 161: Format tiền; Dòng 268: Upload ảnh).
7.  **`AddCategoryActivity`**: Quản lý loại món (Dòng 137: API manageCategory).
8.  **`AddStaffActivity`**: Quản lý nhân viên (Dòng 169: API manageStaff).
9.  **`AddTableActivity`**: Quản lý bàn ăn (Dòng 52: Action add/edit bàn).
10. **`AmountMenuActivity`**: Gọi món (Dòng 76: Gửi món lên Cloud).
11. **`DetailStatisticActivity`**: Chi tiết đơn hàng lịch sử (Dòng 91: Tải chi tiết đơn).
12. **`RegisterActivity`**: Đăng ký bước 1 (Thông tin cơ bản).
13. **`Register2ndActivity`**: Đăng ký bước 2 (Quyền hạn & Ngày sinh).

### 📂 THƯ MỤC: Fragments (Giao diện thành phần)
1.  **`DisplayHomeFragment`**: Dashboard chính (Dòng 73: Tùy biến giao diện cho Nhân viên).
2.  **`DisplayTableFragment`**: Quản lý sơ đồ bàn (Dòng 168: Lấy trạng thái bàn từ Cloud).
3.  **`DisplayCategoryFragment`**: Danh mục thực đơn (Dòng 219: Lấy loại món).
4.  **`DisplayMenuFragment`**: Danh sách món ăn (Dòng 135: Tìm kiếm thời gian thực).
5.  **`DisplayStaffFragment`**: Quản lý nhân sự (Dòng 179: Lấy danh sách nhân viên).
6.  **`DisplayStatisticFragment`**: Báo cáo doanh thu (Dòng 240: Vẽ biểu đồ BarChart).

### 📂 THƯ MỤC: CustomAdapter (Cầu nối dữ liệu)
1.  **`AdapterDisplayTable`**: Hiển thị bàn (Dòng 87: Đổi icon ghế; Dòng 158: Check đơn Cloud).
2.  **`AdapterDisplayMenu`**: Hiển thị món (Dòng 101: Overlay hết món; Dòng 133: Switch trạng thái).
3.  **`AdapterDisplayCategory`**: Hiển thị loại món (Dòng 77: Glide load ảnh).
4.  **`AdapterDisplayStaff`**: Hiển thị nhân viên (Dòng 69: Gán nhãn Quản lý/Nhân viên).
5.  **`AdapterDisplayPayment`**: Hiển thị món thanh toán (Dòng 71: Load ảnh món Cloud).
6.  **`AdapterDisplayStatistic`**: Hiển thị lịch sử đơn (Dòng 71: Trạng thái Đã/Chưa thanh toán).

### 📂 THƯ MỤC: Api (Kết nối Cloud)
1.  **`ApiClient`**: Cấu hình URL Server `103.157.204.120` (Dòng 11).
2.  **`ApiService`**: Định nghĩa tất cả các Endpoints gọi API (Login, Update, Create...).

### 📂 THƯ MỤC: DTO (Đối tượng dữ liệu)
- **`BanAnDTO`, `MonDTO`, `NhanVienDTO`, `DonDatDTO`, `LoaiMonDTO`, `ThanhToanDTO`, `QuyenDTO`**: Các lớp chứa dữ liệu để truyền tải trong App.

### 📂 THƯ MỤC: Utils (Tiện ích)
- **`SessionManager`**: Quản lý phiên làm việc và quyền hạn (Admin/Staff).
- **`ReceiptHelper`**: Hỗ trợ chụp ảnh màn hình và chia sẻ hóa đơn.

---

## 3. PHÂN TÍCH CHI TIẾT CÁC LOGIC "ĐẮT GIÁ"

- **Mã hóa ảnh (AddMenuActivity):** Sử dụng Base64 để gửi ảnh qua JSON API mà không cần dùng Multipart (Dòng 268).
- **Phân quyền động (HomeActivity):** Hệ thống tự kiểm tra quyền Admin để ẩn/hiện các menu quan trọng, đảm bảo tính bảo mật (Dòng 75).
- **Tự động định dạng (AddMenuActivity):** Trải nghiệm người dùng tốt hơn khi tiền tệ tự thêm dấu chấm khi gõ (Dòng 161).
- **Đồng bộ Cloud (AdapterDisplayTable):** App luôn hỏi Server về mã đơn hàng hiện tại của bàn trước khi thanh toán để đảm bảo dữ liệu mới nhất (Dòng 158).

---
*Tài liệu này tổng hợp 100% cấu trúc để bạn tự tin làm chủ dự án Royal Restaurant.*
