package com.sinhvien.orderdrinkapp.Utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Quản lý phiên đăng nhập tập trung.
 */
public class SessionManager {

    private static final String PREF_NAME = "luuquyen";
    private static final String KEY_MAQUYEN = "maquyen";
    private static final String KEY_MANV = "manv";
    private static final String KEY_HOTEN = "hoten";
    private static final String KEY_TOKEN = "session_token";

    /** Lấy session token của người đang đăng nhập */
    public static String getToken(Context context) {
        return getPrefs(context).getString(KEY_TOKEN, "");
    }

    /** Lấy tên đầy đủ của người đang đăng nhập */
    public static String getFullName(Context context) {
        return getPrefs(context).getString(KEY_HOTEN, "");
    }

    /** Lấy mã quyền của người đang đăng nhập (1 = Admin, 2 = Nhân viên) */
    public static int getMaQuyen(Context context) {
        return getPrefs(context).getInt(KEY_MAQUYEN, 0);
    }

    /** Kiểm tra người đang đăng nhập có phải Admin không */
    public static boolean isAdmin(Context context) {
        return getMaQuyen(context) == 1;
    }

    /** Kiểm tra người đang đăng nhập có phải Thu ngân không */
    public static boolean isCashier(Context context) {
        return getMaQuyen(context) == 3;
    }

    /** Lấy mã nhân viên đang đăng nhập */
    public static int getMaNV(Context context) {
        return getPrefs(context).getInt(KEY_MANV, 0);
    }

    /** Kiểm tra đã có phiên đăng nhập hợp lệ chưa */
    public static boolean isLoggedIn(Context context) {
        return getMaNV(context) != 0 && getMaQuyen(context) != 0;
    }

    /** Lưu thông tin phiên sau khi đăng nhập thành công */
    public static void saveSession(Context context, int maquyen, int manv, String hoten, String token) {
        getPrefs(context).edit()
                .putInt(KEY_MAQUYEN, maquyen)
                .putInt(KEY_MANV, manv)
                .putString(KEY_HOTEN, hoten)
                .putString(KEY_TOKEN, token)
                .apply();
    }

    public static void saveSession(Context context, int maquyen, int manv, String hoten) {
        saveSession(context, maquyen, manv, hoten, "");
    }

    /** Xóa phiên khi đăng xuất */
    public static void clearSession(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
