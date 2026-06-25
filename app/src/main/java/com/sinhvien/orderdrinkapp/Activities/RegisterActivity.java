package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.sinhvien.orderdrinkapp.R;

/**
 * RegisterActivity - Màn hình Đăng ký tài khoản (Bước 1/2).
 * Nhiệm vụ:
 * - Thu thập thông tin cá nhân cơ bản của người dùng: Họ và tên, Tên đăng nhập, Email, Số điện thoại, và Mật khẩu.
 * - Kiểm tra tính hợp lệ của từng trường nhập liệu (Validation) theo thời gian thực (Regex, Kiểm tra trống).
 * - Đóng gói dữ liệu hợp lệ vào Bundle và truyền tiếp sang màn hình Register2ndActivity (Bước 2).
 */
public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    // Khóa định danh Bundle để truyền dữ liệu giữa các Activity
    public static final String BUNDLE = "bundle";

    // Khai báo các thành phần giao diện
    ImageView img_signup_BackBtn;
    TextInputLayout txtl_signup_FullName, txtl_signup_UserName, txtl_signup_Email, txtl_signup_PhoneNumber, txtl_signup_Password;
    Button btn_signup_Next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_layout);

        // Ánh xạ các thành phần View từ tệp giao diện XML
        img_signup_BackBtn = findViewById(R.id.img_signup_BackBtn);
        txtl_signup_FullName = findViewById(R.id.txtl_signup_FullName);
        txtl_signup_UserName = findViewById(R.id.txtl_signup_UserName);
        txtl_signup_Email = findViewById(R.id.txtl_signup_Email);
        txtl_signup_PhoneNumber = findViewById(R.id.txtl_signup_PhoneNumber);
        txtl_signup_Password = findViewById(R.id.txtl_signup_Password);
        btn_signup_Next = findViewById(R.id.btn_signup_Next);

        // Phục hồi dữ liệu biểu mẫu từ trạng thái lưu trước đó nếu thiết bị bị xoay màn hình
        if (savedInstanceState != null) {
            String savedFullName = savedInstanceState.getString("fullname", "");
            String savedUserName = savedInstanceState.getString("username", "");
            String savedEmail = savedInstanceState.getString("email", "");
            String savedPhoneNumber = savedInstanceState.getString("phone", "");
            String savedPassword = savedInstanceState.getString("password", "");

            if (txtl_signup_FullName.getEditText() != null) txtl_signup_FullName.getEditText().setText(savedFullName);
            if (txtl_signup_UserName.getEditText() != null) txtl_signup_UserName.getEditText().setText(savedUserName);
            if (txtl_signup_Email.getEditText() != null) txtl_signup_Email.getEditText().setText(savedEmail);
            if (txtl_signup_PhoneNumber.getEditText() != null) txtl_signup_PhoneNumber.getEditText().setText(savedPhoneNumber);
            if (txtl_signup_Password.getEditText() != null) txtl_signup_Password.getEditText().setText(savedPassword);
        }

        // Đăng ký sự kiện lắng nghe thao tác Click
        img_signup_BackBtn.setOnClickListener(this);
        btn_signup_Next.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_signup_Next) {
            // Thực hiện kiểm tra tính hợp lệ của tất cả các ô nhập liệu
            if (!validateFullName() | !validateUserName() | !validateEmail() | !validatePhone() | !validatePass()) {
                return; // Nếu có bất kỳ ô nào lỗi thì dừng lại không tiếp tục
            }

            // Lấy chuỗi văn bản từ các ô nhập liệu đã kiểm duyệt thành công
            String fullname = txtl_signup_FullName.getEditText().getText().toString();
            String username = txtl_signup_UserName.getEditText().getText().toString();
            String email = txtl_signup_Email.getEditText().getText().toString();
            String sdt = txtl_signup_PhoneNumber.getEditText().getText().toString();
            String password = txtl_signup_Password.getEditText().getText().toString();

            // Đóng gói thông tin và gửi sang bước tiếp theo
            Intent intent = new Intent(RegisterActivity.this, Register2ndActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("hoten", fullname);
            bundle.putString("tendn", username);
            bundle.putString("email", email);
            bundle.putString("sdt", sdt);
            bundle.putString("matkhau", password);
            intent.putExtra(BUNDLE, bundle);
            startActivity(intent);
            
            // Hiệu ứng trượt ngang khi chuyển sang màn hình tiếp theo
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        } else if (id == R.id.img_signup_BackBtn) {
            // Quay lại màn hình chào WelcomeActivity
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    /**
     * Xác thực ô Họ và tên (không được phép để trống).
     */
    private boolean validateFullName() {
        String val = txtl_signup_FullName.getEditText().getText().toString().trim();
        if (val.isEmpty()) {
            txtl_signup_FullName.setError(getResources().getString(R.string.not_empty));
            return false;
        } else {
            txtl_signup_FullName.setError(null);
            txtl_signup_FullName.setErrorEnabled(false);
            return true;
        }
    }

    /**
     * Xác thực ô Tên đăng nhập (không được phép để trống).
     */
    private boolean validateUserName() {
        String val = txtl_signup_UserName.getEditText().getText().toString().trim();
        if (val.isEmpty()) {
            txtl_signup_UserName.setError(getResources().getString(R.string.not_empty));
            return false;
        } else {
            txtl_signup_UserName.setError(null);
            txtl_signup_UserName.setErrorEnabled(false);
            return true;
        }
    }

    /**
     * Xác thực định dạng địa chỉ Email bằng biểu thức chính quy (Regular Expression).
     */
    private boolean validateEmail() {
        String val = txtl_signup_Email.getEditText().getText().toString().trim();
        String checkEmail = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (val.isEmpty()) {
            txtl_signup_Email.setError(getResources().getString(R.string.not_empty));
            return false;
        } else if (!val.matches(checkEmail)) {
            txtl_signup_Email.setError("Email không hợp lệ!");
            return false;
        } else {
            txtl_signup_Email.setError(null);
            txtl_signup_Email.setErrorEnabled(false);
            return true;
        }
    }

    /**
     * Xác thực ô Số điện thoại (không được phép để trống).
     */
    private boolean validatePhone() {
        String val = txtl_signup_PhoneNumber.getEditText().getText().toString().trim();
        if (val.isEmpty()) {
            txtl_signup_PhoneNumber.setError(getResources().getString(R.string.not_empty));
            return false;
        } else {
            txtl_signup_PhoneNumber.setError(null);
            txtl_signup_PhoneNumber.setErrorEnabled(false);
            return true;
        }
    }

    /**
     * Xác thực ô Mật khẩu (không được phép để trống).
     */
    private boolean validatePass() {
        String val = txtl_signup_Password.getEditText().getText().toString().trim();
        if (val.isEmpty()) {
            txtl_signup_Password.setError(getResources().getString(R.string.not_empty));
            return false;
        } else {
            txtl_signup_Password.setError(null);
            txtl_signup_Password.setErrorEnabled(false);
            return true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu lại dữ liệu hiện thời khi ứng dụng thay đổi cấu hình phần cứng (xoay màn hình)
        String fullName = txtl_signup_FullName.getEditText() != null ? txtl_signup_FullName.getEditText().getText().toString() : "";
        String userName = txtl_signup_UserName.getEditText() != null ? txtl_signup_UserName.getEditText().getText().toString() : "";
        String email = txtl_signup_Email.getEditText() != null ? txtl_signup_Email.getEditText().getText().toString() : "";
        String phone = txtl_signup_PhoneNumber.getEditText() != null ? txtl_signup_PhoneNumber.getEditText().getText().toString() : "";
        String pass = txtl_signup_Password.getEditText() != null ? txtl_signup_Password.getEditText().getText().toString() : "";

        outState.putString("fullname", fullName);
        outState.putString("username", userName);
        outState.putString("email", email);
        outState.putString("phone", phone);
        outState.putString("password", pass);
    }
}