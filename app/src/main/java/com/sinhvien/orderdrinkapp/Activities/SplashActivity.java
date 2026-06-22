package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static int SPLASH_TIMER = 3000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        // Tạo đối tượng view
        ImageView img_splash_Logo = (ImageView)findViewById(R.id.img_splash_Logo);
        TextView txt_splash_AppName = (TextView)findViewById(R.id.txt_splash_AppName);
        TextView txt_splash_PoweredBy = (TextView)findViewById(R.id.txt_splash_PoweredBy);

        // Lấy đối tượng animation
        Animation sideAnim = AnimationUtils.loadAnimation(this,R.anim.side_anim);
        Animation bottomAnim = AnimationUtils.loadAnimation(this,R.anim.bottom_anim);

        // Thiết lập animation cho component
        img_splash_Logo.setAnimation(sideAnim);
        txt_splash_AppName.setAnimation(sideAnim);
        txt_splash_PoweredBy.setAnimation(bottomAnim);

        // Bắt đầu đồng bộ ngầm toàn bộ dữ liệu danh mục từ Server về SQLite
        startSilentSync();

        new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                if (SessionManager.isLoggedIn(SplashActivity.this)) {
                    if (SessionManager.getMaQuyen(SplashActivity.this) == 4) {
                        intent = new Intent(SplashActivity.this, CustomerHomeActivity.class);
                    } else {
                        intent = new Intent(SplashActivity.this, HomeActivity.class);
                    }
                } else {
                    intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish(); //destroy activity khi back sẽ ko về splash
            }
        },SPLASH_TIMER);
    }

    private void startSilentSync() {
        android.content.SharedPreferences sharedPreferences = getSharedPreferences("app_sync_prefs", MODE_PRIVATE);
        long lastSyncTime = sharedPreferences.getLong("last_sync_timestamp", 0);
        long currentTime = System.currentTimeMillis();

        // 10 phút = 10 * 60 * 1000 = 600.000 ms
        if (currentTime - lastSyncTime < 600000) {
            // Còn trong 10 phút -> bỏ qua không gọi API ngầm nữa
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(this);

        // 1. Đồng bộ danh sách Bàn ăn
        apiService.getTables().enqueue(new retrofit2.Callback<java.util.List<com.sinhvien.orderdrinkapp.Api.TableResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.sinhvien.orderdrinkapp.Api.TableResponse>> call, retrofit2.Response<java.util.List<com.sinhvien.orderdrinkapp.Api.TableResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncTables(response.body());
                    });
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.sinhvien.orderdrinkapp.Api.TableResponse>> call, Throwable t) {
                t.printStackTrace();
            }
        });

        // 2. Đồng bộ danh sách Nhân viên
        apiService.getStaff().enqueue(new retrofit2.Callback<java.util.List<com.sinhvien.orderdrinkapp.Api.StaffResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.sinhvien.orderdrinkapp.Api.StaffResponse>> call, retrofit2.Response<java.util.List<com.sinhvien.orderdrinkapp.Api.StaffResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncStaff(response.body());
                    });
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.sinhvien.orderdrinkapp.Api.StaffResponse>> call, Throwable t) {
                t.printStackTrace();
            }
        });

        // 3. Đồng bộ danh mục Loại món và các Món ăn tương ứng
        apiService.getCategories().enqueue(new retrofit2.Callback<java.util.List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>> call, retrofit2.Response<java.util.List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> categories = response.body();
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncCategories(categories);
                        // Lưu mốc thời gian đồng bộ thành công vào SharedPreferences
                        sharedPreferences.edit().putLong("last_sync_timestamp", System.currentTimeMillis()).apply();

                        // Với mỗi loại món, tự động tải danh sách các món ăn về lưu vào SQLite
                        for (com.sinhvien.orderdrinkapp.Api.LoaiMonResponse cat : categories) {
                            apiService.getDishes(cat.getMaLoai(), 1, 1000, "").enqueue(new retrofit2.Callback<com.sinhvien.orderdrinkapp.Api.DishPageResponse>() {
                                @Override
                                public void onResponse(retrofit2.Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> call, retrofit2.Response<com.sinhvien.orderdrinkapp.Api.DishPageResponse> response) {
                                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                                        java.util.List<com.sinhvien.orderdrinkapp.Api.MonResponse> dishes = response.body().getData();
                                        if (dishes != null) {
                                            LocalDatabaseHelper.getExecutor().execute(() -> {
                                                dbHelper.syncDishes(cat.getMaLoai(), dishes, true);
                                            });
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(retrofit2.Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> call, Throwable t) {
                                    t.printStackTrace();
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
}