package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

public class WelcomeActivity extends AppCompatActivity {

    Button BTN_welcome_Login, BTN_welcome_SignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo dữ liệu mẫu ngay khi vào app
        // Đã chuyển sang dùng Cloud, không cần thông báo thực đơn mẫu nữa
        new Thread(new Runnable() {
            @Override
            public void run() {
                // com.sinhvien.orderdrinkapp.Utils.SampleDataInitializer.init(WelcomeActivity.this); // Disabled for Cloud standardization
            }
        }).start();

        // Nếu đã đăng nhập trước đó → bỏ qua màn hình chào, vào thẳng HomeActivity
        if (SessionManager.isLoggedIn(this)) {
            goToHome();
            return;
        }

        setContentView(R.layout.welcome_layout);

        BTN_welcome_Login = (Button) findViewById(R.id.btn_welcome_Login);
        BTN_welcome_SignUp = (Button) findViewById(R.id.btn_welcome_SignUp);

        BTN_welcome_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),LoginActivity.class);

                Pair[] pairs = new Pair[1];
                pairs[0] = new Pair<View, String>(findViewById(R.id.btn_welcome_Login),"transition_login");

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(WelcomeActivity.this,pairs);
                    startActivity(intent,options.toBundle());
                }
                else {
                    startActivity(intent);
                }
            }
        });

        BTN_welcome_SignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),RegisterActivity.class);
                Pair[] pairs = new Pair[1];
                pairs[0] = new Pair<View, String>(findViewById(R.id.btn_welcome_SignUp),"transition_signup");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(WelcomeActivity.this,pairs);
                    startActivity(intent,options.toBundle());
                }
                else {
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra lại mỗi khi quay lại app
        if (SessionManager.isLoggedIn(this)) {
            goToHome();
        }
    }

    private void goToHome() {
        Intent intent;
        if (SessionManager.getMaQuyen(this) == 4) {
            intent = new Intent(this, CustomerHomeActivity.class);
        } else {
            intent = new Intent(this, HomeActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}