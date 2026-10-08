package com.sinhvien.orderdrinkapp.KhuonMat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.sinhvien.orderdrinkapp.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Phần chung của màn đăng ký và màn đăng nhập bằng khuôn mặt (QĐ-103):
 * xin quyền camera, mở camera trước bằng CameraX, đưa từng khung hình (đã xoay
 * đứng, thu về cạnh dài 480 px) qua {@link MoHinhKhuonMat} trên MỘT luồng riêng,
 * rồi giao kết quả cho lớp con ở {@link #xuLyKhung}.
 *
 * Nạp mô hình (~9 MB) cũng chạy trên luồng đó, không chặn giao diện.
 */
public abstract class ManHinhCameraKhuonMat extends AppCompatActivity {

    private static final String TAG = "CameraKhuonMat";
    /**
     * Cạnh dài của khung đưa vào phân tích. MediaPipe tự thu ảnh về 192–256 px
     * nên khung lớn hơn chỉ tốn thời gian chép và xoay. Mặt ngồi trước máy chiếm
     * ~nửa khung -> ~150 px, vẫn dư so với 90 px tối thiểu để đo chớp mắt.
     */
    private static final int CANH_DAI = 360;

    protected static final int MAU_CHO = 0xFFBCAAA4, MAU_DAT = 0xFF66BB6A, MAU_LOI = 0xFFE57373;

    protected TextView txtTieuDe, txtPhuDe, txtHuongDan, txtChiTiet;
    protected ProgressBar tienDo;
    protected MaterialButton btnPhu;
    private PreviewView xemTruoc;
    private View vien;

    protected MoHinhKhuonMat moHinh;
    private ExecutorService luong;
    private ProcessCameraProvider camera;
    /** Đặt true để ngừng xử lý khung (đang gọi máy chủ, đã xong, đang đóng). */
    protected volatile boolean dungPhanTich = false;

    private final ActivityResultLauncher<String> xinQuyen = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), duoc -> {
                if (duoc) batCamera();
                else baoLoiVaDong("Cần quyền dùng camera để nhận diện khuôn mặt. "
                        + "Bạn có thể cấp lại trong Cài đặt của điện thoại.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quet_khuon_mat);
        txtTieuDe = findViewById(R.id.txt_km_tieu_de);
        txtPhuDe = findViewById(R.id.txt_km_phu_de);
        txtHuongDan = findViewById(R.id.txt_km_huong_dan);
        txtChiTiet = findViewById(R.id.txt_km_chi_tiet);
        tienDo = findViewById(R.id.tien_do_km);
        btnPhu = findViewById(R.id.btn_km_phu);
        xemTruoc = findViewById(R.id.preview_km);
        vien = findViewById(R.id.vien_km);

        luong = Executors.newSingleThreadExecutor();
        luong.execute(() -> {
            try {
                moHinh = new MoHinhKhuonMat(getApplicationContext());
                runOnUiThread(this::kiemQuyenCamera);
            } catch (Throwable t) {
                Log.e(TAG, "Không nạp được mô hình", t);
                runOnUiThread(() -> baoLoiVaDong("Máy này chưa chạy được nhận diện khuôn mặt ("
                        + t.getClass().getSimpleName() + "). Hãy dùng mật khẩu."));
            }
        });
    }

    private void kiemQuyenCamera() {
        if (isFinishing() || isDestroyed()) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            batCamera();
        } else {
            xinQuyen.launch(Manifest.permission.CAMERA);
        }
    }

    private void batCamera() {
        ListenableFuture<ProcessCameraProvider> tuongLai = ProcessCameraProvider.getInstance(this);
        tuongLai.addListener(() -> {
            try {
                camera = tuongLai.get();
                Preview xem = new Preview.Builder().build();
                xem.setSurfaceProvider(xemTruoc.getSurfaceProvider());
                ImageAnalysis phanTich = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(360, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                phanTich.setAnalyzer(luong, this::phanTichKhung);
                CameraSelector chon = camera.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                        ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
                camera.unbindAll();
                camera.bindToLifecycle(this, chon, xem, phanTich);
                khiCameraSanSang();
            } catch (Exception e) {
                Log.e(TAG, "Không mở được camera", e);
                baoLoiVaDong("Không mở được camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private int demKhung = 0;
    private long tongMs = 0, moc = 0;

    private void phanTichKhung(ImageProxy khung) {
        long bd = SystemClock.uptimeMillis();
        try {
            if (dungPhanTich || moHinh == null) return;
            Bitmap anh = dungVaThuNho(khung.toBitmap(), khung.getImageInfo().getRotationDegrees());
            long t = SystemClock.uptimeMillis();
            MoHinhKhuonMat.KhuonMat km = moHinh.phanTich(anh, t);
            if (!dungPhanTich) xuLyKhung(km, anh, t);
        } catch (Throwable e) {
            Log.w(TAG, "Lỗi xử lý khung", e);
        } finally {
            khung.close();
            ghiTocDo(SystemClock.uptimeMillis() - bd);
        }
    }

    /** Ghi tốc độ xử lý mỗi 30 khung (logcat CameraKhuonMat) — để biết máy nào quá chậm cho việc đo chớp mắt. */
    private void ghiTocDo(long ms) {
        long bay = SystemClock.uptimeMillis();
        if (moc == 0) moc = bay;
        tongMs += ms;
        if (++demKhung == 30) {
            Log.i(TAG, String.format(java.util.Locale.US, "%.1f khung/giây, %.0f ms mỗi khung",
                    30000f / Math.max(1, bay - moc), tongMs / 30f));
            demKhung = 0; tongMs = 0; moc = bay;
        }
    }

    private static Bitmap dungVaThuNho(Bitmap goc, int xoay) {
        float tiLe = Math.min(1f, CANH_DAI / (float) Math.max(goc.getWidth(), goc.getHeight()));
        if (xoay == 0 && tiLe == 1f) return goc;
        Matrix m = new Matrix();
        m.postScale(tiLe, tiLe);
        m.postRotate(xoay);
        Bitmap kq = Bitmap.createBitmap(goc, 0, 0, goc.getWidth(), goc.getHeight(), m, true);
        if (kq != goc) goc.recycle();
        return kq;
    }

    /** Gọi trên luồng phân tích cho MỖI khung. km = null khi không thấy khuôn mặt. */
    protected abstract void xuLyKhung(MoHinhKhuonMat.KhuonMat km, Bitmap anh, long tMs) throws Exception;

    /** Camera đã chạy (luồng giao diện). */
    protected void khiCameraSanSang() { }

    /** Cập nhật hướng dẫn + màu vòng tròn; gọi từ luồng nào cũng được. */
    protected void hien(String huongDan, String chiTiet, int mauVien) {
        runOnUiThread(() -> {
            txtHuongDan.setText(huongDan);
            txtChiTiet.setText(chiTiet == null ? "" : chiTiet);
            if (vien.getBackground() instanceof GradientDrawable) {
                ((GradientDrawable) vien.getBackground().mutate()).setStroke(
                        (int) (5 * getResources().getDisplayMetrics().density), mauVien);
            }
        });
    }

    protected void datTienDo(int phanTram) {
        runOnUiThread(() -> {
            tienDo.setVisibility(View.VISIBLE);
            tienDo.setProgress(phanTram);
            tienDo.setProgressTintList(ColorStateList.valueOf(MAU_DAT));
        });
    }

    /** Cho phép xử lý khung trở lại (sau một lần thử thất bại). */
    protected void tiepTucPhanTich() {
        dungPhanTich = false;
    }

    protected void baoLoiVaDong(String thongBao) {
        if (isFinishing() || isDestroyed()) return;
        dungPhanTich = true;
        new AlertDialog.Builder(this)
                .setTitle("Không dùng được khuôn mặt")
                .setMessage(thongBao)
                .setCancelable(false)
                .setPositiveButton("Đã hiểu", (d, w) -> finish())
                .show();
    }

    @Override
    protected void onDestroy() {
        dungPhanTich = true;
        if (camera != null) {
            try { camera.unbindAll(); } catch (Exception ignored) { }
        }
        if (luong != null) {
            luong.execute(() -> { if (moHinh != null) moHinh.close(); });
            luong.shutdown();
        }
        super.onDestroy();
    }
}
