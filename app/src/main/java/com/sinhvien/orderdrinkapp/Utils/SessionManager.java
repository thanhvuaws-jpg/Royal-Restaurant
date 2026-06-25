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
    }

    /**
     * Quá tải (overload) phương thức saveSession không yêu cầu token bảo mật.
     */
    public static void saveSession(Context context, int maquyen, int manv, String hoten) {
        saveSession(context, maquyen, manv, hoten, "");
    }

    /**
     * Xóa sạch toàn bộ thông tin phiên làm việc hiện tại khi người dùng thực hiện Đăng xuất.
     */
    public static void clearSession(Context context) {
        getPrefs(context).edit().clear().apply();
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

