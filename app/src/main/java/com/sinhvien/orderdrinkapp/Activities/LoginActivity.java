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

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";

    TextInputLayout txtl_login_UserName, txtl_login_Password;
    Button btn_login_SignIn, btn_login_SignUp;
    CheckBox cb_login_RememberMe;
    ImageView img_login_BackBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        txtl_login_UserName = findViewById(R.id.txtl_login_UserName);
        txtl_login_Password = findViewById(R.id.txtl_login_Password);
        btn_login_SignIn = findViewById(R.id.btn_login_SignIn);
        btn_login_SignUp = findViewById(R.id.btn_login_SignUp);
        cb_login_RememberMe = findViewById(R.id.cb_login_RememberMe);
        img_login_BackBtn = findViewById(R.id.img_login_BackBtn);

        SharedPreferences sharedPreferences = getSharedPreferences("remember_login", Context.MODE_PRIVATE);
        String user = sharedPreferences.getString("username", "");
        String encodedPass = sharedPreferences.getString("password", "");
        String pass = "";
        try {
            if (!encodedPass.isEmpty()) {
                byte[] decodedBytes = android.util.Base64.decode(encodedPass, android.util.Base64.DEFAULT);
                pass = new String(decodedBytes, "UTF-8");
            }
        } catch (Exception e) {}
        boolean isRemember = sharedPreferences.getBoolean("isRemember", false);

        String savedUsername = "";
        String savedPassword = "";
        boolean savedRememberMe = false;

        if (savedInstanceState != null) {
            savedUsername = savedInstanceState.getString("username", "");
            savedPassword = savedInstanceState.getString("password", "");
            savedRememberMe = savedInstanceState.getBoolean("remember_me", false);
        } else {
            if (isRemember) {
                savedUsername = user;
                savedPassword = pass;
                savedRememberMe = true;
            }
        }

        if (txtl_login_UserName.getEditText() != null) {
            txtl_login_UserName.getEditText().setText(savedUsername);
        }
        if (txtl_login_Password.getEditText() != null) {
            txtl_login_Password.getEditText().setText(savedPassword);
        }
        cb_login_RememberMe.setChecked(savedRememberMe);

        btn_login_SignIn.setOnClickListener(this);
        btn_login_SignUp.setOnClickListener(this);
        img_login_BackBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_login_SignIn) {
            if (ViewUtils.isFastDoubleClick()) return; // Chống double click
            String user = "";
            String pass = "";
            if(txtl_login_UserName.getEditText() != null) user = txtl_login_UserName.getEditText().getText().toString();
            if(txtl_login_Password.getEditText() != null) pass = txtl_login_Password.getEditText().getText().toString();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(this, "Đang đăng nhập...");
            progressDialog.show();

            final String finalUser = user;
            final String finalPass = pass;
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.login(user, pass).enqueue(new Callback<StaffResponse>() {
                @Override
                public void onResponse(Call<StaffResponse> call, Response<StaffResponse> response) {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                        Log.d(TAG, "Đăng nhập thành công: user=" + finalUser + ", role=" + response.body().getMaQuyen());
                        StaffResponse res = response.body();
                        SessionManager.saveSession(LoginActivity.this, res.getMaQuyen(), res.getMaNV(), res.getHoTenNV(), res.getToken());

                        SharedPreferences sharedPreferences = getSharedPreferences("remember_login", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        if (cb_login_RememberMe.isChecked()) {
                            try {
                                String encoded = android.util.Base64.encodeToString(finalPass.getBytes("UTF-8"), android.util.Base64.DEFAULT);
                                editor.putString("username", finalUser);
                                editor.putString("password", encoded);
                            } catch (Exception e) {}
                            editor.putBoolean("isRemember", true);
                        } else {
                            editor.clear();
                        }
                        editor.apply();

                        Intent intent;
                        if (res.getMaQuyen() == 4) {
                            intent = new Intent(LoginActivity.this, CustomerHomeActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, HomeActivity.class);
                        }
                        intent.putExtra("tendn", finalUser);
                        startActivity(intent);
                        finish();
                    } else {
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
            Intent iRegister = new Intent(this, RegisterActivity.class);
            startActivity(iRegister);
        } else if (id == R.id.img_login_BackBtn) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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