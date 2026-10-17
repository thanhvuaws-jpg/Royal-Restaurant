package com.sinhvien.orderdrinkapp.Utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.sinhvien.orderdrinkapp.R;

import java.util.Locale;

/**
 * Thông báo nhanh bộ Sơn Son (QĐ-112, Figma .toast) thay cho Toast mặc định ở mọi màn:
 * viên sơn then viền vàng, huy hiệu nhỏ theo loại (✓ xong, ! cảnh báo, ✕ lỗi, i thông tin).
 *
 *     Toast.makeText(ctx, "Đã lưu", Toast.LENGTH_SHORT).show();
 *  -> ThongBao.hien(ctx, "Đã lưu", Toast.LENGTH_SHORT);
 *
 * Loại tự đoán theo câu chữ; muốn chỉ định thì gọi xong() / canh() / loi() / tin().
 * Gọi từ luồng nền cũng được (tự chuyển về luồng giao diện) — Toast gốc thì văng app.
 */
public final class ThongBao {

    private ThongBao() { }

    public static void hien(Context c, CharSequence noiDung) { hien(c, noiDung, Toast.LENGTH_SHORT); }

    public static void hien(Context c, @StringRes int res, int doDai) {
        if (c == null) return;
        hien(c, c.getText(res), doDai);
    }

    public static void hien(Context c, CharSequence noiDung, int doDai) {
        hien(c, noiDung, doDai, doanLoai(noiDung));
    }

    public static void xong(Context c, CharSequence s) { hien(c, s, Toast.LENGTH_SHORT, HopThoai.Loai.XONG); }
    public static void canh(Context c, CharSequence s) { hien(c, s, Toast.LENGTH_LONG, HopThoai.Loai.CANH); }
    public static void loi(Context c, CharSequence s) { hien(c, s, Toast.LENGTH_LONG, HopThoai.Loai.LOI); }
    public static void tin(Context c, CharSequence s) { hien(c, s, Toast.LENGTH_SHORT, HopThoai.Loai.TIN); }

    public static void hien(Context c, CharSequence noiDung, int doDai, HopThoai.Loai loai) {
        if (c == null || noiDung == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> hien(c, noiDung, doDai, loai));
            return;
        }
        View v = LayoutInflater.from(c).inflate(R.layout.thong_bao_nhanh, null);
        TextView dau = v.findViewById(R.id.txt_tb_dau);
        TextView chu = v.findViewById(R.id.txt_tb_noi_dung);
        chu.setText(noiDung);
        switch (loai) {
            case XONG: dau.setBackgroundResource(R.drawable.dau_tb_xong); dau.setText("✓"); dau.setTextColor(0xFFFFF3CF); break;
            case CANH: dau.setBackgroundResource(R.drawable.dau_tb_canh); dau.setText("!"); dau.setTextColor(0xFF3A1A05); break;
            case LOI:  dau.setBackgroundResource(R.drawable.dau_tb_loi);  dau.setText("✕"); dau.setTextColor(0xFFFFE3A0); break;
            default:   dau.setBackgroundResource(R.drawable.dau_tb_tin);  dau.setText("i"); dau.setTextColor(0xFFFFE3A0); break;
        }
        Toast t = new Toast(c.getApplicationContext());
        t.setView(v);
        t.setDuration(doDai == Toast.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        t.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, HopThoai.dp(c, 96));
        t.show();
    }

    /** Thứ tự kiểm tra có ý: "Đã xảy ra lỗi" là lỗi; "Vui lòng nhập…" là cảnh báo. */
    static HopThoai.Loai doanLoai(CharSequence s) {
        if (s == null) return HopThoai.Loai.TIN;
        String t = s.toString().toLowerCase(new Locale("vi"));
        if (HopThoai.chua(t, "lỗi", "thất bại", "không thể", "mất kết nối", "không kết nối", "error", "failed", "sai ", "không hợp lệ", "từ chối"))
            return HopThoai.Loai.LOI;
        if (HopThoai.chua(t, "vui lòng", "chưa ", "không đủ", "hết ", "vượt", "cảnh báo", "tối đa", "tối thiểu", "để trống", "hãy ", "cần "))
            return HopThoai.Loai.CANH;
        if (HopThoai.chua(t, "thành công", "đã ", "xong", "hoàn tất", "cảm ơn", "✅", "chúc mừng"))
            return HopThoai.Loai.XONG;
        return HopThoai.Loai.TIN;
    }
}
