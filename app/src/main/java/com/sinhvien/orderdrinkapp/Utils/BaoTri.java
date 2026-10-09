package com.sinhvien.orderdrinkapp.Utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.sinhvien.orderdrinkapp.Activities.BaoTriActivity;
import com.sinhvien.orderdrinkapp.RoyalApplication;

import org.json.JSONObject;

/**
 * BaoTri — chuyển app sang màn bảo trì khi máy chủ đang bảo trì (QĐ-107).
 *
 * Hai đường báo, cùng đổ về đây:
 *   1. Caddy trả 503 kèm header X-Bao-Tri cho MỌI lời gọi API (ApiClient bắt).
 *   2. Bảng điều khiển phát sự kiện socket "bao_tri" lúc bật/tắt
 *      (SocketManager) — màn đang mở chuyển ngay, không đợi lần gọi API sau.
 *
 * Chỉ ghi nhận TRẠNG THÁI rồi mở màn bảo trì từ Activity đang hiện. Android 10+
 * không cho mở Activity khi app nằm nền; lúc đó RoyalApplication sẽ chuyển
 * sang màn bảo trì ngay khi người dùng quay lại app (xem moNeuCan).
 */
public final class BaoTri {
    private static volatile boolean dangBaoTri = false;
    private static volatile String thongBao = "";
    private static volatile String duKien = "";
    /** Màn bảo trì đang hiện đăng ký vào đây để socket "tắt bảo trì" gọi nó kiểm tra lại. */
    private static volatile Runnable ngheKetThuc = null;

    private BaoTri() { }

    public static boolean dangBaoTri() { return dangBaoTri; }
    public static String thongBao() { return thongBao; }
    public static String duKien() { return duKien; }

    /** Từ thân JSON của phản hồi 503 (gọi trên luồng mạng). */
    public static void tuPhanHoi(String json) {
        try {
            JSONObject o = new JSONObject(json);
            baoHieu(o.optString("thong_bao", o.optString("message", "")), o.optString("du_kien", ""));
        } catch (Exception e) {
            baoHieu("", "");
        }
    }

    /** Từ sự kiện socket "bao_tri" {bat, thong_bao, du_kien}. */
    public static void tuSocket(Object goi) {
        if (!(goi instanceof JSONObject)) return;
        JSONObject o = (JSONObject) goi;
        if (o.optBoolean("bat", false)) {
            baoHieu(o.optString("thong_bao", ""), o.isNull("du_kien") ? "" : o.optString("du_kien", ""));
        } else {
            Runnable r = ngheKetThuc;
            if (r != null) new Handler(Looper.getMainLooper()).post(r);
        }
    }

    private static void baoHieu(String tb, String dk) {
        thongBao = tb == null ? "" : tb;
        duKien = dk == null || "null".equals(dk) ? "" : dk;
        dangBaoTri = true;
        new Handler(Looper.getMainLooper()).post(() -> {
            Activity a = RoyalApplication.activityDangHien();
            if (a != null) moNeuCan(a);
        });
    }

    /** Gọi mỗi khi một Activity hiện lên (RoyalApplication) — đang bảo trì thì chuyển màn. */
    public static void moNeuCan(Activity a) {
        if (!dangBaoTri || a instanceof BaoTriActivity || a.isFinishing()) return;
        Intent i = new Intent(a, BaoTriActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
    }

    /** Máy chủ đã mở lại. */
    public static void ketThuc() {
        dangBaoTri = false;
    }

    public static void datNgheKetThuc(Runnable r) {
        ngheKetThuc = r;
    }
}
