package com.sinhvien.orderdrinkapp.Utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;

import com.sinhvien.orderdrinkapp.RoyalApplication;

import org.json.JSONObject;

/**
 * ThongBaoHeThong — hiện thông báo do bảng điều khiển máy chủ gửi qua socket
 * "thong_bao_he_thong" (QĐ-107), ví dụ "Bếp quá tải" hay "Sắp bảo trì".
 *
 * App đang nằm nền thì giữ lại thông báo cuối, hiện khi người dùng quay lại
 * (trong vòng 10 phút — cũ hơn thì không còn ý nghĩa).
 */
public final class ThongBaoHeThong {
    private static final long GIU_MS = 10 * 60 * 1000;
    private static volatile JSONObject choHien = null;
    private static volatile long choHienLuc = 0;

    private ThongBaoHeThong() { }

    public static void tuSocket(Object goi) {
        if (!(goi instanceof JSONObject)) return;
        JSONObject o = (JSONObject) goi;
        if (o.optString("noi_dung", "").isEmpty()) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            Activity a = RoyalApplication.activityDangHien();
            if (a != null && !a.isFinishing()) {
                hien(a, o);
            } else {
                choHien = o;
                choHienLuc = System.currentTimeMillis();
            }
        });
    }

    /** RoyalApplication gọi khi một Activity hiện lên. */
    public static void hienNeuCo(Activity a) {
        JSONObject o = choHien;
        if (o == null) return;
        choHien = null;
        if (System.currentTimeMillis() - choHienLuc < GIU_MS) hien(a, o);
    }

    private static void hien(Activity a, JSONObject o) {
        boolean canhBao = "canh_bao".equals(o.optString("muc_do"));
        new AlertDialog.Builder(a)
                .setTitle(canhBao ? "⚠️ Cảnh báo từ quản trị hệ thống" : "📢 Thông báo hệ thống")
                .setMessage(o.optString("noi_dung"))
                .setPositiveButton("Đã hiểu", null)
                .show();
    }
}
