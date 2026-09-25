package com.sinhvien.orderdrinkapp;

import android.app.Application;

import com.sinhvien.orderdrinkapp.Utils.SessionManager;

/**
 * RoyalApplication — chạy TRƯỚC mọi Activity mỗi khi tiến trình khởi động.
 *
 * VÌ SAO CẦN (QĐ-096, lỗi H4)
 * ---------------------------
 * Phiên đăng nhập (manv + token) nằm trong SharedPreferences, còn ApiClient
 * giữ bản sao trong biến tĩnh để gắn vào header. Biến tĩnh mất khi hệ thống
 * thu hồi tiến trình (máy thiếu RAM, app nằm nền lâu). Lúc người dùng quay
 * lại, Android khôi phục THẲNG màn đang mở dở (HomeActivity, màn gọi món…),
 * không qua SplashActivity — nơi duy nhất trước đây nạp lại phiên. Kết quả:
 * mọi lời gọi sau đó nhận 401, bấm "Xóa" không xóa, danh sách trống, mà
 * không có thông báo gì rõ ràng. Đã tái hiện trên máy ảo ngày 24/09.
 *
 * Application.onCreate() luôn chạy trước khi bất kỳ Activity nào được tạo
 * lại, nên nạp phiên ở đây là đủ cho mọi đường vào app.
 */
public class RoyalApplication extends Application {

    private static RoyalApplication instance;

    /** Context của ứng dụng, cho những chỗ không có Activity (tầng HTTP). */
    public static android.content.Context layContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SessionManager.restoreAuth(this);
    }
}
