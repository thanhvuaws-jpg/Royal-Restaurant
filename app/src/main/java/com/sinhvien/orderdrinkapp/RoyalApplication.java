package com.sinhvien.orderdrinkapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.sinhvien.orderdrinkapp.Utils.BaoTri;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.Utils.ThongBaoHeThong;

import java.lang.ref.WeakReference;

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
 *
 * Từ QĐ-107 còn theo dõi Activity đang hiện, để bảo trì và thông báo hệ thống
 * (đến từ tầng mạng/socket, không có Activity trong tay) biết hiện ở đâu.
 */
public class RoyalApplication extends Application {

    private static RoyalApplication instance;
    private static WeakReference<Activity> dangHien = new WeakReference<>(null);

    /** Context của ứng dụng, cho những chỗ không có Activity (tầng HTTP). */
    public static android.content.Context layContext() {
        return instance;
    }

    /** Activity đang ở tiền cảnh, null khi app nằm nền. */
    public static Activity activityDangHien() {
        return dangHien.get();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SessionManager.restoreAuth(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) {
                dangHien = new WeakReference<>(a);
                BaoTri.moNeuCan(a);
                ThongBaoHeThong.hienNeuCo(a);
            }
            @Override public void onActivityPaused(Activity a) {
                if (dangHien.get() == a) dangHien = new WeakReference<>(null);
            }
            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { }
        });
    }
}
