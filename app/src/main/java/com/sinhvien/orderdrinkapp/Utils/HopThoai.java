package com.sinhvien.orderdrinkapp.Utils;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.sinhvien.orderdrinkapp.R;

import java.util.Locale;

/**
 * Hộp thoại bộ giao diện Sơn Son (QĐ-112, Figma trang 03 "Hộp thoại") thay cho hộp
 * thoại mặc định của Android ở MỌI màn hình, mọi vai.
 *
 * Builder kế thừa AlertDialog.Builder nên chỗ gọi chỉ đổi đúng một chữ:
 *
 *     new AlertDialog.Builder(ctx)  ->  new HopThoai.Builder(ctx)
 *
 * còn setTitle / setMessage / setPositiveButton / setItems / setView / show… giữ nguyên.
 * create() trả về một AlertDialog thật (dismiss, isShowing… vẫn dùng được) nhưng
 * nội dung là layout hop_thoai_son_son: huy hiệu nổi trên mép, tiêu đề sơn son,
 * nút chính sơn son, nút phụ viền vàng, nút thứ ba dạng chữ.
 *
 * Huy hiệu: tự chọn theo nội dung (lỗi / cảnh báo / xong / thông tin) nếu chỗ gọi
 * không nói; setLoai(...) hoặc setHuyHieu(R.drawable.ic_hh_...) để chỉ định.
 */
public final class HopThoai {

    public enum Loai { XONG, CANH, LOI, TIN }

    private HopThoai() { }

    public static class Builder extends AlertDialog.Builder {

        private CharSequence tieuDe, noiDung;
        private CharSequence chuChinh, chuPhu, chuThuBa;
        private DialogInterface.OnClickListener ngheChinh, nghePhu, ngheThuBa;
        private CharSequence[] cacMuc;
        private DialogInterface.OnClickListener ngheMuc;
        private View viewRieng;
        private int layoutRieng;
        private boolean huyDuoc = true;
        private DialogInterface.OnDismissListener ngheDong;
        private DialogInterface.OnCancelListener ngheHuy;
        private Loai loai;
        private int huyHieu;
        private boolean anHuyHieu;

        public Builder(@NonNull Context context) {
            super(context);
        }

        public Builder(@NonNull Context context, int themeResId) {
            super(context, themeResId);
        }

        // ── Mở rộng riêng của bộ Sơn Son ───────────────────────────────────

        /** Chọn huy hiệu trạng thái (✓ ! ✕ i) thay vì để tự đoán theo nội dung. */
        public Builder setLoai(Loai loai) { this.loai = loai; return this; }

        /** Dùng một huy hiệu hình của bộ 12 menu (R.drawable.ic_hh_...). */
        public Builder setHuyHieu(@DrawableRes int res) { this.huyHieu = res; return this; }

        /** Không hiện huy hiệu (vd hộp "Đang xử lý…"). */
        public Builder anHuyHieu() { this.anHuyHieu = true; return this; }

        // ── API giống AlertDialog.Builder ──────────────────────────────────

        @Override public Builder setTitle(@Nullable CharSequence t) { tieuDe = t; return this; }
        @Override public Builder setTitle(int res) { tieuDe = getContext().getText(res); return this; }
        @Override public Builder setMessage(@Nullable CharSequence m) { noiDung = m; return this; }
        @Override public Builder setMessage(int res) { noiDung = getContext().getText(res); return this; }

        @Override public Builder setPositiveButton(CharSequence chu, DialogInterface.OnClickListener l) {
            chuChinh = chu; ngheChinh = l; return this;
        }
        @Override public Builder setPositiveButton(int res, DialogInterface.OnClickListener l) {
            return setPositiveButton(getContext().getText(res), l);
        }
        @Override public Builder setNegativeButton(CharSequence chu, DialogInterface.OnClickListener l) {
            chuPhu = chu; nghePhu = l; return this;
        }
        @Override public Builder setNegativeButton(int res, DialogInterface.OnClickListener l) {
            return setNegativeButton(getContext().getText(res), l);
        }
        @Override public Builder setNeutralButton(CharSequence chu, DialogInterface.OnClickListener l) {
            chuThuBa = chu; ngheThuBa = l; return this;
        }
        @Override public Builder setNeutralButton(int res, DialogInterface.OnClickListener l) {
            return setNeutralButton(getContext().getText(res), l);
        }

        @Override public Builder setItems(CharSequence[] muc, DialogInterface.OnClickListener l) {
            cacMuc = muc; ngheMuc = l; return this;
        }
        @Override public Builder setItems(int res, DialogInterface.OnClickListener l) {
            return setItems(getContext().getResources().getTextArray(res), l);
        }

        @Override public Builder setView(View v) { viewRieng = v; layoutRieng = 0; return this; }
        @Override public Builder setView(int layout) { layoutRieng = layout; viewRieng = null; return this; }
        @Override public Builder setCancelable(boolean b) { huyDuoc = b; return this; }
        @Override public Builder setOnDismissListener(DialogInterface.OnDismissListener l) { ngheDong = l; return this; }
        @Override public Builder setOnCancelListener(DialogInterface.OnCancelListener l) { ngheHuy = l; return this; }

        /** Biểu tượng kiểu cũ: nếu là huy hiệu ic_hh_* thì dùng, còn lại để tự đoán loại. */
        @Override public Builder setIcon(int res) {
            try {
                if (res != 0 && getContext().getResources().getResourceEntryName(res).startsWith("ic_hh_")) {
                    huyHieu = res;
                }
            } catch (Exception ignored) { }
            return this;
        }
        @Override public Builder setIcon(@Nullable Drawable d) { return this; }

        @NonNull
        @Override
        public AlertDialog create() {
            Context ctx = getContext();
            View goc = LayoutInflater.from(ctx).inflate(R.layout.hop_thoai_son_son, null);
            AlertDialog hop = new AlertDialog.Builder(ctx).setView(goc).setCancelable(huyDuoc).create();
            if (hop.getWindow() != null) {
                hop.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            if (ngheDong != null) hop.setOnDismissListener(ngheDong);
            if (ngheHuy != null) hop.setOnCancelListener(ngheHuy);

            TextView txtTieuDe = goc.findViewById(R.id.txt_hop_tieu_de);
            TextView txtNoiDung = goc.findViewById(R.id.txt_hop_noi_dung);
            ganChu(txtTieuDe, tieuDe);
            ganChu(txtNoiDung, noiDung);

            // Không có tiêu đề: hộp gọn, khỏi chừa chỗ huy hiệu ở đầu.
            View khung = goc.findViewById(R.id.khung_hop);
            boolean coHuyHieu = !anHuyHieu && (tieuDe != null || huyHieu != 0 || loai != null);
            if (!coHuyHieu) {
                ((ViewGroup.MarginLayoutParams) khung.getLayoutParams()).topMargin = dp(ctx, 8);
                khung.setPadding(khung.getPaddingLeft(), dp(ctx, 18), khung.getPaddingRight(), khung.getPaddingBottom());
            } else {
                datHuyHieu(goc);
            }

            // View riêng (ô nhập, vòng xoay chờ…)
            View vr = viewRieng;
            if (vr == null && layoutRieng != 0) vr = LayoutInflater.from(ctx).inflate(layoutRieng, null);
            if (vr != null) {
                ViewGroup chua = goc.findViewById(R.id.khung_hop_view);
                if (vr.getParent() instanceof ViewGroup) ((ViewGroup) vr.getParent()).removeView(vr);
                chua.addView(vr);
                chua.setVisibility(View.VISIBLE);
            }

            // Danh sách lựa chọn
            if (cacMuc != null && cacMuc.length > 0) {
                ScrollView cuon = goc.findViewById(R.id.cuon_hop_muc);
                LinearLayout ds = goc.findViewById(R.id.ds_hop_muc);
                cuon.setVisibility(View.VISIBLE);
                for (int i = 0; i < cacMuc.length; i++) {
                    TextView dong = (TextView) LayoutInflater.from(ctx).inflate(R.layout.dong_chon_hop, ds, false);
                    dong.setText(cacMuc[i]);
                    final int viTri = i;
                    dong.setOnClickListener(v -> {
                        if (ngheMuc != null) ngheMuc.onClick(hop, viTri);
                        hop.dismiss();
                    });
                    ds.addView(dong);
                }
                if (cacMuc.length > 6) {
                    cuon.getLayoutParams().height = dp(ctx, 6 * 58);
                }
            }

            // Hàng nút: thứ ba (chữ) | phụ (viền) | chính (sơn son), như Figma.
            LinearLayout hang = goc.findViewById(R.id.hang_nut_hop);
            themNut(hang, R.layout.nut_hop_chu, chuThuBa, ngheThuBa, hop, DialogInterface.BUTTON_NEUTRAL);
            themNut(hang, R.layout.nut_hop_phu, chuPhu, nghePhu, hop, DialogInterface.BUTTON_NEGATIVE);
            themNut(hang, R.layout.nut_hop_chinh, chuChinh, ngheChinh, hop, DialogInterface.BUTTON_POSITIVE);
            if (hang.getChildCount() > 0) hang.setVisibility(View.VISIBLE);
            return hop;
        }

        @Override
        public AlertDialog show() {
            AlertDialog hop = create();
            hop.show();
            return hop;
        }

        private void themNut(LinearLayout hang, int layout, CharSequence chu,
                             DialogInterface.OnClickListener nghe, AlertDialog hop, int loaiNut) {
            if (chu == null) return;
            MaterialButton nut = (MaterialButton) LayoutInflater.from(hang.getContext()).inflate(layout, hang, false);
            nut.setText(chu);
            if (hang.getChildCount() > 0) {
                ((LinearLayout.LayoutParams) nut.getLayoutParams()).setMarginStart(dp(hang.getContext(), 10));
            }
            nut.setOnClickListener(v -> {
                if (nghe != null) nghe.onClick(hop, loaiNut);
                hop.dismiss();
            });
            hang.addView(nut);
        }

        private void datHuyHieu(View goc) {
            if (huyHieu != 0) {
                ImageView img = goc.findViewById(R.id.img_huy_hieu_hop);
                img.setImageResource(huyHieu);
                img.setVisibility(View.VISIBLE);
                return;
            }
            Loai l = loai != null ? loai : doanLoai(tieuDe, noiDung);
            TextView dau = goc.findViewById(R.id.txt_huy_hieu_hop);
            switch (l) {
                case XONG: dau.setBackgroundResource(R.drawable.dau_hop_xong); dau.setText("✓"); dau.setTextColor(0xFFFFF3CF); break;
                case CANH: dau.setBackgroundResource(R.drawable.dau_hop_canh); dau.setText("!"); dau.setTextColor(0xFF3A1A05); break;
                case LOI:  dau.setBackgroundResource(R.drawable.dau_hop_loi);  dau.setText("✕"); dau.setTextColor(0xFFFFE3A0); break;
                default:   dau.setBackgroundResource(R.drawable.dau_hop_tin);  dau.setText("i"); dau.setTextColor(0xFFFFE3A0); break;
            }
            dau.setVisibility(View.VISIBLE);
        }

        private static void ganChu(TextView tv, CharSequence chu) {
            if (chu == null || chu.length() == 0) {
                tv.setVisibility(View.GONE);
            } else {
                tv.setText(chu);
            }
        }
    }

    /**
     * Đoán loại huy hiệu từ chữ của hộp thoại. Thứ tự có ý: "Đã xảy ra lỗi" là LỖI
     * dù có chữ "đã"; câu hỏi xác nhận ("Xóa món?") không bao giờ là XONG.
     */
    static Loai doanLoai(CharSequence tieuDe, CharSequence noiDung) {
        String t = ((tieuDe == null ? "" : tieuDe) + " " + (noiDung == null ? "" : noiDung))
                .toLowerCase(new Locale("vi"));
        String td = tieuDe == null ? "" : tieuDe.toString().toLowerCase(new Locale("vi"));
        if (chua(t, "lỗi", "thất bại", "không thể", "mất kết nối", "không kết nối", "từ chối", "bị khóa")) return Loai.LOI;
        if (chua(td, "xóa", "hủy", "khóa ", "đuổi")) return Loai.LOI;
        if (chua(t, "cảnh báo", "chú ý", "lưu ý", "chưa ", "không đủ", "hết hạn", "đã hết", "vượt", "tối đa", "trùng")) return Loai.CANH;
        if (!td.contains("?") && chua(t, "thành công", "hoàn tất", "đã lưu", "đã gửi", "đã đổi", "đã nhận", "đã đặt", "chúc mừng", "xong")) return Loai.XONG;
        return Loai.TIN;
    }

    static boolean chua(String s, String... tu) {
        for (String x : tu) if (s.contains(x)) return true;
        return false;
    }

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
