# BÁO CÁO PHÂN TÍCH TOÀN DIỆN HỆ THỐNG POS CLOUD-NATIVE
## Đề tài: Hệ thống Quản lý Nhà hàng Royal POS (Bản đầy đủ tất cả màn hình)

---

### Phần 1. Giới thiệu đề tài
*   **Tên ứng dụng:** Royal Restaurant POS.
*   **Bối cảnh:** Nhà hàng cần một giải pháp quản lý đồng bộ dữ liệu trên Cloud để nhân viên có thể sử dụng điện thoại cá nhân phục vụ khách tại bàn mà không phụ thuộc vào máy chủ nội bộ.

---

### Phần 2. Danh sách và Phân tích CHI TIẾT TẤT CẢ màn hình

#### 1. Nhóm màn hình Khởi động & Xác thực (Auth)
*   **Màn hình Splash (SplashActivity):**
    *   *Chức năng:* Hiển thị logo thương hiệu, kiểm tra kết nối mạng ban đầu.
    *   *UI:* `ConstraintLayout` lồng `ImageView`. Sử dụng `Handler` để delay chuyển cảnh.
*   **Màn hình Đăng nhập (LoginActivity):**
    *   *UI:* `TextInputLayout` cho Tên đăng nhập và Mật khẩu. Nút "Đăng nhập" dùng Material Button.
    *   *Logic:* Gọi API `login.php`. Nếu thành công, lưu thông tin vào `SessionManager` (SharedPreferences) để duy trì đăng nhập.
*   **Màn hình Đăng ký (RegisterActivity & Register2ndActivity):**
    *   *UI:* Chia làm 2 giai đoạn (Step-by-step) để tránh làm người dùng ngộp thông tin.
    *   *Logic:* Truyền dữ liệu từ Step 1 sang Step 2 qua `Bundle`.

#### 2. Màn hình Chính (HomeActivity)
*   **Cấu trúc:** Sử dụng `BottomNavigationView` kết hợp với `FragmentContainerView`.
*   **Điều hướng:** Sử dụng `FragmentManager` để chuyển đổi giữa 4 Fragment chính: Home, Table, Menu, Staff.
*   **View Hierarchy:** 
    ```text
    CoordinatorLayout
    ├── AppBarLayout -> Toolbar
    ├── FrameLayout (Container)
    └── BottomNavigationView
    ```

#### 3. Màn hình Quản lý Bàn (DisplayTableFragment)
*   **Chức năng:** Xem sơ đồ bàn ăn.
*   **UI:** `GridView` hoặc `RecyclerView` với `GridLayoutManager`.
*   **Logic:** Tự động đổi màu icon bàn (Xanh: Trống, Đỏ: Có khách) dựa trên trường `TINHTRANG` nhận từ API `get_tables.php`.
*   **Action:** Khi click vào bàn -> Gọi `get_order_by_table.php` để biết bàn đó đã có hóa đơn chưa.

#### 4. Màn hình Thêm/Sửa Bàn (AddTableActivity)
*   **UI:** Đơn giản với 1 `TextInputLayout` và nút "Xác nhận".
*   **Logic:** Dùng chung 1 giao diện cho cả 2 hành động. Phân biệt bằng biến `maban` truyền qua Intent (maban=0 là Thêm, maban!=0 là Sửa).

#### 5. Màn hình Quản lý Thực đơn (Category & Menu Selection)
*   **DisplayCategoryFragment:** Danh sách các loại món (Khai vị, Món chính...). Dùng `RecyclerView` cuộn dọc.
*   **DisplayMenuFragment:** Danh sách các món ăn thuộc loại đã chọn. 
    *   *Advanced Control:* Tích hợp `SearchView` trên Toolbar để lọc món ăn nhanh theo tên.
*   **AmountMenuActivity (Chọn số lượng):** 
    *   Xuất hiện khi nhân viên click chọn món cho khách.
    *   *UI:* `EditText` chỉ cho nhập số. Nút "Đồng ý" sẽ gọi API `add_order_detail.php`.

#### 6. Màn hình Quản lý Món ăn - Admin (AddMenuActivity)
*   **Chức năng:** Cho phép Admin thêm/sửa món ăn và tải ảnh lên Cloud.
*   **Kỹ thuật Image:** Sử dụng `Base64` để mã hóa ảnh từ `ImageView` và gửi lên Server PHP để lưu thành file `.jpg`.
*   **Logic Self-Sufficient:** Tự động fetch thông tin loại món từ Server để điền vào UI, tránh sai sót dữ liệu.

#### 7. Màn hình Thanh toán (PaymentActivity)
*   **UI:** Hiển thị `ListView` chứa danh sách các món khách đã gọi (ThanhToanDTO).
*   **Logic:** Tính tổng tiền ngay trên App. Khi nhấn "Thanh toán", gọi API `checkout_order.php` để cập nhật trạng thái bàn về "Trống" và hóa đơn về "Đã trả tiền".

#### 8. Màn hình Thống kê (DisplayStatisticFragment & DetailStatisticActivity)
*   **Màn hình Danh sách:** Hiển thị các hóa đơn đã hoàn thành. 
*   **Màn hình Chi tiết:** Khi click vào một hóa đơn, App sẽ mở `DetailStatisticActivity` hiển thị toàn bộ danh sách món ăn của hóa đơn đó. 
    *   *Dữ liệu:* Truyền mã hóa đơn qua `Intent.putExtra("madondat", id)`.

#### 9. Màn hình Quản lý Nhân viên (DisplayStaffFragment & AddStaffActivity)
*   **Chức năng:** Chỉ dành cho Admin.
*   **UI:** Danh sách nhân viên kèm thông tin Email, SĐT.
*   **Bảo mật:** Check `SessionManager.isAdmin()` trước khi cho phép vào màn hình này.

---

### Phần 3. Phân tích Các Thành Phần Nâng Cao (Advanced)

*   **Custom Adapter System:** 
    *   Mỗi danh sách đều có một Adapter riêng (VD: `AdapterDisplayTable`, `AdapterDisplayMenu`). 
    *   Tất cả đều dùng mẫu **ViewHolder** để tối ưu tốc độ cuộn, tránh giật lag (Jank).
*   **Cơ chế truyền dữ liệu qua Fragment:**
    *   Sử dụng `setArguments(bundle)` khi tạo Fragment để truyền mã bàn hoặc mã loại món.
*   **Xử lý Responsive:** 
    *   Sử dụng `android:layout_width="0dp"` và `app:layout_constraintHorizontal_weight` để chia tỷ lệ các cột trong GridView đều nhau trên mọi kích thước màn hình.

---

### Phần 4. Phân tích Trải nghiệm người dùng (UX) và Thiết kế (UI)
*   **Màu sắc:** Sử dụng tông màu Nâu (Brown 700) làm chủ đạo, tạo cảm giác ấm cúng, sang trọng phù hợp với nhà hàng.
*   **Tương tác:** 
    *   Hỗ trợ **Context Menu** (Nhấn giữ) để hiện các tùy chọn Sửa/Xóa, giúp màn hình gọn gàng hơn.
    *   Sử dụng **Dialog xác nhận** trước khi thực hiện các hành động nguy hiểm như Xóa nhân viên hoặc Xóa món ăn.
*   **Thông báo:** Sử dụng `Toast` và `Snackbar` để phản hồi tức thì các kết quả từ Cloud (VD: "Đã cập nhật giá món ăn thành công").

---

### Phần 5. Quy chuẩn Kỹ thuật
*   **View:** `TextView`, `ImageView`, `Button`, `FloatingActionButton`.
*   **ViewGroup:** `ConstraintLayout`, `LinearLayout`, `FrameLayout`, `RecyclerView`.
*   **Advanced:** `Retrofit`, `Glide`, `Gson`, `PDO (Server side)`.

---

### Phần 6. Kết luận
Hệ thống Royal Restaurant POS là một ví dụ điển hình về ứng dụng di động hiện đại. Việc áp dụng kiến trúc Cloud kết hợp với các thành phần UI/UX của Material Design giúp ứng dụng đạt được sự chuyên nghiệp, ổn định và mang lại giá trị thực tiễn cao cho doanh nghiệp.
