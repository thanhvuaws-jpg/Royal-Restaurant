package com.sinhvien.orderdrinkapp.KhuonMat;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Activities.CustomerHomeActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.StaffResponse;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Đăng nhập bằng khuôn mặt (QĐ-103) — lựa chọn thứ hai ở màn đăng nhập, bên
 * cạnh gõ mật khẩu.
 *
 * Điều kiện để vào:
 *   • khuôn mặt khớp mẫu của một tài khoản đã đăng ký trên máy này với cosine
 *     ≥ {@link #NGUONG_KHOP} ở ít nhất {@link #SO_LAN_KHOP} trong
 *     {@link #CUA_SO} lần trích đặc trưng gần nhất (một khung lẻ may mắn không đủ).
 *     Mỗi tài khoản trên máy có mẫu riêng; khớp nhiều tài khoản thì hỏi chọn;
 *   • đã thấy chớp mắt thật (điểm chớp mắt = 1) — ảnh chụp không chớp mắt được.
 * Khi đủ cả hai, máy mới gửi khóa thiết bị (cất trên máy, mã hóa bằng khóa trong
 * Android Keystore) lên api/dang_nhap_khuon_mat.php để lấy phiên đăng nhập.
 *
 * Ngưỡng 0,65 chọn theo số đo trên 504 ảnh LFW (mẫu trung bình 5 ảnh): người lạ
 * lọt 0,17 %, người thật bị từ chối 3,5 % mỗi lần thử. Đăng nhập là chỗ cần chặt
 * — từ chối oan thì người dùng thử lại hoặc gõ mật khẩu, nhận nhầm thì lộ tài khoản.
 */
public class DangNhapKhuonMatActivity extends ManHinhCameraKhuonMat {

    static final float NGUONG_KHOP = 0.65f;
    static final int CUA_SO = 4, SO_LAN_KHOP = 2;
    static final long CACH_MS = 250;
    static final long HET_GIO_MS = 30_000;

    private List<KhoKhuonMat.TaiKhoan> cacTaiKhoan;
    private final KiemTraChopMat chopMat = new KiemTraChopMat();
    /** Điểm cosine của TỪNG tài khoản đã đăng ký, cho mỗi lần trích đặc trưng gần nhất. */
    private final ArrayDeque<float[]> ganDay = new ArrayDeque<>();
    private long lanTrichCuoi = 0, batDau = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cacTaiKhoan = KhoKhuonMat.docTatCa(this);
        if (cacTaiKhoan.isEmpty()) {
            baoLoiVaDong("Máy này chưa đăng ký khuôn mặt cho tài khoản nào. "
                    + "Hãy đăng nhập bằng mật khẩu rồi bật đăng nhập bằng khuôn mặt.");
            return;
        }
        txtTieuDe.setText("Đăng nhập bằng khuôn mặt");
        btnPhu.setText("Dùng mật khẩu");
        btnPhu.setOnClickListener(v -> finish());
    }

    @Override
    protected void khiCameraSanSang() {
        batDau = 0;
        hien("Nhìn vào camera, nhắm mắt lại rồi mở ra", null, MAU_CHO);
    }

    @Override
    protected void xuLyKhung(MoHinhKhuonMat.KhuonMat km, Bitmap anh, long t) throws Exception {
        if (batDau == 0) batDau = t;
        if (t - batDau > HET_GIO_MS) {
            dungPhanTich = true;
            runOnUiThread(this::hetGio);
            return;
        }
        if (km == null) {
            chopMat.khongThayMat(t);
            ganDay.clear();
            hien("Đưa khuôn mặt vào khung tròn", null, MAU_CHO);
            return;
        }
        chopMat.capNhat(km.ear, t);
        if (!km.datChatLuong()) {
            hien(km.loi, null, MAU_LOI);
            return;
        }

        if (t - lanTrichCuoi >= CACH_MS) {
            lanTrichCuoi = t;
            float[] e = moHinh.dacTrung(anh, km.namDiem);
            float[] diem = new float[cacTaiKhoan.size()];
            for (int i = 0; i < diem.length; i++) diem[i] = MoHinhKhuonMat.cosine(e, cacTaiKhoan.get(i).mau);
            ganDay.addLast(diem);
            while (ganDay.size() > CUA_SO) ganDay.removeFirst();
        }

        List<KhoKhuonMat.TaiKhoan> khop = taiKhoanKhop();
        float song = chopMat.diem(t);

        if (!khop.isEmpty() && song >= 1f) {
            dungPhanTich = true;
            if (khop.size() == 1) {
                KhoKhuonMat.TaiKhoan tk = khop.get(0);
                hien("Xin chào " + tk.hoTen, "Đang đăng nhập...", MAU_DAT);
                runOnUiThread(() -> dangNhap(tk));
            } else {
                hien("Khuôn mặt này có " + khop.size() + " tài khoản", "Chọn tài khoản để vào", MAU_DAT);
                runOnUiThread(() -> chonTaiKhoan(khop));
            }
        } else if (!khop.isEmpty()) {
            hien("Đã nhận ra " + khop.get(0).hoTen, "Nhắm mắt lại rồi mở ra để vào", MAU_DAT);
        } else if (song == 0f && chopMat.matDungIm()) {
            hien("Không thấy chớp mắt", "Ảnh chụp không dùng được — hãy nhìn thẳng và chớp mắt", MAU_LOI);
        } else if (ganDay.size() >= CUA_SO) {
            hien("Chưa khớp khuôn mặt đã đăng ký", "Nhìn thẳng, đủ sáng, không đeo khẩu trang", MAU_LOI);
        } else {
            hien("Nhìn vào camera, nhắm mắt lại rồi mở ra", null, MAU_CHO);
        }
    }

    /**
     * Các tài khoản có cosine ≥ NGUONG_KHOP ở ít nhất SO_LAN_KHOP trong CUA_SO lần gần
     * nhất, xếp theo điểm trung bình giảm dần. Một người có thể đăng ký cùng khuôn mặt
     * cho nhiều tài khoản trên một máy (vd vừa là nhân viên vừa là khách) — khi đó
     * KHÔNG tự chọn hộ mà hỏi người dùng muốn vào tài khoản nào.
     */
    private List<KhoKhuonMat.TaiKhoan> taiKhoanKhop() {
        List<KhoKhuonMat.TaiKhoan> ds = new java.util.ArrayList<>();
        java.util.List<float[]> diemTb = new java.util.ArrayList<>();
        for (int i = 0; i < cacTaiKhoan.size(); i++) {
            int dem = 0;
            float tong = 0;
            for (float[] g : ganDay) {
                if (i < g.length && g[i] >= NGUONG_KHOP) dem++;
                if (i < g.length) tong += g[i];
            }
            if (dem >= SO_LAN_KHOP) diemTb.add(new float[]{i, tong / ganDay.size()});
        }
        diemTb.sort((a, b) -> Float.compare(b[1], a[1]));
        for (float[] d : diemTb) ds.add(cacTaiKhoan.get((int) d[0]));
        return ds;
    }

    /**
     * Tấm chọn tài khoản trượt từ dưới lên: mỗi tài khoản một thẻ (chữ cái đầu tên,
     * họ tên, vai trò, tên đăng nhập). Vuốt xuống / bấm ra ngoài / "Quét lại" thì quét lại.
     */
    private void chonTaiKhoan(List<KhoKhuonMat.TaiKhoan> khop) {
        if (isFinishing() || isDestroyed()) return;
        BottomSheetDialog tam = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.tam_chon_tai_khoan_km, null);
        ((TextView) v.findViewById(R.id.txt_ctk_phu_de)).setText("Khuôn mặt này đã đăng ký cho "
                + khop.size() + " tài khoản trên máy. Chọn tài khoản bạn muốn vào.");

        LinearLayout ds = v.findViewById(R.id.ds_ctk);
        boolean[] daChon = {false};   // chặn bấm hai thẻ liền nhau
        for (KhoKhuonMat.TaiKhoan tk : khop) {
            View the = getLayoutInflater().inflate(R.layout.the_tai_khoan_km, ds, false);
            ((TextView) the.findViewById(R.id.txt_ttk_chu_dau)).setText(chuDau(tk.hoTen));
            ((TextView) the.findViewById(R.id.txt_ttk_ten)).setText(tk.hoTen);
            TextView quyen = the.findViewById(R.id.txt_ttk_quyen);
            int mau = mauQuyen(tk.maQuyen);
            quyen.setText(tenQuyen(tk.maQuyen));
            quyen.setTextColor(mau);
            quyen.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.setAlphaComponent(mau, 0x33)));
            TextView tenDN = the.findViewById(R.id.txt_ttk_ten_dn);
            if (tk.tenDN == null || tk.tenDN.isEmpty()) tenDN.setVisibility(View.GONE);
            else tenDN.setText("@" + tk.tenDN);
            the.setOnClickListener(x -> {
                if (daChon[0]) return;
                daChon[0] = true;
                tam.dismiss();
                hien("Xin chào " + tk.hoTen, "Đang đăng nhập...", MAU_DAT);
                dangNhap(tk);
            });
            ds.addView(the);
        }
        v.findViewById(R.id.btn_ctk_quet_lai).setOnClickListener(x -> tam.cancel());
        v.findViewById(R.id.btn_ctk_mat_khau).setOnClickListener(x -> { tam.dismiss(); finish(); });

        tam.setContentView(v);
        tam.setOnCancelListener(d -> batDauLai());
        tam.setOnShowListener(d -> {
            // Bỏ nền trắng mặc định của khung để thấy góc bo của tấm; mở hết cỡ ngay.
            View khung = tam.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (khung == null) return;
            khung.setBackgroundColor(Color.TRANSPARENT);
            BottomSheetBehavior<View> hanhVi = BottomSheetBehavior.from(khung);
            hanhVi.setSkipCollapsed(true);
            hanhVi.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        tam.show();
    }

    /** Chữ cái đầu của TÊN (từ cuối họ tên kiểu Việt): "Nguyễn Văn An" -> "A". */
    static String chuDau(String hoTen) {
        if (hoTen == null || hoTen.trim().isEmpty()) return "?";
        String[] tu = hoTen.trim().split("\\s+");
        String ten = tu[tu.length - 1];
        return new String(Character.toChars(ten.codePointAt(0))).toUpperCase(new Locale("vi"));
    }

    private static String tenQuyen(int q) {
        switch (q) {
            case 1: return "Quản lý";
            case 2: return "Nhân viên phục vụ";
            case 3: return "Thu ngân";
            default: return "Khách hàng";
        }
    }

    private static int mauQuyen(int q) {
        switch (q) {
            case 1: return 0xFFE57373;   // đỏ nhạt
            case 2: return 0xFF81C784;   // xanh lá
            case 3: return 0xFF64B5F6;   // xanh dương
            default: return 0xFFD4AF37;  // vàng — khách hàng
        }
    }

    private void dangNhap(KhoKhuonMat.TaiKhoan tk) {
        if (isFinishing() || isDestroyed()) return;
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.dangNhapKhuonMat(tk.maNV, tk.maKhoa, tk.khoa).enqueue(new Callback<StaffResponse>() {
            @Override
            public void onResponse(Call<StaffResponse> call, Response<StaffResponse> r) {
                if (isFinishing() || isDestroyed()) return;
                StaffResponse b = r.body();
                if (r.isSuccessful() && b != null && "success".equals(b.getStatus())) {
                    SessionManager.saveSession(DangNhapKhuonMatActivity.this,
                            b.getMaQuyen(), b.getMaNV(), b.getHoTenNV(), b.getToken());
                    Intent i = new Intent(DangNhapKhuonMatActivity.this,
                            b.getMaQuyen() == 4 ? CustomerHomeActivity.class : HomeActivity.class);
                    i.putExtra("tendn", b.getTenDN());
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                } else if (r.code() == 401) {
                    // Khóa đã bị thu hồi (đổi mật khẩu, tắt ở máy khác, xóa tài khoản): bỏ dữ liệu cũ.
                    KhoKhuonMat.xoa(DangNhapKhuonMatActivity.this, tk.maNV);
                    baoLoiVaDong(ViewUtils.docLoiMayChu(r, "Đăng nhập bằng khuôn mặt không còn hiệu lực. "
                            + "Hãy đăng nhập bằng mật khẩu."));
                } else {
                    thuLai(ViewUtils.docLoiMayChu(r, "Không đăng nhập được (mã " + r.code() + ")."));
                }
            }

            @Override
            public void onFailure(Call<StaffResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                thuLai("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void hetGio() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("Chưa nhận ra khuôn mặt")
                .setMessage("Hãy nhìn thẳng vào camera ở chỗ đủ sáng và chớp mắt. "
                        + "Hoặc đăng nhập bằng mật khẩu.")
                .setCancelable(false)
                .setPositiveButton("Thử lại", (d, w) -> batDauLai())
                .setNegativeButton("Dùng mật khẩu", (d, w) -> finish())
                .show();
    }

    private void thuLai(String lyDo) {
        new AlertDialog.Builder(this)
                .setTitle("Chưa đăng nhập được")
                .setMessage(lyDo)
                .setCancelable(false)
                .setPositiveButton("Thử lại", (d, w) -> batDauLai())
                .setNegativeButton("Dùng mật khẩu", (d, w) -> finish())
                .show();
    }

    private void batDauLai() {
        ganDay.clear();
        chopMat.datLai();
        batDau = 0;
        hien("Nhìn vào camera, nhắm mắt lại rồi mở ra", null, MAU_CHO);
        tiepTucPhanTich();
    }
}
