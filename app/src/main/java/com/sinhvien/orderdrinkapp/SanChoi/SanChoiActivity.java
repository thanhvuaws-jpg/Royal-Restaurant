package com.sinhvien.orderdrinkapp.SanChoi;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.MiniGameKetQua;
import com.sinhvien.orderdrinkapp.Api.MiniGameResponse;
import com.sinhvien.orderdrinkapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * SanChoiActivity — Sân chơi Royal (QĐ-111): vòng quay may mắn và rương thưởng
 * chuỗi 7 ngày điểm danh, dựng lại đúng bản demo web người dùng đã duyệt
 * (_nangcap/minigame/choi_thu).
 *
 * MÁY CHỦ QUYẾT ĐỊNH, APP DIỄN LẠI
 * --------------------------------
 * Bấm QUAY → gọi `minigame.php` (action = quay). Máy chủ chọn ô, ghi điểm hoặc
 * phát mã, trừ lượt — xong mới trả về chỉ số ô. App cho bánh xe quay tới đúng
 * ô đó. Tắt app giữa chừng cũng không mất giải: điểm đã nằm trong CSDL.
 *
 * VÍ ĐIỂM: số trên ví chỉ nhích lên khi từng đồng xu bay tới (như demo), nhưng
 * số thật đã có ngay từ phản hồi máy chủ (diemThat) — chotXu() chốt lại sau.
 */
public class SanChoiActivity extends AppCompatActivity {

    /** Lựa chọn bật/tắt tiếng — dùng chung với màn Điểm danh. */
    public static final String PREF = "san_choi";
    public static final String KHOA_AM = "am_thanh";

    private ApiService api;
    private AmThanhGame am;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();

    private View sanVq, sanRuong, khungVq, khungRuong, ruong, loe, haoQuang, bong, thuong, viXu, theKetQua;
    private TextView tabVq, tabRuong, txtDiem, txtLuot, txtGhiChuVq, txtKq, txtKqPhu, btnNhan,
            txtThuong, txtThuongPhu, txtNutMo, txtRuongPhu, txtThongBao, txtTen, txtNutQuay;
    private ImageView kim, anhRuong, imgXuVi;
    private ImageButton btnAm;
    private VongQuayView vongQuay;
    private TiaSangView tia;
    private HieuUngView hieuUng;
    private NutVang nutQuay, nutMo;
    private LinearLayout chamChuoi;
    private ProgressBar dangTai;

    private MiniGameResponse trangThai;
    private MiniGameKetQua kqVuaTrung;
    private int diemThat = 0, diemHienThi = 0;
    private boolean dangGoi = false;
    private boolean chuaSanSang = false;
    /** dong → dang_mo → da_mo */
    private String trangThaiRuong = "dong";
    private ValueAnimator loLung, xoayTia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_san_choi);
        getWindow().setStatusBarColor(Color.parseColor("#170D0A"));
        getWindow().setNavigationBarColor(Color.parseColor("#170D0A"));

        api = ApiClient.getClient().create(ApiService.class);
        SharedPreferences sp = getSharedPreferences(PREF, MODE_PRIVATE);
        am = new AmThanhGame(this, sp.getBoolean(KHOA_AM, true));

        sanVq = findViewById(R.id.san_mg_vong_quay);
        sanRuong = findViewById(R.id.san_mg_ruong);
        khungVq = findViewById(R.id.khung_mg_vong_quay);
        khungRuong = findViewById(R.id.khung_mg_ruong);
        ruong = findViewById(R.id.ruong_mg);
        anhRuong = findViewById(R.id.img_mg_ruong);
        loe = findViewById(R.id.loe_mg);
        haoQuang = findViewById(R.id.hao_quang_mg);
        bong = findViewById(R.id.bong_mg);
        tia = findViewById(R.id.tia_mg);
        thuong = findViewById(R.id.thuong_mg);
        txtThuong = findViewById(R.id.txt_mg_thuong);
        txtThuongPhu = findViewById(R.id.txt_mg_thuong_phu);
        viXu = findViewById(R.id.vi_mg_xu);
        imgXuVi = findViewById(R.id.img_mg_xu_vi);
        txtDiem = findViewById(R.id.txt_mg_diem);
        txtTen = findViewById(R.id.txt_mg_ten);
        tabVq = findViewById(R.id.tab_mg_vong_quay);
        tabRuong = findViewById(R.id.tab_mg_ruong);
        vongQuay = findViewById(R.id.vong_quay);
        kim = findViewById(R.id.img_mg_kim);
        theKetQua = findViewById(R.id.the_mg_ket_qua);
        txtKq = findViewById(R.id.txt_mg_kq);
        txtKqPhu = findViewById(R.id.txt_mg_kq_phu);
        btnNhan = findViewById(R.id.btn_mg_nhan);
        nutQuay = findViewById(R.id.nut_mg_quay);
        txtNutQuay = findViewById(R.id.txt_mg_nut_quay);
        txtLuot = findViewById(R.id.txt_mg_luot);
        txtGhiChuVq = findViewById(R.id.txt_mg_ghi_chu_vq);
        nutMo = findViewById(R.id.nut_mg_mo);
        txtNutMo = findViewById(R.id.txt_mg_nut_mo);
        txtRuongPhu = findViewById(R.id.txt_mg_ruong_phu);
        chamChuoi = findViewById(R.id.cham_mg_chuoi);
        txtThongBao = findViewById(R.id.txt_mg_thong_bao);
        dangTai = findViewById(R.id.tai_mg);
        hieuUng = findViewById(R.id.hieu_ung_mg);
        btnAm = findViewById(R.id.btn_mg_am);

        findViewById(R.id.btn_mg_quay_lai).setOnClickListener(v -> finish());
        btnAm.setOnClickListener(v -> {
            am.setBat(!am.isBat());
            sp.edit().putBoolean(KHOA_AM, am.isBat()).apply();
            veNutAm();
        });
        veNutAm();

        tabVq.setOnClickListener(v -> moTab(false));
        tabRuong.setOnClickListener(v -> moTab(true));
        nutQuay.setOnClickListener(v -> quay());
        btnNhan.setOnClickListener(v -> nhanThuong());
        nutMo.setOnClickListener(v -> moRuong());
        ruong.setOnClickListener(v -> moRuong());
        nutQuay.setEnabled(false);
        nutMo.setEnabled(false);

        vongQuay.setNgheQuay(new VongQuayView.NgheQuay() {
            @Override public void onVachQuaKim(float tienDo) { kimGiat(tienDo); }
            @Override public void onDungLai(int o) { xongQuay(); }
        });

        taiPhongChu();
        findViewById(R.id.man_san_choi).post(this::canhKichThuoc);
        moTab(savedInstanceState != null && savedInstanceState.getBoolean("tab_ruong", false)
                || "ruong".equals(getIntent().getStringExtra("tab")));
        taiTrangThai();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("tab_ruong", sanRuong.getVisibility() == View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (loLung != null) loLung.cancel();
        if (xoayTia != null) xoayTia.cancel();
        am.giaiPhong();
        super.onDestroy();
    }

    /* ═══════════════════════════ Dựng giao diện ═══════════════════════════ */

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    /** Baloo 2 ExtraBold (tải qua Google Fonts); không tải được thì giữ chữ đậm mặc định. */
    private void taiPhongChu() {
        try {
            ResourcesCompat.getFont(this, R.font.baloo2_extrabold, new ResourcesCompat.FontCallback() {
                @Override public void onFontRetrieved(@NonNull Typeface tf) {
                    if (isFinishing()) return;
                    for (TextView t : new TextView[]{txtTen, tabVq, tabRuong, txtDiem, txtNutQuay, txtNutMo,
                            txtKq, btnNhan, txtThuong}) {
                        t.setTypeface(tf);
                    }
                    vongQuay.setPhongChu(tf);
                }
                @Override public void onFontRetrievalFailed(int reason) { }
            }, h);
        } catch (Exception ignored) { }
    }

    /**
     * Cỡ bánh xe và rương theo màn hình, như demo: bánh xe min(86% bề rộng, 350dp),
     * rương min(84%, 330dp); màn thấp thì thu theo chiều cao còn lại.
     */
    private void canhKichThuoc() {
        View sanKhau = findViewById(R.id.san_khau);
        int rong = sanKhau.getWidth(), cao = sanKhau.getHeight();
        if (rong == 0 || cao == 0) return;
        int dVq = (int) Math.min(Math.min(rong * .86f, dp(350)), cao - dp(34 + 28 + 70 + 34));
        datCo(khungVq, dVq, dVq);
        int rongKim = (int) (dVq * .15f);
        FrameLayout.LayoutParams lpKim = (FrameLayout.LayoutParams) kim.getLayoutParams();
        lpKim.width = rongKim;
        lpKim.height = (int) (rongKim * 84f / 60f);
        lpKim.topMargin = (int) (-dVq * .075f);
        kim.setLayoutParams(lpKim);
        kim.setPivotX(rongKim / 2f);
        kim.setPivotY(lpKim.height * .28f);
        theKetQua.setTranslationY(-dVq * .1f);

        int dR = (int) Math.min(Math.min(rong * .84f, dp(330)), cao - dp(40 + 16 + 70 + 40));
        datCo(khungRuong, dR, dR);
        datCo(ruong, (int) (dR * .84f), ViewGroup.LayoutParams.WRAP_CONTENT);
        ruong.setPivotX(dR * .84f / 2f);
        ruong.post(() -> ruong.setPivotY(ruong.getHeight() * .85f));
        FrameLayout.LayoutParams lpBong = (FrameLayout.LayoutParams) bong.getLayoutParams();
        lpBong.width = (int) (dR * .62f);
        lpBong.height = (int) (dR * .09f);
        lpBong.bottomMargin = (int) (dR * .13f);   // sát đáy rương (ảnh rương rộng hơn cao)
        bong.setLayoutParams(lpBong);
        thuong.setTranslationY(-dR * .07f);         // số thưởng nằm trên nắp, không đè lên

        bong.setBackground(gradTron(new int[]{0xCC000000, 0x00000000}, lpBong.width / 2f));
        haoQuang.setBackground(gradTron(new int[]{0xFFFFCF6A, 0x66FF9B3A, 0x00FF9B3A}, dR * .44f));
        haoQuang.setAlpha(.25f);
        haoQuang.setScaleX(.7f); haoQuang.setScaleY(.7f);
        loe.setBackground(gradTron(new int[]{0xFFFFFFFF, 0xFFFFF4C8, 0x00FFF4C8}, dR * .6f));
        FrameLayout.LayoutParams lpTia = (FrameLayout.LayoutParams) tia.getLayoutParams();
        int coTia = (int) (dR * 1.44f);
        lpTia.width = coTia; lpTia.height = coTia;
        lpTia.gravity = android.view.Gravity.CENTER;
        tia.setLayoutParams(lpTia);
    }

    private static void datCo(View v, int w, int hh) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = w; lp.height = hh;
        v.setLayoutParams(lp);
    }

    private static GradientDrawable gradTron(int[] mau, float banKinh) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setColors(mau);
        g.setGradientRadius(banKinh);
        return g;
    }

    private void veNutAm() {
        btnAm.setImageResource(am.isBat() ? R.drawable.ic_mg_am_bat : R.drawable.ic_mg_am_tat);
        btnAm.setContentDescription(am.isBat() ? "Tắt âm thanh" : "Bật âm thanh");
    }

    private void moTab(boolean ruongTab) {
        sanVq.setVisibility(ruongTab ? View.GONE : View.VISIBLE);
        sanRuong.setVisibility(ruongTab ? View.VISIBLE : View.GONE);
        tabVq.setBackgroundResource(ruongTab ? 0 : R.drawable.bg_mg_tab_chon);
        tabRuong.setBackgroundResource(ruongTab ? R.drawable.bg_mg_tab_chon : 0);
        tabVq.setTextColor(Color.parseColor(ruongTab ? "#B8A386" : "#2A1406"));
        tabRuong.setTextColor(Color.parseColor(ruongTab ? "#2A1406" : "#B8A386"));
    }

    /* ═════════════════════════════ Dữ liệu ═════════════════════════════════ */

    private void taiTrangThai() {
        api.layTrangThaiSanChoi().enqueue(new Callback<MiniGameResponse>() {
            @Override
            public void onResponse(@NonNull Call<MiniGameResponse> call, @NonNull Response<MiniGameResponse> r) {
                if (isFinishing()) return;
                dangTai.setVisibility(View.GONE);
                MiniGameResponse b = r.body();
                if (r.isSuccessful() && b != null && "success".equals(b.getStatus())) {
                    trangThai = b;
                    chuaSanSang = false;
                    txtThongBao.setVisibility(View.GONE);
                    diemThat = b.getDiem();
                    if (!hieuUng.dangChay()) {
                        diemHienThi = diemThat;
                        txtDiem.setText(String.valueOf(diemHienThi));
                    }
                    List<String[]> nhan = new ArrayList<>();
                    if (b.getVongQuay() != null && b.getVongQuay().getO() != null) {
                        for (MiniGameResponse.O o : b.getVongQuay().getO()) nhan.add(new String[]{o.getTo(), o.getNho()});
                    }
                    vongQuay.setNhan(nhan);
                    veLuot();
                    veRuong();
                    return;
                }
                org.json.JSONObject loi = thanLoi(r);
                if (r.code() == 503 && "chua_san_sang".equals(loi.optString("ma"))) {
                    chuaSanSang = true;
                    hienThongBao("Sân chơi sắp mở!\nNhà hàng đang chuẩn bị giải thưởng, bạn quay lại sau nhé.");
                } else {
                    String m = loi.optString("message", "");
                    hienThongBao(m.isEmpty() ? "Không tải được sân chơi. Mở lại sau ít phút nhé." : m);
                }
                veLuot();
                veRuong();
            }

            @Override
            public void onFailure(@NonNull Call<MiniGameResponse> call, @NonNull Throwable t) {
                if (isFinishing()) return;
                dangTai.setVisibility(View.GONE);
                hienThongBao("Không kết nối được máy chủ. Kiểm tra mạng rồi mở lại sân chơi nhé.");
            }
        });
    }

    private void hienThongBao(String chu) {
        txtThongBao.setText(chu);
        txtThongBao.setVisibility(View.VISIBLE);
    }

    private int luotCon() {
        return trangThai != null && trangThai.getVongQuay() != null ? trangThai.getVongQuay().getLuotCon() : 0;
    }

    private void veLuot() {
        boolean coDuLieu = trangThai != null && trangThai.getVongQuay() != null && !chuaSanSang;
        int luot = luotCon();
        if (!coDuLieu) {
            txtLuot.setText(chuaSanSang ? "Sắp mở" : "Chưa tải được");
        } else if (luot > 0) {
            txtLuot.setText("Còn " + luot + " lượt hôm nay");
        } else {
            txtLuot.setText(trangThai.getVongQuay().isDaDiemDanh()
                    ? "Hết lượt, quay lại vào ngày mai" : "Hết lượt, điểm danh để thêm 1 lượt");
        }
        nutQuay.setEnabled(coDuLieu && luot > 0 && !dangGoi && !vongQuay.isDangQuay()
                && theKetQua.getVisibility() != View.VISIBLE);
    }

    private void veRuong() {
        MiniGameResponse.Ruong r = trangThai != null ? trangThai.getRuong() : null;
        int can = r != null && r.getCan() > 0 ? r.getCan() : 7;
        int chuoi = r != null ? r.getChuoi() : 0;

        chamChuoi.removeAllViews();
        for (int i = 1; i <= can; i++) {
            View c = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) dp(12), (int) dp(12));
            lp.setMargins((int) dp(4), 0, (int) dp(4), 0);
            c.setLayoutParams(lp);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            boolean xong = i <= chuoi || (r != null && r.isMoDuoc());
            g.setColor(Color.parseColor(xong ? "#E3B34F" : "#1D0F0C"));
            g.setStroke((int) dp(1), Color.parseColor(xong ? "#FFE6A3" : "#6B4A1F"));
            c.setBackground(g);
            chamChuoi.addView(c);
        }

        if ("da_mo".equals(trangThaiRuong) || "dang_mo".equals(trangThaiRuong)) return;
        boolean moDuoc = r != null && r.isMoDuoc() && !chuaSanSang;
        nutMo.setEnabled(moDuoc && !dangGoi);
        txtNutMo.setText("MỞ RƯƠNG");
        if (moDuoc) {
            txtRuongPhu.setText(r.getHan() != null ? "Mở trước hết ngày " + r.getHan() : "Rương đã sẵn sàng!");
            ruong.setAlpha(1f);
            batLoLung(true);
        } else {
            txtRuongPhu.setText(chuaSanSang ? "Sắp mở" : "Điểm danh " + chuoi + "/" + can + " ngày liên tiếp");
            ruong.setAlpha(.55f);
            batLoLung(false);
        }
    }

    /** Rương "lơ lửng" nhẹ khi đang chờ mở (translateY 0 → −7dp → 0, 2,8 giây). */
    private void batLoLung(boolean bat) {
        if (bat && loLung == null) {
            loLung = ValueAnimator.ofFloat(0f, 1f);
            loLung.setDuration(2800);
            loLung.setRepeatCount(ValueAnimator.INFINITE);
            loLung.setInterpolator(new LinearInterpolator());
            loLung.addUpdateListener(a -> ruong.setTranslationY(
                    -dp(7) * (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * (float) a.getAnimatedValue()))));
            loLung.start();
        } else if (!bat && loLung != null) {
            loLung.cancel();
            loLung = null;
            ruong.setTranslationY(0);
        }
    }

    /* ═══════════════════════════════ Vòng quay ═════════════════════════════ */

    private void quay() {
        if (dangGoi || vongQuay.isDangQuay() || luotCon() <= 0) return;
        dangGoi = true;
        nutQuay.setEnabled(false);
        api.choiSanChoi("quay").enqueue(new Callback<MiniGameKetQua>() {
            @Override
            public void onResponse(@NonNull Call<MiniGameKetQua> call, @NonNull Response<MiniGameKetQua> r) {
                if (isFinishing()) return;
                MiniGameKetQua b = r.body();
                if (r.isSuccessful() && b != null && "success".equals(b.getStatus())) {
                    kqVuaTrung = b;
                    diemThat = b.getDiem();
                    if (trangThai != null && trangThai.getVongQuay() != null) {
                        // Cập nhật số lượt cục bộ; tải lại đầy đủ sau khi khách bấm "Nhận".
                        trangThai.getVongQuay().setLuotCon(b.getLuotCon());
                    }
                    vongQuay.quayToi(b.getO());
                } else {
                    dangGoi = false;
                    Toast.makeText(SanChoiActivity.this, docLoi(r, "Chưa quay được, thử lại nhé."), Toast.LENGTH_LONG).show();
                    taiTrangThai();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MiniGameKetQua> call, @NonNull Throwable t) {
                if (isFinishing()) return;
                dangGoi = false;
                veLuot();
                Toast.makeText(SanChoiActivity.this, "Mất kết nối, lượt quay chưa bị trừ. Thử lại nhé.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Vách vàng quét qua kim: kim giật lên −22° rồi bật về, kêu "tích". */
    private void kimGiat(float tienDo) {
        am.tich();
        ObjectAnimator a = ObjectAnimator.ofFloat(kim, View.ROTATION, -22f, 0f);
        a.setDuration((long) Math.min(260, 60 + tienDo * 300));
        a.setInterpolator(new OvershootInterpolator(3f));
        a.start();
    }

    private void xongQuay() {
        dangGoi = false;
        MiniGameKetQua b = kqVuaTrung;
        if (b == null) return;
        ((TextView) findViewById(R.id.txt_mg_kq_dau)).setText("trong".equals(b.getLoai()) ? "TIẾC QUÁ!" : "BẠN NHẬN ĐƯỢC");
        txtKq.setText(b.getTen() != null ? b.getTen() : "+" + b.getDiemNhan() + " điểm");
        String phu = null;
        if (b.getMaCode() != null) phu = "Mã " + b.getMaCode() + " đã vào ví của bạn";
        else if ("luot".equals(b.getLoai())) phu = "Bấm QUAY để dùng ngay";
        txtKqPhu.setText(phu);
        txtKqPhu.setVisibility(phu != null ? View.VISIBLE : View.GONE);

        h.postDelayed(() -> {
            theKetQua.setVisibility(View.VISIBLE);
            theKetQua.setAlpha(0f);
            theKetQua.setScaleX(.3f); theKetQua.setScaleY(.3f);
            theKetQua.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600)
                    .setInterpolator(new OvershootInterpolator(1.6f)).start();
            if ("trong".equals(b.getLoai())) {
                am.thich();
            } else {
                am.thang();
                float[] tam = tamTrongHieuUng(khungVq, .5f, .4f);
                boolean lon = b.getDiemNhan() >= 100 || "voucher".equals(b.getLoai());
                hieuUng.phaoGiay(tam[0], tam[1], lon ? 160 : 90, lon ? 1.3f : 1f);
            }
            veLuot();
        }, 450);
    }

    private void nhanThuong() {
        MiniGameKetQua b = kqVuaTrung;
        theKetQua.animate().alpha(0f).scaleX(.6f).scaleY(.6f).setDuration(200)
                .withEndAction(() -> { theKetQua.setVisibility(View.INVISIBLE); veLuot(); }).start();
        if (b != null && b.getDiemNhan() > 0) {
            float[] tu = tamTrongHieuUng(txtKq, .5f, .5f);
            float[] den = tamTrongHieuUng(imgXuVi, .5f, .5f);
            int so = Math.min(14, 4 + Math.round(b.getDiemNhan() / 15f));
            hieuUng.xuBay(tu[0], tu[1], den[0], den[1], so, .8f, this::xuToi);
            h.postDelayed(this::chotXu, so * 45L + 1500);
        } else {
            chotXu();
        }
        kqVuaTrung = null;
        taiTrangThai();   // lượt còn lại, nhãn ô (voucher có thể vừa đổi) đúng theo máy chủ
    }

    /** Một đồng xu vừa tới ví: số nhích lên, ví nảy nhẹ, kêu "ting". */
    private void xuToi() {
        int buoc = Math.max(1, (int) Math.ceil((diemThat - diemHienThi) / 6.0));
        diemHienThi = Math.min(diemThat, diemHienThi + buoc);
        txtDiem.setText(String.valueOf(diemHienThi));
        viXu.animate().cancel();
        viXu.setScaleX(1f); viXu.setScaleY(1f);
        viXu.animate().scaleX(1.14f).scaleY(1.14f).setDuration(140)
                .withEndAction(() -> viXu.animate().scaleX(1f).scaleY(1f).setDuration(210).start()).start();
        am.ting();
    }

    private void chotXu() {
        diemHienThi = diemThat;
        txtDiem.setText(String.valueOf(diemHienThi));
    }

    /* ════════════════════════════════ Rương ════════════════════════════════ */

    private void moRuong() {
        if (!"dong".equals(trangThaiRuong) || dangGoi || trangThai == null || trangThai.getRuong() == null
                || !trangThai.getRuong().isMoDuoc()) return;
        dangGoi = true;
        nutMo.setEnabled(false);
        api.choiSanChoi("mo_ruong").enqueue(new Callback<MiniGameKetQua>() {
            @Override
            public void onResponse(@NonNull Call<MiniGameKetQua> call, @NonNull Response<MiniGameKetQua> r) {
                if (isFinishing()) return;
                MiniGameKetQua b = r.body();
                if (r.isSuccessful() && b != null && "success".equals(b.getStatus())) {
                    diemThat = b.getDiem();
                    rungRuong(b);
                } else {
                    dangGoi = false;
                    Toast.makeText(SanChoiActivity.this, docLoi(r, "Chưa mở được rương, thử lại nhé."),
                            Toast.LENGTH_LONG).show();
                    taiTrangThai();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MiniGameKetQua> call, @NonNull Throwable t) {
                if (isFinishing()) return;
                dangGoi = false;
                veRuong();
                Toast.makeText(SanChoiActivity.this, "Mất kết nối, rương vẫn còn đó. Thử lại nhé.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /** 1) Rung mạnh dần 1,3 giây, hào quang lớn dần, tia lửa xì ra khe nắp. */
    private void rungRuong(MiniGameKetQua kq) {
        trangThaiRuong = "dang_mo";
        batLoLung(false);
        ValueAnimator rung = ValueAnimator.ofFloat(0f, 1f);
        rung.setDuration(1300);
        rung.setInterpolator(new LinearInterpolator());
        final long t0 = System.currentTimeMillis();
        rung.addUpdateListener(a -> {
            float k = (float) a.getAnimatedValue();
            long t = System.currentTimeMillis() - t0;
            float bien = 1.5f + k * 6f;
            double nhip = t / (42.0 - k * 18.0);
            ruong.setTranslationX(dp((float) (Math.sin(nhip * 2.1) * bien * .6)));
            ruong.setTranslationY(dp((float) (Math.abs(Math.sin(nhip)) * -bien * .4)));
            ruong.setRotation((float) (Math.sin(nhip) * bien * .7));
            ruong.setScaleX(1 + k * .05f); ruong.setScaleY(1 + k * .05f);
            haoQuang.setAlpha(.25f + k * .75f);
            haoQuang.setScaleX(.7f + k * .5f); haoQuang.setScaleY(.7f + k * .5f);
            if (rnd.nextFloat() < .25f + k * .5f) {
                float[] p = tamTrongHieuUng(ruong, .5f, .42f);
                hieuUng.tiaLua(p[0], p[1], 2);
            }
            if (rnd.nextFloat() < .18f) am.lach();
        });
        rung.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) { bungNap(kq); }
        });
        rung.start();
    }

    /** 2) Lóe trắng, nắp bật nảy kiểu lò xo, tia sáng xoay, xu tung ra, số thưởng bật lên. */
    private void bungNap(MiniGameKetQua kq) {
        ruong.setTranslationX(0); ruong.setTranslationY(0); ruong.setRotation(0);
        loe.setAlpha(0f);
        ValueAnimator loeA = ValueAnimator.ofFloat(0f, 1f);
        loeA.setDuration(650);
        loeA.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            loe.setAlpha(t < .25f ? t / .25f : 1f - (t - .25f) / .75f);
        });
        loeA.start();
        am.thich();
        h.postDelayed(() -> anhRuong.setImageResource(R.drawable.mg_ruong_mo), 130);

        // Nén – giãn: (0,86; 1,08) → (1,14; 0,92) → (0,97; 1,03) → 1
        final float[][] khung = {{.86f, 1.08f}, {1.14f, .92f}, {.97f, 1.03f}, {1f, 1f}};
        final float[] moc = {0f, .35f, .7f, 1f};
        ValueAnimator nen = ValueAnimator.ofFloat(0f, 1f);
        nen.setDuration(700);
        nen.setInterpolator(new AccelerateDecelerateInterpolator());
        nen.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            int i = t < moc[1] ? 0 : t < moc[2] ? 1 : 2;
            float u = (t - moc[i]) / (moc[i + 1] - moc[i]);
            ruong.setScaleX(khung[i][0] + (khung[i + 1][0] - khung[i][0]) * u);
            ruong.setScaleY(khung[i][1] + (khung[i + 1][1] - khung[i][1]) * u);
        });
        nen.start();

        tia.animate().alpha(1f).setDuration(600).start();
        xoayTia = ValueAnimator.ofFloat(0f, 360f);
        xoayTia.setDuration(14000);
        xoayTia.setRepeatCount(ValueAnimator.INFINITE);
        xoayTia.setInterpolator(new LinearInterpolator());
        xoayTia.addUpdateListener(a -> tia.setRotation((float) a.getAnimatedValue()));
        xoayTia.start();
        haoQuang.animate().alpha(.9f).scaleX(1.25f).scaleY(1.25f).setDuration(250).start();

        h.postDelayed(() -> {
            float[] tu = tamTrongHieuUng(ruong, .5f, .38f);
            float[] den = tamTrongHieuUng(imgXuVi, .5f, .5f);
            hieuUng.xuBay(tu[0], tu[1], den[0], den[1], 16, 1.15f, this::xuToi);
            float[] p = tamTrongHieuUng(ruong, .5f, .3f);
            hieuUng.phaoGiay(p[0], p[1], 110, 1.1f);
            am.thang();
            txtThuong.setText("+" + kq.getDiemNhan());
            txtThuongPhu.setText(kq.getTenVoucher() != null
                    ? "kèm mã " + kq.getTenVoucher() + (kq.getMaCode() != null ? " · " + kq.getMaCode() : "")
                    : "điểm thưởng trọn 7 ngày");
            findViewById(R.id.txt_mg_ghi_chu_ruong).animate().alpha(0f).setDuration(250).start();
            thuong.setAlpha(0f);
            thuong.setScaleX(.4f); thuong.setScaleY(.4f);
            thuong.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700)
                    .setInterpolator(new OvershootInterpolator(2.2f)).start();
            h.postDelayed(this::chotXu, 16 * 45L + 1500);
        }, 160);

        h.postDelayed(() -> {
            trangThaiRuong = "da_mo";
            dangGoi = false;
            txtNutMo.setText("ĐÃ MỞ RƯƠNG");
            txtRuongPhu.setText("Rương mới khi trọn chuỗi 7 ngày tiếp theo");
            nutMo.setEnabled(false);
            taiTrangThai();
        }, 1900);
    }

    /* ═══════════════════════════════ Tiện ích ══════════════════════════════ */

    /** Điểm (fx, fy theo tỉ lệ) của view `v`, tính theo tọa độ của lớp hiệu ứng. */
    private float[] tamTrongHieuUng(View v, float fx, float fy) {
        int[] a = new int[2], b = new int[2];
        v.getLocationInWindow(a);
        hieuUng.getLocationInWindow(b);
        return new float[]{a[0] - b[0] + v.getWidth() * fx, a[1] - b[1] + v.getHeight() * fy};
    }

    /**
     * Thân lỗi JSON (409/503…). errorBody() chỉ đọc được MỘT lần, nên đọc một
     * lần rồi lấy các trường từ đối tượng này.
     */
    private static org.json.JSONObject thanLoi(Response<?> r) {
        try {
            if (r.errorBody() != null) return new org.json.JSONObject(r.errorBody().string());
        } catch (Exception ignored) { }
        return new org.json.JSONObject();
    }

    private static String docLoi(Response<?> r, String macDinh) {
        String m = thanLoi(r).optString("message", "");
        return m.isEmpty() ? macDinh : m;
    }
}
