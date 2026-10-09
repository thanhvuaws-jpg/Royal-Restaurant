package com.sinhvien.orderdrinkapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.sinhvien.orderdrinkapp.BuildConfig;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.BaoTri;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Màn "Hệ thống đang bảo trì" (QĐ-107).
 *
 * Mở bởi BaoTri khi máy chủ báo bảo trì. Cứ 15 giây hỏi lại
 * <MAY_CHU>/bao-tri/trang-thai (Caddy luôn trả lời đường này, kể cả lúc bảo
 * trì); hết bảo trì thì quay về SplashActivity để đi lại đúng luồng đăng nhập.
 * Socket báo "tắt bảo trì" thì hỏi lại ngay, không đợi hết chu kỳ.
 */
public class BaoTriActivity extends AppCompatActivity {
    private static final long CHU_KY_MS = 15000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .build();
    private TextView txtThongBao, txtDuKien, txtTrangThai;
    private boolean dangHoi = false;

    private final Runnable hoiDinhKy = new Runnable() {
        @Override public void run() {
            kiemTra();
            handler.postDelayed(this, CHU_KY_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bao_tri_layout);
        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.royal_bg));
        txtThongBao = findViewById(R.id.txt_baotri_ThongBao);
        txtDuKien = findViewById(R.id.txt_baotri_DuKien);
        txtTrangThai = findViewById(R.id.txt_baotri_TrangThai);
        findViewById(R.id.btn_baotri_ThuLai).setOnClickListener(v -> kiemTra());

        // Không có màn nào phía sau để quay về (đã xóa ngăn xếp): nút Back
        // chỉ đưa app ra nền.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { moveTaskToBack(true); }
        });
        hienNoiDung(BaoTri.thongBao(), BaoTri.duKien());
    }

    @Override
    protected void onResume() {
        super.onResume();
        BaoTri.datNgheKetThuc(this::kiemTra);
        handler.postDelayed(hoiDinhKy, CHU_KY_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BaoTri.datNgheKetThuc(null);
        handler.removeCallbacks(hoiDinhKy);
    }

    private void hienNoiDung(String thongBao, String duKien) {
        if (thongBao != null && !thongBao.isEmpty()) txtThongBao.setText(thongBao);
        String gio = dinhDangGio(duKien);
        txtDuKien.setVisibility(gio == null ? View.GONE : View.VISIBLE);
        if (gio != null) txtDuKien.setText("Dự kiến hoạt động lại lúc " + gio);
    }

    /**
     * "2026-10-09T17:30:00+07:00" -> "17:30" theo giờ máy; sai định dạng thì bỏ qua.
     * Dùng SimpleDateFormat (mẫu XXX có từ API 24) vì java.time cần API 26 mà
     * app còn hỗ trợ Android 7.0.
     */
    private static String dinhDangGio(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(iso);
            return d == null ? null : new SimpleDateFormat("HH:mm", Locale.US).format(d);
        } catch (Exception e) {
            return null;
        }
    }

    private void kiemTra() {
        if (dangHoi) return;
        dangHoi = true;
        txtTrangThai.setText("Đang kiểm tra…");
        Request yc = new Request.Builder().url(BuildConfig.MAY_CHU + "bao-tri/trang-thai").build();
        http.newCall(yc).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    dangHoi = false;
                    txtTrangThai.setText("Chưa kết nối được máy chủ — sẽ thử lại sau ít giây.");
                });
            }

            @Override public void onResponse(Call call, Response r) throws IOException {
                String than = r.body() != null ? r.body().string() : "";
                boolean vanBaoTri = false;
                String tb = "", dk = "";
                // Chỉ coi là còn bảo trì khi máy chủ nói rõ bat=true. Máy chủ
                // không có đường này (chạy local, không qua Caddy) thì không
                // thể đang bảo trì.
                if (r.isSuccessful()) {
                    try {
                        JSONObject o = new JSONObject(than);
                        vanBaoTri = o.optBoolean("bat", false);
                        tb = o.optString("thong_bao", "");
                        dk = o.isNull("du_kien") ? "" : o.optString("du_kien", "");
                    } catch (Exception ignored) { }
                }
                final boolean con = vanBaoTri;
                final String tb2 = tb, dk2 = dk;
                runOnUiThread(() -> {
                    dangHoi = false;
                    if (con) {
                        hienNoiDung(tb2, dk2);
                        txtTrangThai.setText("Vẫn đang bảo trì — app sẽ tự mở lại khi xong.");
                    } else {
                        moLai();
                    }
                });
            }
        });
    }

    private void moLai() {
        BaoTri.ketThuc();
        Intent i = new Intent(this, SplashActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
