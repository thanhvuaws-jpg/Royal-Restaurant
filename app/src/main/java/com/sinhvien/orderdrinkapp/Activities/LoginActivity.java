package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.StaffResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginActivity - Quản lý màn hình Đăng nhập của ứng dụng.
 * Hỗ trợ các tính năng:
 * - Đăng nhập tài khoản Nhân viên / Khách hàng thông qua kết nối API Cloud Server (Retrofit).
 * - Cơ chế lưu mật khẩu đã mã hóa AES vào SharedPreferences ("Ghi nhớ mật khẩu").
 * - Kiểm tra quyền hạn sau khi đăng nhập thành công và định hướng vào trang chủ thích hợp.
 * - Tự động hủy yêu cầu API nếu màn hình bị đóng giữa chừng để tránh rò rỉ bộ nhớ.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";

    // Khởi tạo đối tượng Call để quản lý yêu cầu đăng nhập qua API
    private Call<StaffResponse> loginCall;
    
    // Khai báo các thành phần giao diện
    TextInputLayout txtl_login_UserName, txtl_login_Password;
    Button btn_login_SignIn, btn_login_SignUp, btn_login_KhuonMat;
    CheckBox cb_login_RememberMe;
    ImageView img_login_BackBtn;

    /** Màn chính sẽ mở sau khi đăng ký khuôn mặt xong (hoặc người dùng hủy). */
    private Intent manChinhSauDangKy;
    private final androidx.activity.result.ActivityResultLauncher<Intent> moDangKyKhuonMat =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    kq -> moManChinh(manChinhSauDangKy));

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy tiến trình API nếu Activity bị huỷ đột ngột (tránh leak memory)
        if (loginCall != null) {
            loginCall.cancel();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        // Ánh xạ View từ layout XML
        txtl_login_UserName = findViewById(R.id.txtl_login_UserName);
        txtl_login_Password = findViewById(R.id.txtl_login_Password);
        btn_login_SignIn = findViewById(R.id.btn_login_SignIn);
        btn_login_SignUp = findViewById(R.id.btn_login_SignUp);
        cb_login_RememberMe = findViewById(R.id.cb_login_RememberMe);
        img_login_BackBtn = findViewById(R.id.img_login_BackBtn);
        btn_login_KhuonMat = findViewById(R.id.btn_login_KhuonMat);
        btn_login_KhuonMat.setOnClickListener(v -> startActivity(
                new Intent(this, com.sinhvien.orderdrinkapp.KhuonMat.DangNhapKhuonMatActivity.class)));

        // Đọc tên đăng nhập đã ghi nhớ (nếu có).
        //
        // CHỈ NHỚ TÊN, KHÔNG NHỚ MẬT KHẨU (sửa 25/09/2026, QĐ-096). Bản cũ lưu
        // mật khẩu mã hóa AES với khóa ghi cứng trong APK — ai có tệp APK là
        // giải ra được — rồi tự điền nguyên văn vào ô mật khẩu. Không cần tới
        // mật khẩu để giữ đăng nhập: phiên đã được giữ bằng token (SessionManager).
        SharedPreferences sharedPreferences = getSharedPreferences("remember_login", Context.MODE_PRIVATE);
        if (sharedPreferences.contains("password")) {
            // Xóa mật khẩu mà bản cũ đã lưu trên máy.
            sharedPreferences.edit().remove("password").apply();
        }
        String user = sharedPreferences.getString("username", "");
        boolean isRemember = sharedPreferences.getBoolean("isRemember", false);

        String savedUsername = "";
        boolean savedRememberMe = false;

        // Ưu tiên khôi phục dữ liệu từ InstanceState nếu Activity bị reload (xoay màn hình)
        if (savedInstanceState != null) {
            savedUsername = savedInstanceState.getString("username", "");
            savedRememberMe = savedInstanceState.getBoolean("remember_me", false);
        } else if (isRemember) {
            savedUsername = user;
            savedRememberMe = true;
        }

        // Đưa dữ liệu khôi phục lên giao diện người dùng
        if (txtl_login_UserName.getEditText() != null) {
            txtl_login_UserName.getEditText().setText(savedUsername);
        }
        cb_login_RememberMe.setChecked(savedRememberMe);

        // "Quên mật khẩu?" trước đây không có dòng xử lý nào (bấm không có gì
        // xảy ra — H14). Hệ thống không có máy chủ thư để gửi liên kết đặt lại,
        // nên chỉ đúng cách thật: nhân viên nhờ quản lý đặt mật khẩu mới (màn
        // Nhân viên → sửa), khách gọi hotline để nhà hàng hỗ trợ.
        View nutQuenMatKhau = findViewById(R.id.btn_login_ForgotPassword);
        if (nutQuenMatKhau != null) {
            nutQuenMatKhau.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Quên mật khẩu")
                    .setMessage("• Nhân viên: nhờ quản lý đặt lại mật khẩu trong mục Nhân viên.\n\n"
                            + "• Khách hàng: gọi hotline 0856 761 038, nhà hàng sẽ xác minh và đặt lại mật khẩu cho bạn.")
                    .setPositiveButton("Gọi hotline", (d, w) -> startActivity(new Intent(Intent.ACTION_DIAL,
                            android.net.Uri.parse("tel:0856761038"))))
                    .setNegativeButton("Đóng", null)
                    .show());
        }

        // Gán sự kiện lắng nghe thao tác Click cho các nút
        btn_login_SignIn.setOnClickListener(this);
        btn_login_SignUp.setOnClickListener(this);
        img_login_BackBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_login_SignIn) {
            // Chống spam bấm nút đăng nhập quá nhanh gây lỗi hoặc gửi nhiều request trùng lặp
            if (ViewUtils.isFastDoubleClick()) return;
            
            String user = "";
            String pass = "";
            if(txtl_login_UserName.getEditText() != null) user = txtl_login_UserName.getEditText().getText().toString();
            if(txtl_login_Password.getEditText() != null) pass = txtl_login_Password.getEditText().getText().toString();

            // Kiểm tra tính hợp lệ của thông tin đầu vào
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hiển thị hộp thoại vòng xoay chờ đợi
            androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(this, "Đang đăng nhập...");
            progressDialog.show();

            final String finalUser = user;
            final String finalPass = pass;
            
            // Thực hiện gọi API đăng nhập qua Retrofit Service
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            loginCall = apiService.login(user, pass);
            loginCall.enqueue(new Callback<StaffResponse>() {
                @Override
                public void onResponse(Call<StaffResponse> call, Response<StaffResponse> response) {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    if (isFinishing() || isDestroyed()) return;
                    
                    // Nếu đăng nhập thành công và server trả về mã trạng thái 'success'
                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                        Log.d(TAG, "Đăng nhập thành công: user=" + finalUser + ", role=" + response.body().getMaQuyen());
                        StaffResponse res = response.body();
                        
                        // Lưu trữ thông tin tài khoản đăng nhập thành công vào phiên ứng dụng (SessionManager)
                        SessionManager.saveSession(LoginActivity.this, res.getMaQuyen(), res.getMaNV(), res.getHoTenNV(), res.getToken());

                        // Xử lý logic ghi nhớ tài khoản đăng nhập
                        SharedPreferences sharedPreferences = getSharedPreferences("remember_login", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        if (cb_login_RememberMe.isChecked()) {
                            // Chỉ nhớ tên đăng nhập — xem chú thích ở onCreate.
                            editor.putString("username", finalUser);
                            editor.remove("password");
                            editor.putBoolean("isRemember", true);
                        } else {
                            // Xóa sạch dữ liệu nếu người dùng không chọn ghi nhớ
                            editor.clear();
                        }
                        editor.apply();

                        // Điều hướng người dùng dựa vào mã quyền (1, 2, 3: Admin/Nhân viên, 4: Khách hàng)
                        Intent intent;
                        if (res.getMaQuyen() == 4) {
                            intent = new Intent(LoginActivity.this, CustomerHomeActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, HomeActivity.class);
                        }
                        intent.putExtra("tendn", finalUser);
                        moiDangKyKhuonMatRoiMo(intent, res.getMaNV(), finalUser, finalPass);
                    } else {
                        // Nhận thông điệp lỗi trả về từ server
                        String msg = response.body() != null ? response.body().getMessage() : "Sai tên đăng nhập hoặc mật khẩu!";
                        Log.w(TAG, "Đăng nhập thất bại: " + msg);
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<StaffResponse> call, Throwable t) {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    Log.e(TAG, "Lỗi kết nối API login: " + t.getMessage());
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(LoginActivity.this, "Lỗi kết nối Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (id == R.id.btn_login_SignUp) {
            // Chuyển tới màn hình đăng ký tài khoản (khách hàng mới)
            Intent iRegister = new Intent(this, RegisterActivity.class);
            startActivity(iRegister);
        } else if (id == R.id.img_login_BackBtn) {
            // Quay lại trang trước đó bằng hiệu ứng trượt màn hình từ trái qua phải
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Nút "Đăng nhập bằng khuôn mặt" chỉ hiện khi máy đã đăng ký cho ít nhất một tài khoản.
        btn_login_KhuonMat.setVisibility(
                com.sinhvien.orderdrinkapp.KhuonMat.KhoKhuonMat.coTaiKhoan(this) ? View.VISIBLE : View.GONE);
    }

    /**
     * Đăng nhập mật khẩu thành công: nếu tài khoản này chưa đăng ký khuôn mặt trên
     * máy, hỏi một lần có muốn bật không (dùng luôn mật khẩu vừa gõ, khỏi hỏi lại),
     * rồi mới mở màn chính. "Không hỏi lại" được nhớ theo từng tài khoản.
     */
    private void moiDangKyKhuonMatRoiMo(Intent manChinh, int maNV, String tenDN, String matKhau) {
        SharedPreferences pref = getSharedPreferences("khuon_mat_hoi", Context.MODE_PRIVATE);
        boolean coCamera = getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY);
        if (!coCamera || pref.getBoolean("khong_hoi_" + maNV, false)
                || com.sinhvien.orderdrinkapp.KhuonMat.KhoKhuonMat.tim(this, maNV) != null) {
            moManChinh(manChinh);
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đăng nhập nhanh bằng khuôn mặt?")
                .setMessage("Lần sau chỉ cần nhìn vào camera và chớp mắt, không phải gõ mật khẩu. "
                        + "Ảnh khuôn mặt chỉ xử lý trên máy này, không gửi đi đâu.")
                .setCancelable(false)
                .setPositiveButton("Bật ngay", (d, w) -> {
                    manChinhSauDangKy = manChinh;
                    Intent i = new Intent(this, com.sinhvien.orderdrinkapp.KhuonMat.DangKyKhuonMatActivity.class);
                    i.putExtra(com.sinhvien.orderdrinkapp.KhuonMat.DangKyKhuonMatActivity.EXTRA_MAT_KHAU, matKhau);
                    i.putExtra(com.sinhvien.orderdrinkapp.KhuonMat.DangKyKhuonMatActivity.EXTRA_TEN_DN, tenDN);
                    moDangKyKhuonMat.launch(i);
                })
                .setNegativeButton("Để sau", (d, w) -> moManChinh(manChinh))
                .setNeutralButton("Không hỏi lại", (d, w) -> {
                    pref.edit().putBoolean("khong_hoi_" + maNV, true).apply();
                    moManChinh(manChinh);
                })
                .show();
    }

    private void moManChinh(Intent manChinh) {
        if (manChinh == null) return;
        startActivity(manChinh);
        finish(); // Kết thúc đăng nhập, loại bỏ khỏi ngăn xếp BackStack
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu giữ tạm thời dữ liệu biểu mẫu khi ứng dụng bị xoay dọc/ngang tránh mất thông tin đã gõ
        // (Không tự lưu mật khẩu vào Bundle: ô nhập tự giữ nội dung khi xoay.)
        String user = "";
        if (txtl_login_UserName.getEditText() != null) {
            user = txtl_login_UserName.getEditText().getText().toString();
        }
        outState.putString("username", user);
        outState.putBoolean("remember_me", cb_login_RememberMe.isChecked());
    }
}