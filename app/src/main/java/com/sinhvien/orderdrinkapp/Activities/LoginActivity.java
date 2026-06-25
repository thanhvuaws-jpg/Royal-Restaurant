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
 * - Cơ chế lưu mật khẩu đã mã hóa Base64 vào SharedPreferences ("Ghi nhớ mật khẩu").
 * - Kiểm tra quyền hạn sau khi đăng nhập thành công và định hướng vào trang chủ thích hợp.
 * - Tự động hủy yêu cầu API nếu màn hình bị đóng giữa chừng để tránh rò rỉ bộ nhớ.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";

    // Khởi tạo đối tượng Call để quản lý yêu cầu đăng nhập qua API
    private Call<StaffResponse> loginCall;
    
    // Khai báo các thành phần giao diện
    TextInputLayout txtl_login_UserName, txtl_login_Password;
    Button btn_login_SignIn, btn_login_SignUp;
    CheckBox cb_login_RememberMe;
    ImageView img_login_BackBtn;

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

        // Đọc thông tin ghi nhớ đăng nhập đã lưu trong SharedPreferences (nếu có)
        SharedPreferences sharedPreferences = getSharedPreferences("remember_login", Context.MODE_PRIVATE);
        String user = sharedPreferences.getString("username", "");
        String encodedPass = sharedPreferences.getString("password", "");
        String pass = "";
        try {
            if (!encodedPass.isEmpty()) {
                // Giải mã mật khẩu bằng Base64 để hiển thị lên trường nhập liệu
                byte[] decodedBytes = android.util.Base64.decode(encodedPass, android.util.Base64.DEFAULT);
                pass = new String(decodedBytes, "UTF-8");
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi giải mã mật khẩu đã lưu: " + e.getMessage());
        }
        boolean isRemember = sharedPreferences.getBoolean("isRemember", false);

        String savedUsername = "";
        String savedPassword = "";
        boolean savedRememberMe = false;

        // Ưu tiên khôi phục dữ liệu từ InstanceState nếu Activity bị reload (xoay màn hình)
        if (savedInstanceState != null) {
            savedUsername = savedInstanceState.getString("username", "");
            savedPassword = savedInstanceState.getString("password", "");
            savedRememberMe = savedInstanceState.getBoolean("remember_me", false);
        } else {
            // Ngược lại thì điền thông tin đăng nhập tự động từ dữ liệu "Ghi nhớ" trước đó
            if (isRemember) {
                savedUsername = user;
                savedPassword = pass;
                savedRememberMe = true;
            }
        }

        // Đưa dữ liệu khôi phục lên giao diện người dùng
        if (txtl_login_UserName.getEditText() != null) {
            txtl_login_UserName.getEditText().setText(savedUsername);
        }
        if (txtl_login_Password.getEditText() != null) {
            txtl_login_Password.getEditText().setText(savedPassword);
        }
        cb_login_RememberMe.setChecked(savedRememberMe);

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
                            try {
                                // Mã hóa Base64 trước khi lưu mật khẩu để tăng tính an toàn ở mức cơ bản
                                String encoded = android.util.Base64.encodeToString(finalPass.getBytes("UTF-8"), android.util.Base64.DEFAULT);
                                editor.putString("username", finalUser);
                                editor.putString("password", encoded);
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi mã hóa mật khẩu: " + e.getMessage());
                            }
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
                        startActivity(intent);
                        finish(); // Kết thúc đăng nhập, loại bỏ khỏi ngăn xếp BackStack
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu giữ tạm thời dữ liệu biểu mẫu khi ứng dụng bị xoay dọc/ngang tránh mất thông tin đã gõ
        String user = "";
        String pass = "";
        if (txtl_login_UserName.getEditText() != null) {
            user = txtl_login_UserName.getEditText().getText().toString();
        }
        if (txtl_login_Password.getEditText() != null) {
            pass = txtl_login_Password.getEditText().getText().toString();
        }
        outState.putString("username", user);
        outState.putString("password", pass);
        outState.putBoolean("remember_me", cb_login_RememberMe.isChecked());
    }
}