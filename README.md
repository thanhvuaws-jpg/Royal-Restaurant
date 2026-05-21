# 🍽️ Royal Restaurant POS – Cloud Edition

> Hệ thống Quản lý Nhà hàng thời gian thực, kết hợp ứng dụng **Android** và **Web Admin**, vận hành hoàn toàn trên nền tảng **Cloud VPS**.

---

## 📌 Giới thiệu Dự án

**Royal Restaurant POS** là hệ thống quản lý nhà hàng toàn diện gồm hai thành phần chính:

| Thành phần | Nền tảng | Vai trò |
|---|---|---|
| 📱 **Ứng dụng Android** | Android (Java) | Nhân viên gọi món, quản lý bàn, xem thực đơn |
| 🖥️ **Web Admin / Thu Ngân** | HTML + JavaScript (jQuery) | Quản lý hệ thống, theo dõi đơn hàng, thống kê doanh thu |

Toàn bộ dữ liệu được lưu trữ và xử lý tập trung trên **Cloud VPS** tại địa chỉ `103.157.204.120`, không sử dụng SQLite hay bất kỳ bộ lưu trữ nội bộ nào trên thiết bị.

---

## 🏗️ Kiến trúc Hệ thống

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLOUD VPS  (103.157.204.120)                 │
│                                                                  │
│  ┌───────────────────┐      ┌────────────────────────────────┐  │
│  │   Web Server      │      │       Database Server          │  │
│  │   Laravel (PHP)   │      │       MySQL / MariaDB          │  │
│  │   Port :80/443    │◄────►│       Port :3306               │  │
│  │                   │      │                                │  │
│  │  /public/quantri/ │      │  Tables: nhanvien, loaimon,   │  │
│  │  (Web Admin UI)   │      │  monan, banan, dondat,        │  │
│  │                   │      │  chitietdondat, thanhtoan     │  │
│  └───────────────────┘      └────────────────────────────────┘  │
│                                       ▲                          │
│  ┌───────────────────┐                │                          │
│  │   REST API Server │                │                          │
│  │   PHP Scripts     │────────────────┘                          │
│  │   Port :8081      │                                           │
│  └───────────────────┘                                           │
└─────────────────────────────────────────────────────────────────┘
          ▲                          ▲
          │  Retrofit2 HTTP          │  jQuery AJAX
          │                          │
  ┌───────┴──────┐          ┌────────┴──────┐
  │ Android App  │          │  Web Browser  │
  │  (Java)      │          │  (Admin/Cashier│
  └──────────────┘          └───────────────┘
```

---

## 🛠️ Công nghệ Sử dụng

### 📱 Android Application
| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **Java** | JDK 11+ | Ngôn ngữ lập trình chính |
| **Android SDK** | API 24+ (Android 7.0) | Nền tảng ứng dụng di động |
| **Retrofit 2** | 2.9.0 | HTTP Client giao tiếp REST API |
| **OkHttp 3** | 4.x | Tầng mạng cơ sở, Timeout 30s |
| **Gson** | 2.x | Serialization/Deserialization JSON |
| **Glide** | 4.x | Tải và cache ảnh từ URL |
| **MPAndroidChart** | 3.x | Vẽ biểu đồ doanh thu (BarChart) |
| **Material Components** | 1.x | UI Components (TextInputLayout, CardView) |

### 🌐 Web Portal (Admin & Cashier)
| Công nghệ | Mục đích |
|---|---|
| **HTML5 + CSS3** | Cấu trúc và tạo kiểu giao diện |
| **Tailwind CSS** | Utility-first styling, glassmorphism |
| **jQuery 3.6** | AJAX calls, DOM manipulation |
| **Chart.js** | Biểu đồ doanh thu trực quan |
| **SweetAlert2** | Hộp thoại thông báo đẹp mắt |
| **Font Awesome 6** | Bộ biểu tượng vector |
| **YouTube Iframe API** | Phát nhạc nền trong hệ thống |

### ☁️ Backend & Infrastructure
| Công nghệ | Mục đích |
|---|---|
| **PHP 7/8** | Xử lý API endpoints (REST) |
| **Laravel** | Framework chính Web Server |
| **MySQL / MariaDB** | Cơ sở dữ liệu quan hệ |
| **Nginx** | Reverse Proxy & Web Server |
| **Linux VPS** | Máy chủ ảo đám mây |

---

## 📂 Cây Thư Mục Dự án

```
QLnhahang/
├── app/
│   └── src/main/
│       ├── java/com/sinhvien/orderdrinkapp/
│       │   ├── Activities/               # Màn hình chức năng
│       │   │   ├── SplashActivity.java       # Màn hình giới thiệu (3s)
│       │   │   ├── WelcomeActivity.java      # Chào mừng & tự đăng nhập
│       │   │   ├── LoginActivity.java        # Xác thực người dùng
│       │   │   ├── HomeActivity.java         # Màn hình chính + phân quyền
│       │   │   ├── AmountMenuActivity.java   # Gọi món & nhập số lượng
│       │   │   ├── PaymentActivity.java      # Thanh toán hóa đơn
│       │   │   ├── CashierConfirmActivity.java # Xác nhận đơn thu ngân
│       │   │   ├── AddMenuActivity.java      # Quản lý món ăn (CRUD)
│       │   │   ├── AddCategoryActivity.java  # Quản lý loại món (CRUD)
│       │   │   ├── AddTableActivity.java     # Quản lý bàn ăn (CRUD)
│       │   │   ├── AddStaffActivity.java     # Quản lý nhân viên (CRUD)
│       │   │   ├── DetailStatisticActivity.java # Chi tiết lịch sử đơn
│       │   │   ├── RegisterActivity.java     # Đăng ký bước 1
│       │   │   └── Register2ndActivity.java  # Đăng ký bước 2 (quyền hạn)
│       │   │
│       │   ├── Fragments/                # Giao diện thành phần (Tab)
│       │   │   ├── DisplayHomeFragment.java    # Dashboard tổng quan
│       │   │   ├── DisplayTableFragment.java   # Sơ đồ bàn ăn
│       │   │   ├── DisplayCategoryFragment.java # Danh mục thực đơn
│       │   │   ├── DisplayMenuFragment.java    # Danh sách món + tìm kiếm
│       │   │   ├── DisplayStaffFragment.java   # Quản lý nhân sự
│       │   │   └── DisplayStatisticFragment.java # Báo cáo & biểu đồ
│       │   │
│       │   ├── Api/                      # Tầng kết nối Cloud
│       │   │   ├── ApiClient.java            # Cấu hình Retrofit (Base URL, Timeout)
│       │   │   ├── ApiService.java           # Định nghĩa tất cả Endpoints REST
│       │   │   ├── OrderResponse.java        # Đối tượng phản hồi đơn hàng
│       │   │   ├── StaffResponse.java        # Đối tượng phản hồi nhân viên
│       │   │   ├── MonResponse.java          # Đối tượng phản hồi món ăn
│       │   │   ├── DishPageResponse.java     # Phân trang danh sách món
│       │   │   ├── LoaiMonResponse.java      # Đối tượng loại món
│       │   │   ├── TableResponse.java        # Đối tượng bàn ăn
│       │   │   ├── OrderDetailResponse.java  # Chi tiết đơn đặt
│       │   │   ├── StaffItemResponse.java    # Chi tiết nhân viên
│       │   │   └── StatisticResponse.java    # Đối tượng thống kê
│       │   │
│       │   ├── CustomAdapter/            # RecyclerView / GridView Adapters
│       │   │   ├── AdapterDisplayTable.java    # Hiển thị và quản lý bàn
│       │   │   ├── AdapterDisplayMenu.java     # Hiển thị thực đơn
│       │   │   ├── AdapterDisplayMenuRecycler.java # Thực đơn dạng danh sách
│       │   │   ├── AdapterDisplayCategory.java # Hiển thị loại món
│       │   │   ├── AdapterDisplayStaff.java    # Hiển thị nhân viên
│       │   │   ├── AdapterDisplayPayment.java  # Hiển thị hóa đơn thanh toán
│       │   │   └── AdapterDisplayStatistic.java # Hiển thị lịch sử đơn
│       │   │
│       │   ├── DTO/                      # Data Transfer Objects
│       │   │   ├── BanAnDTO.java             # Dữ liệu bàn ăn
│       │   │   ├── MonDTO.java               # Dữ liệu món ăn
│       │   │   ├── LoaiMonDTO.java           # Dữ liệu loại món
│       │   │   ├── NhanVienDTO.java          # Dữ liệu nhân viên
│       │   │   ├── DonDatDTO.java            # Dữ liệu đơn đặt
│       │   │   ├── ChiTietDonDatDTO.java     # Chi tiết đơn đặt
│       │   │   ├── ThanhToanDTO.java         # Dữ liệu thanh toán
│       │   │   └── QuyenDTO.java             # Dữ liệu quyền hạn
│       │   │
│       │   └── Utils/                    # Tiện ích hệ thống
│       │       ├── SessionManager.java       # Quản lý phiên & quyền (Admin/Staff)
│       │       └── ReceiptHelper.java        # Chụp ảnh & chia sẻ hóa đơn
│       │
│       └── res/
│           ├── layout/                   # File giao diện XML
│           ├── drawable/                 # Hình ảnh & vector icons
│           ├── values/
│           │   ├── colors.xml            # Bảng màu hệ thống
│           │   ├── strings.xml           # Chuỗi văn bản đa ngôn ngữ
│           │   └── themes.xml            # Theme & Style toàn cục
│           └── font/                     # Font chữ (Muli)
│
├── build.gradle                          # Cấu hình build Gradle
├── settings.gradle                       # Cài đặt module
└── README.md                             # Tài liệu này
```

---

## 🗄️ Cơ Sở Dữ Liệu (Database)

Hệ thống sử dụng **MySQL / MariaDB** trên Cloud VPS với thiết kế quan hệ như sau:

### Danh sách Bảng (Tables)

| Bảng | Mô tả | Trường chính |
|---|---|---|
| `nhanvien` | Tài khoản nhân viên & quản lý | `manv`, `hoten`, `tendn`, `matkhau`, `maquyen` |
| `quyen` | Phân quyền (Admin / Nhân viên) | `maquyen`, `tenquyen` |
| `loaimon` | Danh mục thực đơn | `maloai`, `tenloai`, `hinhanh` |
| `monan` | Danh sách món ăn | `mamon`, `tenmon`, `giatien`, `maloai`, `tinhtrang`, `hinhanh` |
| `banan` | Bàn ăn trong nhà hàng | `maban`, `tenban` |
| `dondat` | Đơn đặt món theo bàn | `madondat`, `manv`, `maban`, `thoigiantao`, `trangthai` |
| `chitietdondat` | Chi tiết các món trong đơn | `machitiet`, `madondat`, `mamon`, `soluong`, `dongia` |
| `thanhtoan` | Lịch sử thanh toán | `mathanhtoan`, `madondat`, `tongtien`, `phuongthuc`, `thoigian` |

### Sơ đồ Quan hệ (ERD tóm tắt)
```
quyen ──< nhanvien ──< dondat ──< chitietdondat >── monan >── loaimon
                          │
                        banan
                          │
                       thanhtoan
```

---

## 🌐 REST API Endpoints

Máy chủ API chạy tại: `http://103.157.204.120:8081/api/`

| Endpoint | Phương thức | Chức năng |
|---|---|---|
| `login.php` | POST | Xác thực đăng nhập nhân viên |
| `get_staff.php` | GET | Lấy danh sách nhân viên |
| `add_staff.php` | POST | Thêm nhân viên mới |
| `update_staff.php` | POST | Sửa / Xóa nhân viên |
| `get_categories.php` | GET | Lấy danh mục thực đơn |
| `update_category.php` | POST | Thêm / Sửa / Xóa loại món |
| `get_dishes.php` | GET | Lấy danh sách món (hỗ trợ phân trang & tìm kiếm) |
| `update_dish.php` | POST | Thêm / Sửa / Xóa món ăn |
| `update_dish_status.php` | POST | Bật/Tắt trạng thái món ăn |
| `get_tables.php` | GET | Lấy danh sách bàn |
| `add_table.php` | POST | Thêm bàn mới |
| `update_table_admin.php` | POST | Sửa / Xóa bàn |
| `get_order_by_table.php` | GET | Lấy đơn hiện tại của bàn |
| `create_order.php` | POST | Tạo đơn đặt mới |
| `add_order_detail.php` | POST | Thêm món vào đơn |
| `get_order_details.php` | GET | Lấy chi tiết đơn |
| `checkout_order.php` | POST | Thanh toán đơn hàng |
| `get_pending_orders.php` | GET | Lấy danh sách đơn chờ thanh toán |
| `confirm_payment.php` | POST | Xác nhận thanh toán (Thu ngân) |
| `get_paid_orders.php` | GET | Lịch sử đơn đã thanh toán |
| `get_statistics.php` | GET | Lấy dữ liệu thống kê doanh thu |
| `check_session.php` | POST | Kiểm tra phiên đăng nhập hợp lệ |

---

## ✨ Tính năng Hệ thống

### 📱 Ứng dụng Android
- 🔐 **Đăng nhập & Phân quyền:** Hệ thống tự động ẩn/hiện tính năng theo vai trò (Admin / Nhân viên)
- 🪑 **Sơ đồ Bàn thời gian thực:** Hiển thị trạng thái bàn trực tiếp từ Cloud (Trống / Đang dùng)
- 🍜 **Thực đơn đầy đủ:** Phân loại theo danh mục, tìm kiếm theo tên, hình ảnh từ Cloud
- 🛒 **Gọi món nhanh:** Nhân viên chọn bàn → Chọn món → Nhập số lượng → Gửi lên Cloud ngay lập tức
- 💳 **Thanh toán:** Xuất hóa đơn, chọn phương thức thanh toán, chia sẻ hóa đơn qua ảnh
- 📊 **Thống kê Doanh thu:** Biểu đồ cột trực quan, xem chi tiết từng đơn trong lịch sử

### 🖥️ Web Admin / Thu Ngân
- 👑 **Dashboard Quản lý:** Tổng quan doanh thu, số đơn, nhân viên theo thời gian thực
- 📋 **Quản lý Thực đơn:** Thêm, sửa, xóa món ăn và danh mục trực tiếp trên trình duyệt
- 👥 **Quản lý Nhân viên:** Tạo và quản lý tài khoản cho toàn bộ đội ngũ
- 💰 **Giao diện Thu Ngân:** Xem danh sách đơn hàng đang chờ, xác nhận thanh toán
- 🎵 **Trình phát Nhạc nền:** Hộp nhạc nổi tích hợp YouTube & MP3, có danh sách phát

---

## 🚀 Hướng dẫn Triển khai

### Yêu cầu
- Android Studio (Hedgehog trở lên)
- JDK 11+
- Android device / Emulator (API 24+)
- Kết nối Internet đến VPS

### Clone & Chạy
```bash
git clone https://github.com/thanhvuaws-jpg/Royal-Restaurant.git
cd Royal-Restaurant
```
Mở bằng **Android Studio** → **Sync Gradle** → **Run app**.

### Cấu hình API
Thay đổi địa chỉ server trong `ApiClient.java`:
```java
public static final String BASE_URL = "http://YOUR_VPS_IP:8081/";
```

### Web Portal
Truy cập tại: `http://vtkt.online/quantri/`
- `index.html` — Trang đăng nhập
- `admin.html` — Giao diện Quản lý (Admin)
- `cashier.html` — Giao diện Thu ngân

---

## 👤 Tác giả

**Vũ Thanh** — Royal Restaurant POS System  
🔗 [GitHub](https://github.com/thanhvuaws-jpg)

---

*© 2026 Royal Restaurant. Excellence in every detail.*
