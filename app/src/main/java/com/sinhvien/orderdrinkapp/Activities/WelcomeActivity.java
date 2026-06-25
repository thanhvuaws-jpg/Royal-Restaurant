package com.sinhvien.orderdrinkapp.Activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

/**
 * WelcomeActivity - Màn hình chào mừng người dùng mới.
 * Chức năng:
 * - Kiểm tra phiên đăng nhập đã tồn tại để bỏ qua màn hình chào (Auto-login).
 * - Cung cấp hai nút điều hướng chính: Đăng nhập (LoginActivity) và Đăng ký (RegisterActivity).
 * - Sử dụng hiệu ứng chuyển đổi mượt mà Shared Elements (Transition) cho các nút trên các phiên bản Android hỗ trợ Lollipop trở lên.
 */
public class WelcomeActivity extends AppCompatActivity {

    // Khai báo các nút bấm trên giao diện chào mừng
    Button BTN_welcome_Login, BTN_welcome_SignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // [TỐI ƯU] Nếu tài khoản đã đăng nhập trước đó -> Tự động chuyển thẳng vào màn hình chính phù hợp
        if (SessionManager.isLoggedIn(this)) {
            goToHome();
            return;
        }

        setContentView(R.layout.welcome_layout);

        // Ánh xạ các nút bấm từ layout XML
        BTN_welcome_Login = (Button) findViewById(R.id.btn_welcome_Login);
        BTN_welcome_SignUp = (Button) findViewById(R.id.btn_welcome_SignUp);

        // Sự kiện click nút Đăng nhập
        BTN_welcome_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);

                // Áp dụng Shared Elements Transition nếu phiên bản hệ điều hành Android >= Lollipop (API 21)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                            WelcomeActivity.this, 
                            findViewById(R.id.btn_welcome_Login), 
                            "transition_login"
                    );
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        });

        // Sự kiện click nút Đăng ký
        BTN_welcome_SignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                
                // Áp dụng Shared Elements Transition nếu phiên bản hệ điều hành Android >= Lollipop (API 21)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                            WelcomeActivity.this, 
                            findViewById(R.id.btn_welcome_SignUp), 
                            "transition_signup"
                    );
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra lại phiên đăng nhập khi người dùng quay lại Activity này (VD: sau khi đăng xuất)
        if (SessionManager.isLoggedIn(this)) {
            goToHome();
        }
    }

    /**
     * Điều hướng trực tiếp đến trang chủ tương ứng dựa trên mã quyền được lưu trong Session.
     */
    private void goToHome() {
        Intent intent;
        if (SessionManager.getMaQuyen(this) == 4) {
            // Quyền 4 = Khách hàng
            intent = new Intent(this, CustomerHomeActivity.class);
        } else {
            // Các quyền khác = Quản lý / Nhân viên
            intent = new Intent(this, HomeActivity.class);
        }
        // Thiết lập cờ xóa toàn bộ stack để ngăn người dùng Back quay lại màn hình Welcome
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Đóng Activity hiện tại
    }
}