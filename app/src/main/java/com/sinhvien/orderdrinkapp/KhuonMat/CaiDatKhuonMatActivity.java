package com.sinhvien.orderdrinkapp.KhuonMat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.KhuonMatResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Bật / đăng ký lại / tắt đăng nhập bằng khuôn mặt cho tài khoản đang đăng nhập (QĐ-103).
 * Mở từ menu "Đăng nhập bằng khuôn mặt" của màn khách hàng và màn nhân viên.
 */
public class CaiDatKhuonMatActivity extends AppCompatActivity {

    private TextView txtTrangThai, txtChiTiet;
    private MaterialButton btnBat, btnTat;

    private final ActivityResultLauncher<Intent> moDangKy = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), kq -> veLai());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cai_dat_khuon_mat);
        txtTrangThai = findViewById(R.id.txt_cdkm_trang_thai);
        txtChiTiet = findViewById(R.id.txt_cdkm_chi_tiet);
        btnBat = findViewById(R.id.btn_cdkm_bat);
        btnTat = findViewById(R.id.btn_cdkm_tat);
        findViewById(R.id.img_cdkm_quay_lai).setOnClickListener(v -> finish());
        btnBat.setOnClickListener(v -> moDangKy.launch(new Intent(this, DangKyKhuonMatActivity.class)));
        btnTat.setOnClickListener(v -> xacNhanTat());
        veLai();
    }

    private void veLai() {
        KhoKhuonMat.TaiKhoan tk = KhoKhuonMat.tim(this, SessionManager.getMaNV(this));
        if (tk == null) {
            txtTrangThai.setText("Chưa bật trên máy này");
            txtChiTiet.setText("Bật để lần sau đăng nhập bằng cách nhìn vào camera và chớp mắt, "
                    + "không cần gõ mật khẩu.");
            btnBat.setText("Bật đăng nhập bằng khuôn mặt");
            btnTat.setVisibility(View.GONE);
        } else {
            String ngay = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(tk.ngayDangKy));
            txtTrangThai.setText("Đang bật trên máy này");
            txtChiTiet.setText("Đăng ký lúc " + ngay + ". Ở màn đăng nhập, chọn \"Đăng nhập bằng khuôn mặt\".");
            btnBat.setText("Đăng ký lại khuôn mặt");
            btnTat.setVisibility(View.VISIBLE);
        }
        // Mỗi tài khoản trên máy có mẫu khuôn mặt riêng; nói rõ để người dùng biết
        // bật cho tài khoản này không ảnh hưởng các tài khoản khác đã bật.
        int soTk = KhoKhuonMat.docTatCa(this).size();
        if (soTk > (tk == null ? 0 : 1)) {
            txtChiTiet.append("\n\nMáy này đang bật đăng nhập bằng khuôn mặt cho " + soTk
                    + " tài khoản; mỗi tài khoản có khuôn mặt riêng.");
        }
    }

    private void xacNhanTat() {
        new AlertDialog.Builder(this)
                .setTitle("Tắt đăng nhập bằng khuôn mặt?")
                .setMessage("Dữ liệu khuôn mặt trên máy này sẽ bị xóa. Lần sau phải đăng nhập bằng mật khẩu.")
                .setPositiveButton("Tắt", (d, w) -> tat())
                .setNegativeButton("Giữ lại", null)
                .show();
    }

    private void tat() {
        KhoKhuonMat.TaiKhoan tk = KhoKhuonMat.tim(this, SessionManager.getMaNV(this));
        if (tk == null) { veLai(); return; }
        // Xóa trên máy trước: kể cả khi mất mạng, máy này không còn đăng nhập bằng mặt được nữa.
        KhoKhuonMat.xoa(this, tk.maNV);
        veLai();
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.tatKhuonMat("tat", tk.maKhoa).enqueue(new Callback<KhuonMatResponse>() {
            @Override
            public void onResponse(Call<KhuonMatResponse> call, Response<KhuonMatResponse> r) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(CaiDatKhuonMatActivity.this, "Đã tắt đăng nhập bằng khuôn mặt", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<KhuonMatResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(CaiDatKhuonMatActivity.this,
                        "Đã xóa trên máy. Chưa báo được máy chủ (mất kết nối) — khóa cũ không còn dùng được vì máy đã xóa.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
