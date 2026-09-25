package com.sinhvien.orderdrinkapp.Utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.sinhvien.orderdrinkapp.Activities.LoginActivity;
import com.sinhvien.orderdrinkapp.RoyalApplication;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PhienHetHan — xử lý MỘT CHỖ khi máy chủ trả 401 cho một yêu cầu có mang
 * phiên đăng nhập.
 *
 * VÌ SAO (QĐ-096): phiên có thể bị hủy từ phía máy chủ (quản lý đổi mật khẩu
 * hay quyền của tài khoản, đăng nhập đè ở máy khác). App nhân viên có vòng
 * kiểm phiên 10 giây, app khách thì không có gì: mọi màn hình cứ thế nhận
 * 401 và hiện trống, không một lời giải thích. Bắt ở tầng HTTP thì mọi màn
 * hình, mọi vai đều được xử lý như nhau.
 *
 * Nhiều yêu cầu cùng nhận 401 một lúc (màn hình tải song song) chỉ xử lý
 * một lần; ApiClient.setAuth() mở lại cờ sau khi đăng nhập mới.
 */
public final class PhienHetHan {

    private static final AtomicBoolean daXuLy = new AtomicBoolean(false);

    private PhienHetHan() { }

    /** Gọi từ luồng mạng khi một yêu cầu có phiên nhận 401. */
    public static void baoHieu() {
        Context ctx = RoyalApplication.layContext();
        if (ctx == null || !daXuLy.compareAndSet(false, true)) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            // Người dùng có thể đã tự đăng xuất trong lúc chờ.
            if (!SessionManager.isLoggedIn(ctx)) return;
            SessionManager.clearSession(ctx);
            Toast.makeText(ctx, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            Intent i = new Intent(ctx, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ctx.startActivity(i);
        });
    }

    /** Đăng nhập (hoặc khôi phục phiên) xong thì cho phép xử lý lại lần sau. */
    public static void datLai() {
        daXuLy.set(false);
    }
}
