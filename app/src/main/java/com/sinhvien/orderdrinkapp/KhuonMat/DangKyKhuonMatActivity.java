package com.sinhvien.orderdrinkapp.KhuonMat;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.KhuonMatResponse;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Đăng ký khuôn mặt cho TÀI KHOẢN ĐANG ĐĂNG NHẬP (QĐ-103).
 *
 *   1. Mật khẩu: lấy từ màn đăng nhập (vừa gõ xong) hoặc hỏi lại.
 *   2. Lấy {@link #SO_MAU} khung đạt chất lượng, cách nhau ít nhất {@link #CACH_MS},
 *      trích đặc trưng từng khung. Mẫu = trung bình các vector (đã đo trên LFW:
 *      mẫu trung bình 5 ảnh hạ tỉ lệ từ chối người thật từ 21,9 % xuống 3,5 % ở
 *      ngưỡng 0,65 so với so từng cặp ảnh).
 *   3. Phải chớp mắt ít nhất một lần — không đăng ký được bằng ảnh chụp.
 *   4. Gửi mật khẩu lên máy chủ để nhận khóa thiết bị, cất cùng mẫu vào
 *      {@link KhoKhuonMat} (mã hóa bằng Android Keystore).
 *
 * Extra: "matkhau" (tùy chọn), "tendn" (tùy chọn, để hiện ở màn đăng nhập).
 */
public class DangKyKhuonMatActivity extends ManHinhCameraKhuonMat {

    public static final String EXTRA_MAT_KHAU = "matkhau";
    public static final String EXTRA_TEN_DN = "tendn";

    static final int SO_MAU = 6;
    static final long CACH_MS = 350;
    /** Khung mới lệch quá xa các khung đã lấy -> có thể đã đổi người, lấy lại từ đầu. */
    static final float NGUONG_NHAT_QUAN = 0.50f;

    private enum Buoc { LAY_MAU, CHO_CHOP_MAT, GUI, XONG }

    private volatile Buoc buoc = Buoc.LAY_MAU;
    private final List<float[]> cacMau = new ArrayList<>();
    private final KiemTraChopMat chopMat = new KiemTraChopMat();
    private long lanLayCuoi = 0;
    private String matKhau;
    private float[] mauCuoi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        txtTieuDe.setText("Đăng ký khuôn mặt");
        txtPhuDe.setText("Lần sau chỉ cần nhìn vào camera và chớp mắt để đăng nhập. "
                + "Ảnh khuôn mặt không rời khỏi máy này.");
        btnPhu.setText("Hủy");
        btnPhu.setOnClickListener(v -> finish());
        tienDo.setProgress(0);
        matKhau = getIntent().getStringExtra(EXTRA_MAT_KHAU);
        if (matKhau == null || matKhau.isEmpty()) hoiMatKhau(null);
    }

    @Override
    protected void khiCameraSanSang() {
        hien("Nhìn thẳng vào camera", "Giữ khuôn mặt trong khung tròn", MAU_CHO);
        datTienDo(0);
    }

    @Override
    protected void xuLyKhung(MoHinhKhuonMat.KhuonMat km, Bitmap anh, long t) throws Exception {
        if (buoc == Buoc.GUI || buoc == Buoc.XONG || matKhau == null) return;
        if (km == null) {
            chopMat.khongThayMat(t);
            hien("Đưa khuôn mặt vào khung tròn", null, MAU_CHO);
            return;
        }
        chopMat.capNhat(km.ear, t);
        if (!km.datChatLuong()) {
            hien(km.loi, cacMau.isEmpty() ? null : "Đã lấy " + cacMau.size() + "/" + SO_MAU, MAU_LOI);
            return;
        }

        if (buoc == Buoc.LAY_MAU) {
            if (t - lanLayCuoi < CACH_MS) return;
            lanLayCuoi = t;
            float[] e = moHinh.dacTrung(anh, km.namDiem);
            if (cacMau.size() >= 2 && MoHinhKhuonMat.cosine(e, trungBinh(cacMau)) < NGUONG_NHAT_QUAN) {
                cacMau.clear();
                datTienDo(0);
                hien("Chỉ một người nhìn vào camera", "Đang lấy lại từ đầu", MAU_LOI);
                return;
            }
            cacMau.add(e);
            datTienDo(cacMau.size() * 80 / SO_MAU);
            if (cacMau.size() < SO_MAU) {
                hien("Giữ yên, đang lấy mẫu khuôn mặt", "Đã lấy " + cacMau.size() + "/" + SO_MAU, MAU_DAT);
                return;
            }
            buoc = Buoc.CHO_CHOP_MAT;
        }

        if (buoc == Buoc.CHO_CHOP_MAT) {
            if (chopMat.diem(t) >= 1f) {
                datTienDo(100);
                buoc = Buoc.GUI;
                dungPhanTich = true;
                mauCuoi = trungBinh(cacMau);
                hien("Đã lấy xong mẫu khuôn mặt", "Đang lưu...", MAU_DAT);
                runOnUiThread(this::guiMayChu);
            } else {
                hien("Nhắm mắt lại rồi mở ra", "Để chứng minh đây là người thật, không phải ảnh chụp", MAU_CHO);
            }
        }
    }

    private static float[] trungBinh(List<float[]> ds) {
        float[] tb = new float[ds.get(0).length];
        for (float[] v : ds) for (int i = 0; i < tb.length; i++) tb[i] += v[i];
        return MoHinhKhuonMat.chuanHoa(tb);
    }

    private void guiMayChu() {
        if (isFinishing() || isDestroyed()) return;
        String tenMay = (Build.MANUFACTURER + " " + Build.MODEL).trim();
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.batKhuonMat("bat", matKhau, tenMay).enqueue(new Callback<KhuonMatResponse>() {
            @Override
            public void onResponse(Call<KhuonMatResponse> call, Response<KhuonMatResponse> r) {
                if (isFinishing() || isDestroyed()) return;
                KhuonMatResponse b = r.body();
                if (r.isSuccessful() && b != null && "success".equals(b.getStatus()) && b.getKhoa() != null) {
                    luuVaXong(b);
                } else if (r.code() == 403) {
                    hoiMatKhau("Mật khẩu không đúng. Nhập lại để hoàn tất:");
                } else {
                    thatBai(ViewUtils.docLoiMayChu(r, "Không bật được đăng nhập bằng khuôn mặt (mã " + r.code() + ")."));
                }
            }

            @Override
            public void onFailure(Call<KhuonMatResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                thatBai("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void luuVaXong(KhuonMatResponse b) {
        KhoKhuonMat.TaiKhoan tk = new KhoKhuonMat.TaiKhoan();
        tk.maNV = SessionManager.getMaNV(this);
        tk.hoTen = SessionManager.getFullName(this);
        tk.maQuyen = SessionManager.getMaQuyen(this);
        String tenDN = getIntent().getStringExtra(EXTRA_TEN_DN);
        tk.tenDN = tenDN != null ? tenDN : "";
        tk.maKhoa = b.getMaKhoa();
        tk.khoa = b.getKhoa();
        tk.mau = mauCuoi;
        tk.ngayDangKy = System.currentTimeMillis();
        try {
            KhoKhuonMat.luu(this, tk);
        } catch (Exception e) {
            thatBai("Không lưu được dữ liệu khuôn mặt trên máy: " + e.getMessage());
            return;
        }
        buoc = Buoc.XONG;
        matKhau = null;
        new AlertDialog.Builder(this)
                .setTitle("Đã bật đăng nhập bằng khuôn mặt")
                .setMessage("Lần sau ở màn đăng nhập, chọn \"Đăng nhập bằng khuôn mặt\", nhìn vào camera và chớp mắt.")
                .setCancelable(false)
                .setPositiveButton("Xong", (d, w) -> { setResult(RESULT_OK); finish(); })
                .show();
    }

    private void thatBai(String lyDo) {
        new AlertDialog.Builder(this)
                .setTitle("Chưa bật được")
                .setMessage(lyDo)
                .setPositiveButton("Thử lại", (d, w) -> guiMayChu())
                .setNegativeButton("Hủy", (d, w) -> finish())
                .show();
    }

    /** Hỏi mật khẩu tài khoản; ghi chú != null thì là lần hỏi lại sau khi sai. */
    private void hoiMatKhau(String ghiChu) {
        EditText o = new EditText(this);
        o.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        o.setHint("Mật khẩu");
        FrameLayout khung = new FrameLayout(this);
        int le = (int) (20 * getResources().getDisplayMetrics().density);
        khung.setPadding(le, le / 2, le, 0);
        khung.addView(o);
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận mật khẩu")
                .setMessage(ghiChu != null ? ghiChu : "Nhập mật khẩu tài khoản để bật đăng nhập bằng khuôn mặt:")
                .setView(khung)
                .setCancelable(false)
                .setPositiveButton("Tiếp tục", (d, w) -> {
                    String mk = o.getText().toString();
                    if (mk.isEmpty()) { hoiMatKhau("Chưa nhập mật khẩu. Nhập mật khẩu tài khoản:"); return; }
                    matKhau = mk;
                    if (buoc == Buoc.GUI) guiMayChu();      // đã có mẫu, chỉ còn chờ mật khẩu đúng
                })
                .setNegativeButton("Hủy", (d, w) -> finish())
                .show();
    }
}
