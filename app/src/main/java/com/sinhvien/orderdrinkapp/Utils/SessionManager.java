package com.sinhvien.orderdrinkapp.Utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager — Quản lý phiên đăng nhập tập trung (Authentication & Session State Manager).
 *
 * Nhiệm vụ:
 * - Lưu trữ thông tin đăng nhập thành công của nhân viên hoặc khách hàng vào SharedPreferences nội bộ.
 * - Hỗ trợ kiểm tra quyền hạn của tài khoản: Admin (mã 1), Phục vụ (mã 2), Thu ngân (mã 3), Khách hàng (mã 4).
 * - Cung cấp Token xác thực (bearer token) cho các yêu cầu gửi lên Server API.
 * - Hỗ trợ lưu trữ các cài đặt cá nhân của ứng dụng như kiểu hiển thị thanh điều hướng (Drawer Menu hoặc Bottom Navigation).
 */
public class SessionManager {

    // Tên file SharedPreferences lưu dữ liệu phiên đăng nhập
    private static final String PREF_NAME = "luuquyen";
    
    // Các khoá định danh lưu trữ dữ liệu
    private static final String KEY_MAQUYEN = "maquyen";
    private static final String KEY_MANV = "manv";
    private static final String KEY_HOTEN = "hoten";
    private static final String KEY_TOKEN = "session_token";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_SDT = "sdt";
    private static final String KEY_HINHANH = "hinhanh";

    /**
     * Lấy token xác thực (JWT Token) của phiên đăng nhập hiện thời.
     * @return Chuỗi token đã lưu, hoặc chuỗi rỗng nếu chưa đăng nhập.
     */
    public static String getToken(Context context) {
        return getPrefs(context).getString(KEY_TOKEN, "");
    }

    /**
     * Lấy họ tên đầy đủ của nhân viên/khách hàng đang đăng nhập.
     */
    public static String getFullName(Context context) {
        return getPrefs(context).getString(KEY_HOTEN, "");
    }

    /**
     * Lấy mã quyền (Role Code) của tài khoản đang đăng nhập.
     * @return Mã quyền dạng số nguyên: 1 (Admin), 2 (Phục vụ), 3 (Thu ngân), 4 (Khách hàng).
     */
    public static int getMaQuyen(Context context) {
        return getPrefs(context).getInt(KEY_MAQUYEN, 0);
    }

    /**
     * Kiểm tra xem tài khoản đang đăng nhập có phải là Admin (Quản trị viên) hay không.
     */
    public static boolean isAdmin(Context context) {
        return getMaQuyen(context) == 1;
    }

    /**
     * Kiểm tra xem tài khoản đang đăng nhập có phải là Thu ngân hay không.
     */
    public static boolean isCashier(Context context) {
        return getMaQuyen(context) == 3;
    }

    /**
     * Kiểm tra xem tài khoản đang đăng nhập có phải là Khách hàng (Thành viên) hay không.
     */
    public static boolean isCustomer(Context context) {
        return getMaQuyen(context) == 4;
    }

    /**
     * Lấy mã số định danh (ID) của nhân viên hoặc khách hàng đang đăng nhập.
     */
    public static int getMaNV(Context context) {
        return getPrefs(context).getInt(KEY_MANV, 0);
    }

    /**
     * Kiểm tra nhanh xem người dùng đã thực hiện đăng nhập vào ứng dụng hay chưa.
     * @return true nếu đã đăng nhập (có ID và mã quyền hợp lệ), false nếu chưa.
     */
    public static boolean isLoggedIn(Context context) {
        return getMaNV(context) != 0 && getMaQuyen(context) != 0;
    }

    /**
     * Ghi nhận và lưu lại thông tin phiên đăng nhập mới lên thiết bị.
     *
     * @param maquyen Mã vai trò/chức vụ.
     * @param manv Mã số định danh tài khoản.
     * @param hoten Họ và tên hiển thị.
     * @param token Chuỗi token định danh bảo mật từ Server.
     */
    public static void saveSession(Context context, int maquyen, int manv, String hoten, String token) {
        getPrefs(context).edit()
                .putInt(KEY_MAQUYEN, maquyen)
                .putInt(KEY_MANV, manv)
                .putString(KEY_HOTEN, hoten)
                .putString(KEY_TOKEN, token)
                .apply();

        // Đồng bộ sang ApiClient để Interceptor đính kèm vào mọi lời gọi
        // API sau đó. Không có dòng này thì đăng nhập xong vẫn bị máy chủ
        // trả 401 ở mọi màn hình, vì yêu cầu không mang theo token.
        com.sinhvien.orderdrinkapp.Api.ApiClient.setAuth(manv, token);
    }

    /**
     * Quá tải (overload) phương thức saveSession không yêu cầu token bảo mật.
     */
    public static void saveSession(Context context, int maquyen, int manv, String hoten) {
        saveSession(context, maquyen, manv, hoten, "");
    }

    /**
     * Cập nhật thông tin hồ sơ (Họ tên, SĐT, Email, URL Avatar) trong SharedPreferences.
     */
    public static void updateProfile(Context context, String hoten, String sdt, String email, String hinhAnh) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        if (hoten != null && !hoten.trim().isEmpty()) {
            editor.putString(KEY_HOTEN, hoten.trim());
        }
        if (sdt != null) {
            editor.putString(KEY_SDT, sdt.trim());
        }
        if (email != null) {
            editor.putString(KEY_EMAIL, email.trim());
        }
        if (hinhAnh != null) {
            editor.putString(KEY_HINHANH, hinhAnh.trim());
        }
        editor.apply();
    }

    /**
     * Lấy URL ảnh đại diện đã lưu của người dùng.
     */
    public static String getHinhAnh(Context context) {
        return getPrefs(context).getString(KEY_HINHANH, "");
    }

    /**
     * Xóa sạch toàn bộ thông tin phiên làm việc hiện tại khi người dùng thực hiện Đăng xuất.
     */
    public static void clearSession(Context context) {
        getPrefs(context).edit().clear().apply();
        com.sinhvien.orderdrinkapp.Api.ApiClient.clearAuth();

        // Ngắt Socket.IO tại ĐÂY — nơi phiên làm việc thật sự kết thúc.
        //
        // Trước đây việc này nằm trong onDestroy() của HomeActivity và
        // CustomerHomeActivity. Nhưng SocketManager là singleton dùng chung
        // toàn ứng dụng, còn onDestroy() thì chạy mỗi khi Android hủy một
        // Activity — kể cả lúc chỉ xoay màn hình hay tạo lại màn hình trong
        // điều hướng thông thường.
        //
        // Hậu quả đã quan sát được trên máy ảo: một instance HomeActivity
        // mới kết nối và vào room lúc 12:46:59, rồi instance CŨ bị hủy lúc
        // 12:47:00 và giết luôn socket mà instance mới đang dùng. Vì
        // disconnect() đặt cờ intentionalDisconnect = true nên cơ chế tự
        // kết nối lại cũng bị vô hiệu — realtime chết hẳn cho tới khi khởi
        // động lại ứng dụng, mà không có dấu hiệu gì trên giao diện.
        com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().disconnect();
    }

    /**
     * Nạp lại thông tin phiên từ SharedPreferences vào ApiClient.
     *
     * Cần gọi lúc ứng dụng khởi động: bộ nhớ đệm trong ApiClient là biến
     * tĩnh nên mất khi tiến trình bị hệ thống thu hồi, trong khi phiên vẫn
     * còn nguyên trong SharedPreferences. Thiếu bước này thì người dùng mở
     * lại app (không đăng nhập lại) sẽ gặp 401 ở mọi nơi.
     */
    public static void restoreAuth(Context context) {
        int manv = getMaNV(context);
        String token = getToken(context);
        if (manv > 0 && token != null && !token.isEmpty()) {
            com.sinhvien.orderdrinkapp.Api.ApiClient.setAuth(manv, token);
        } else {
            com.sinhvien.orderdrinkapp.Api.ApiClient.clearAuth();
        }
    }

    /**
     * Thiết lập cấu hình tuỳ chọn giao diện: Bật/tắt thanh điều hướng Bottom Navigation View.
     * @param value true: hiển thị Bottom Navigation, false: hiển thị Drawer Menu (Menu vuốt cạnh).
     */
    public static void setUseBottomNav(Context context, boolean value) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("use_bottom_nav", value).apply();
    }

    /**
     * Kiểm tra xem cấu hình giao diện hiện thời có sử dụng Bottom Navigation hay không.
     * Mặc định trả về false (sử dụng Drawer Navigation).
     */
    public static boolean isUseBottomNav(Context context) {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("use_bottom_nav", false);
    }

    /**
     * Lấy tham chiếu tới đối tượng SharedPreferences lưu dữ liệu phiên.
     */
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}

