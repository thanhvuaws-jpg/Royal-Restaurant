<div align="center">

<img src="https://img.shields.io/badge/-%F0%9F%8D%BD%EF%B8%8F%20ROYAL%20RESTAURANT%20POS-gold?style=for-the-badge&labelColor=1a0a00&color=c8902a" alt="Royal Restaurant POS" height="45"/>

<br/>
<br/>

**Hệ thống Quản lý Nhà hàng toàn diện — Android + Web Admin — Cloud VPS**

<br/>

[![Android](https://img.shields.io/badge/Android-API%2021%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Java-JDK%2011%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com)
[![PHP](https://img.shields.io/badge/PHP-7%2F8-777BB4?style=flat-square&logo=php&logoColor=white)](https://php.net)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://mysql.com)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://docker.com)
[![Cloudflare](https://img.shields.io/badge/Cloudflare-SSL-F38020?style=flat-square&logo=cloudflare&logoColor=white)](https://cloudflare.com)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

<br/>

> *"Từ gọi món đến thanh toán — mọi thứ đồng bộ thời gian thực trên Cloud"*

<br/>

[📱 Android App](#-ứng-dụng-android) · [🖥️ Web Admin](#️-web-portal--admin--thu-ngân) · [☁️ Backend API](#️-backend--infrastructure) · [🚀 Triển khai](#-hướng-dẫn-triển-khai) · [📸 Demo](#-demo)

</div>

---

## 📌 Giới thiệu

**Royal Restaurant POS** là hệ thống quản lý nhà hàng end-to-end gồm hai thành phần hoàn toàn đồng bộ:

| | Thành phần | Nền tảng | Vai trò |
|---|---|---|---|
| 📱 | **Android App** | Java · Android API 21+ | Nhân viên gọi món, quản lý bàn, xem thực đơn, thanh toán |
| 🖥️ | **Web Admin / Thu Ngân** | HTML · jQuery · Tailwind CSS | Quản lý hệ thống, thống kê doanh thu, xác nhận thanh toán |

Toàn bộ dữ liệu xử lý tập trung trên **Cloud VPS**, không dùng bất kỳ bộ nhớ cục bộ nào — mọi thiết bị luôn hiển thị trạng thái mới nhất theo thời gian thực.

---

## 🏗️ Kiến trúc Hệ thống

```
┌──────────────────────────────────────────────────────────────────────┐
│                       CLOUD VPS (103.157.204.120)                    │
│                                                                       │
│   ┌─────────────────────┐         ┌──────────────────────────────┐   │
│   │   Apache2  :80/443  │         │     Docker — REST API        │   │
│   │   + Cloudflare SSL  │         │     PHP Scripts  :8081       │   │
│   │                     │─proxy──▶│                              │   │
│   │  /public/quantri/   │ /api/   │  api/login.php               │   │
│   │  Web Admin UI       │         │  api/get_dishes.php  ...     │   │
│   └─────────────────────┘         └──────────────┬───────────────┘   │
│                                                   │                   │
│   ┌─────────────────────┐         ┌──────────────▼───────────────┐   │
│   │  Docker — Laravel   │         │   Docker — MySQL 8.0         │   │
│   │  PHP Framework :8000│◀───────▶│   Port :3307 (internal)      │   │
│   └─────────────────────┘         └──────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────┘
            ▲                                    ▲
            │  Retrofit2 / OkHttp (HTTP)         │  jQuery AJAX (HTTPS)
            │                                    │
   ┌────────┴──────────┐               ┌─────────┴──────────┐
   │   📱 Android App  │               │  🌐 Web Browser    │
   │   Java · API 21+  │               │  Admin / Cashier   │
   └───────────────────┘               └────────────────────┘
```

> **SSL Flow:** Browser → HTTPS (Cloudflare) → Apache → `/api/` proxy → Docker:8081

---

## 📱 Ứng dụng Android

### Màn hình & Luồng điều hướng

```
SplashActivity (Màn hình khởi động 3s)
       │
WelcomeActivity ──── (Đã đăng nhập?) ────┬─▶ [Admin/NV/Thu ngân] ─▶ HomeActivity
       │                                 │                             │
LoginActivity                            └─▶ [Khách hàng] ────────▶ CustomerHomeActivity
       │                                                               │
Register (Bước 1+2)                                                    ├─▶ CustomerBookingFragment
                                                                       │    (Đặt bàn & Chọn món)
                                                                       └─▶ CustomerProfileFragment
                                                                            (Lịch sử & Chi tiêu)
```

### Tính năng chính

- 🔐 **Đăng nhập & Bảo mật** — Phân quyền vai trò người dùng (Admin, Nhân viên, Thu ngân, Khách hàng). Bảo mật dữ liệu mật khẩu bằng cơ chế mã hóa **Bcrypt** tự động cả ở client và server, hỗ trợ chuyển đổi mật khẩu cũ tự động khi đăng nhập thành công.
- ⚡ **Realtime Socket.IO** — Đồng bộ thông tin đơn hàng, đặt bàn, thay đổi trạng thái bàn ăn tức thời giữa các nhân viên, thu ngân và bếp mà không cần tải lại trang.
- 💾 **Bộ nhớ đệm SQLite & Đồng bộ hóa nền** — Hỗ trợ tải dữ liệu ngoại tuyến (Offline) tức thì bằng cơ sở dữ liệu `ql_nhahang_local.db` cục bộ, tự động đồng bộ ngầm cập nhật mới từ Cloud khi thiết bị kết nối mạng.
- 🪑 **Sơ đồ Bàn thời gian thực** — Trạng thái Trống / Đang dùng cập nhật trực tiếp và đồng bộ realtime.
- 🍜 **Thực đơn thông minh** — Phân trang hiển thị món ăn, tìm kiếm theo tên, tải ảnh từ Cloud cực nhanh thông qua thư viện Glide.
- 🛒 **Gọi món nhanh** — Trải nghiệm UX mượt mà với công cụ chống nhấp chuột nhanh liên tiếp (**Spam double-click protection**) tránh tạo đơn trùng lặp, chọn món gán trực tiếp lên sơ đồ bàn ăn.
- 💳 **Thanh toán đa phương thức** — Hỗ trợ thanh toán Tiền mặt, Chuyển khoản, hoặc tự tạo mã QR VietQR (BIDV) động theo giá trị hóa đơn.
- 📊 **Thống kê doanh thu** — Trực quan hóa doanh thu dạng biểu đồ cột thông qua thư viện MPAndroidChart, xem lại lịch sử chi tiết hóa đơn.
- 📸 **Xuất hóa đơn** — Chụp ảnh biên lai và chia sẻ trực tiếp với khách hàng qua ReceiptHelper.

### Stack công nghệ

| Thư viện | Phiên bản | Mục đích |
|---|---|---|
| **Retrofit 2** | 2.9.0 | Khung kết nối, định nghĩa các API Endpoints giao tiếp REST API |
| **OkHttp 3** | 4.x | Quản lý kết nối HTTP, cấu hình Timeout 30s tránh nghẽn mạng |
| **Gson** | 2.x | Serialize / Deserialize dữ liệu JSON thành Java Object |
| **Socket.IO Client** | 2.0.0 | Thiết lập kết nối hai chiều (Full-Duplex) Real-time với Server |
| **SQLite (OpenHelper)** | Built-in | Cơ sở dữ liệu cục bộ hỗ trợ lưu cache ngoại tuyến và tối ưu hóa tải trang |
| **Glide** | 4.12.0 | Tải, xử lý và bộ nhớ đệm (caching) hình ảnh từ máy chủ |
| **MPAndroidChart** | 3.1.0 | Biểu đồ trực quan hóa dữ liệu thống kê doanh số |
| **Material Components** | 1.3.0 | Bộ công cụ thiết kế Material Design (CardView, TextInputLayout, BottomSheet) |
| **CircleImageView** | 3.1.0 | Định dạng ảnh đại diện nhân viên dạng hình tròn thẩm mỹ |

---

## 🖥️ Web Portal — Admin & Thu Ngân

Truy cập tại: **[https://vtkt.online/quantri](https://vtkt.online/quantri)**

| File | Vai trò |
|---|---|
| `index.html` | Trang đăng nhập (dark/light mode) |
| `admin.html` | Dashboard quản lý toàn hệ thống |
| `cashier.html` | Giao diện thu ngân xác nhận thanh toán |

### Tính năng

- 👑 **Dashboard** — Doanh thu, số đơn, nhân viên theo thời gian thực + đồng hồ live
- 📋 **Quản lý Thực đơn** — CRUD món ăn & danh mục trực tiếp trên trình duyệt
- 👥 **Quản lý Nhân viên** — Tạo, sửa, xóa tài khoản + phân quyền
- 💰 **Thu Ngân** — Danh sách đơn chờ thanh toán, xác nhận 1 click
- 🎵 **Music Player** — Trình phát nhạc nổi tích hợp YouTube Iframe API + MP3
- 🌗 **Dark / Light Mode** — Chuyển chủ đề mượt mà, lưu localStorage

### Stack công nghệ

| Công nghệ | Mục đích |
|---|---|
| **Tailwind CSS** | Utility-first styling, glassmorphism |
| **jQuery 3.6** | AJAX calls, DOM manipulation |
| **Chart.js** | Biểu đồ doanh thu |
| **SweetAlert2** | Hộp thoại thông báo |
| **Font Awesome 6** | Bộ icon vector |
| **YouTube Iframe API** | Nhạc nền hệ thống |

---

## ☁️ Backend & Infrastructure

### REST API Endpoints

Base URL: `https://vtkt.online/api/`

| Nhóm | Endpoint | Method | Chức năng |
|---|---|---|---|
| **Auth** | `login.php` | POST | Đăng nhập, trả về token session |
| | `check_session.php` | POST | Kiểm tra phiên hợp lệ (polling) |
| **Nhân viên** | `get_staff.php` | GET | Danh sách nhân viên |
| | `add_staff.php` | POST | Thêm mới |
| | `update_staff.php` | POST | Sửa / Xóa |
| | `get_staff_by_id.php` | GET | Chi tiết theo ID |
| **Thực đơn** | `get_dishes.php` | GET | Danh sách (phân trang + tìm kiếm) |
| | `update_dish.php` | POST | Thêm / Sửa / Xóa |
| | `update_dish_status.php` | POST | Bật/Tắt trạng thái |
| | `get_dish_by_id.php` | GET | Chi tiết theo ID |
| **Loại món** | `get_categories.php` | GET | Danh sách danh mục |
| | `update_category.php` | POST | Thêm / Sửa / Xóa |
| **Bàn ăn** | `get_tables.php` | GET | Danh sách bàn |
| | `add_table.php` | POST | Thêm bàn |
| | `update_table_admin.php` | POST | Sửa / Xóa |
| | `delete_table.php` | POST | Xóa bàn |
| **Đơn hàng** | `create_order.php` | POST | Tạo đơn mới |
| | `add_order_detail.php` | POST | Thêm món vào đơn |
| | `get_order_by_table.php` | GET | Đơn hiện tại của bàn |
| | `get_order_details.php` | GET | Chi tiết đơn |
| | `get_pending_orders.php` | GET | Đơn đang chờ thanh toán |
| | `checkout_order.php` | POST | Thanh toán (nhân viên) |
| | `confirm_payment.php` | POST | Xác nhận (thu ngân) |
| | `get_paid_orders.php` | GET | Lịch sử đã thanh toán |
| | `check_order_status.php` | GET | Kiểm tra trạng thái đơn |
| **Thống kê** | `get_statistics.php` | GET | Dữ liệu doanh thu |

---

## 🗄️ Cơ sở Dữ liệu

Sử dụng **MySQL 8.0** chạy trong Docker container.

### Sơ đồ Quan hệ

```
quyen ──────< nhanvien ─────────< dondat ──────< chitietdondat >── monan >── loaimon
                                     │
                                   banan
                                     │
                                  thanhtoan
```

### Bảng dữ liệu

| Bảng | Mô tả | Trường chính |
|---|---|---|
| `quyen` | Phân quyền hệ thống | `maquyen`, `tenquyen` |
| `nhanvien` | Tài khoản nhân viên | `manv`, `hoten`, `tendn`, `matkhau`, `maquyen` |
| `loaimon` | Danh mục thực đơn | `maloai`, `tenloai`, `hinhanh` |
| `monan` | Món ăn | `mamon`, `tenmon`, `giatien`, `maloai`, `tinhtrang`, `hinhanh` |
| `banan` | Bàn ăn | `maban`, `tenban` |
| `dondat` | Đơn đặt món | `madondat`, `manv`, `maban`, `thoigiantao`, `trangthai` |
| `chitietdondat` | Chi tiết đơn | `machitiet`, `madondat`, `mamon`, `soluong`, `dongia` |
| `thanhtoan` | Lịch sử thanh toán | `mathanhtoan`, `madondat`, `tongtien`, `phuongthuc`, `thoigian` |

---

## 🗂️ Cấu trúc Project

<details>
<summary><b>📱 Android App — Royal-Restaurant/</b></summary>

```
app/src/main/java/com/sinhvien/orderdrinkapp/
├── Activities/
│   ├── SplashActivity.java          # Màn hình giới thiệu (3s)
│   ├── WelcomeActivity.java         # Chào mừng & tự đăng nhập
│   ├── LoginActivity.java           # Xác thực người dùng
│   ├── HomeActivity.java            # Màn hình chính + điều hướng tab
│   ├── AmountMenuActivity.java      # Gọi món & nhập số lượng
│   ├── PaymentActivity.java         # Thanh toán hóa đơn
│   ├── CashierConfirmActivity.java  # Xác nhận đơn (Thu ngân)
│   ├── AddMenuActivity.java         # CRUD món ăn
│   ├── AddCategoryActivity.java     # CRUD danh mục
│   ├── AddTableActivity.java        # CRUD bàn ăn
│   ├── AddStaffActivity.java        # CRUD nhân viên
│   ├── DetailStatisticActivity.java # Chi tiết lịch sử đơn
│   ├── RegisterActivity.java        # Đăng ký bước 1
│   └── Register2ndActivity.java     # Đăng ký bước 2 (phân quyền)
│
├── Fragments/
│   ├── DisplayHomeFragment.java     # Dashboard tổng quan
│   ├── DisplayTableFragment.java    # Sơ đồ bàn ăn
│   ├── DisplayCategoryFragment.java # Danh mục thực đơn
│   ├── DisplayMenuFragment.java     # Danh sách món + tìm kiếm
│   ├── DisplayStaffFragment.java    # Quản lý nhân sự
│   ├── DisplayCashierFragment.java  # Thu ngân
│   └── DisplayStatisticFragment.java # Báo cáo & biểu đồ
│
├── Api/
│   ├── ApiClient.java               # Retrofit config (Base URL, Timeout 30s)
│   ├── ApiService.java              # Interface định nghĩa tất cả endpoints
│   └── *Response.java               # Models phản hồi từ server
│
├── DTO/                             # Data Transfer Objects
├── CustomAdapter/                   # RecyclerView / GridView Adapters
└── Utils/
    ├── SessionManager.java          # Quản lý phiên & quyền (SharedPreferences)
    └── ReceiptHelper.java           # Chụp ảnh & chia sẻ hóa đơn
```
</details>

<details>
<summary><b>🖥️ Web Portal — web-odrinapp/</b></summary>

```
web-odrinapp/
├── index.html        # Trang đăng nhập (dark/light mode)
├── admin.html        # Dashboard quản lý (Admin)
├── cashier.html      # Giao diện thu ngân
└── js/
    ├── config.js     # BASE_URL cấu hình API
    ├── login.js      # Xử lý đăng nhập & redirect theo quyền
    ├── admin.js      # Logic toàn bộ trang Admin
    ├── cashier.js    # Logic trang Thu ngân
    └── music-player.js # Trình phát nhạc nổi
```
</details>

---

## 🚀 Hướng dẫn Triển khai

### Yêu cầu

- Android Studio Hedgehog trở lên
- JDK 11+
- Android device / Emulator API 21+
- Docker & Docker Compose (cho backend)
- Kết nối Internet đến VPS

### 1. Clone project

```bash
# Android App
git clone https://github.com/thanhvuaws-jpg/Royal-Restaurant.git

# Web Portal
git clone https://github.com/thanhvuaws-jpg/web-odrinapp.git
```

### 2. Chạy Backend (Docker)

```bash
cd /path/to/backend
docker-compose up -d
```

Các service sẽ khởi động:
- `domain_app` — Laravel API tại `:8000`
- `domain_db` — MySQL 8.0 tại `127.0.0.1:3307`
- `domain_phpmyadmin` — phpMyAdmin tại `127.0.0.1:8080`

### 3. Cấu hình Web Portal

Sửa `js/config.js`:

```js
const CONFIG = {
    BASE_URL: "/api/"   // Nếu deploy cùng domain
    // hoặc BASE_URL: "https://your-domain.com/api/"
};
```

Copy thư mục web vào public folder của Laravel:

```bash
cp -r web-odrinapp/ /var/www/domain/public/quantri/
```

### 4. Cấu hình Android App

Sửa `ApiClient.java`:

```java
public static final String BASE_URL = "http://YOUR_VPS_IP:8081/";
```

Mở bằng **Android Studio** → Sync Gradle → Run.

### 5. Apache Proxy (tránh Mixed Content HTTPS)

Thêm vào `/etc/apache2/sites-available/your-domain.conf`:

```apache
# Phải đặt TRƯỚC ProxyPass /
ProxyPass /api/ http://localhost:8081/
ProxyPassReverse /api/ http://localhost:8081/

ProxyPass / http://localhost:8000/
ProxyPassReverse / http://localhost:8000/
```

```bash
systemctl reload apache2
```

---

## 🔐 Phân quyền Hệ thống

| Vai trò | Mã quyền | Giao diện chính | Quyền truy cập |
|---|---|---|---|
| **Admin** | `1` | `HomeActivity` | Toàn quyền quản trị hệ thống: quản lý nhân sự, thực đơn, sơ đồ bàn, cấu hình và báo cáo thống kê doanh số |
| **Nhân viên** | `2` | `HomeActivity` | Phục vụ bàn ăn: Xem sơ đồ bàn, gọi món trực tiếp tại bàn, thanh toán nhanh qua BIDV QR / Tiền mặt |
| **Thu ngân** | `3` | `HomeActivity` | Xác nhận hóa đơn: Kiểm tra và duyệt thanh toán cho các đơn hàng của nhân viên/khách đặt |
| **Khách hàng** | `4` | `CustomerHomeActivity` | Khách dùng bữa: Tự đặt bàn trước, chọn món qua menu online, theo dõi tiến độ đơn hàng thời gian thực, quản lý lịch sử chi tiêu |

---

## 📸 Demo

| Android App | Web Admin |
|---|---|
| Sơ đồ bàn thời gian thực | Dashboard doanh thu |
| Gọi món & Thanh toán | Quản lý thực đơn |
| Biểu đồ thống kê | Giao diện thu ngân |

🌐 **Web Portal:** [https://vtkt.online/quantri](https://vtkt.online/quantri)

---

## 👥 Tác giả

<div align="center">

**Vũ Thanh** — Full-stack Developer

[![GitHub](https://img.shields.io/badge/GitHub-thanhvuaws--jpg-181717?style=flat-square&logo=github)](https://github.com/thanhvuaws-jpg)

</div>

---

<div align="center">

*© 2026 Royal Restaurant POS — Excellence in every detail.*

⭐ Nếu project hữu ích, hãy để lại một star nhé!

</div>