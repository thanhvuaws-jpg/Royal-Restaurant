package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.sinhvien.orderdrinkapp.R;

public class SplashActivity extends AppCompatActivity {

    private static int SPLASH_TIMER = 3000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);
        

        //tạo đối tượng view
        ImageView img_splash_Logo = (ImageView)findViewById(R.id.img_splash_Logo);
        TextView txt_splash_AppName = (TextView)findViewById(R.id.txt_splash_AppName);
        TextView txt_splash_PoweredBy = (TextView)findViewById(R.id.txt_splash_PoweredBy);

        //lấy đối tượng animation
        Animation sideAnim = AnimationUtils.loadAnimation(this,R.anim.side_anim);
        Animation bottomAnim = AnimationUtils.loadAnimation(this,R.anim.bottom_anim);

        //thiết lập animation cho component
        img_splash_Logo.setAnimation(sideAnim);
        txt_splash_AppName.setAnimation(sideAnim);
        txt_splash_PoweredBy.setAnimation(bottomAnim);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish(); //destroy activity khi back sẽ ko về splash
            }
        },SPLASH_TIMER);
    }
}