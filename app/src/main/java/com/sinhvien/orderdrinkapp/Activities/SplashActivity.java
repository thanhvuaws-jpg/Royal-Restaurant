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

/**
 * SplashActivity - Màn hình chào/khởi động khi mở ứng dụng.
 * Thực hiện: Hiển thị hiệu ứng animation giới thiệu app, đồng bộ ngầm dữ liệu từ server về SQLite,
 * và điều hướng người dùng tới màn hình phù hợp (Trang chủ Admin/Nhân viên, Trang chủ Khách hàng, hoặc Trang Welcome).
 */
public class SplashActivity extends AppCompatActivity {

    // Thời gian hiển thị màn hình chào trước khi chuyển trang (3000ms = 3 giây)
    private static int SPLASH_TIMER = 3000;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        // Ánh xạ các thành phần giao diện
        ImageView img_splash_Logo = (ImageView)findViewById(R.id.img_splash_Logo);
        TextView txt_splash_AppName = (TextView)findViewById(R.id.txt_splash_AppName);
        TextView txt_splash_PoweredBy = (TextView)findViewById(R.id.txt_splash_PoweredBy);

        // Khởi tạo các hiệu ứng chuyển động từ tài nguyên XML (anim)
        Animation sideAnim = AnimationUtils.loadAnimation(this, R.anim.side_anim);
        Animation bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_anim);

        // Thiết lập hiệu ứng chuyển động cho các view giao diện
        img_splash_Logo.setAnimation(sideAnim);
        txt_splash_AppName.setAnimation(sideAnim);
        txt_splash_PoweredBy.setAnimation(bottomAnim);

        // Kích hoạt tiến trình đồng bộ dữ liệu ngầm từ Server Cloud về SQLite SQLite nội bộ
        startSilentSync();

        // Trì hoãn việc chuyển màn hình trong khoảng thời gian SPLASH_TIMER
        new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                // Kiểm tra xem phiên đăng nhập của người dùng còn hiệu lực không
                if (SessionManager.isLoggedIn(SplashActivity.this)) {
                    // Nếu là khách hàng (maquyen == 4) thì vào trang chủ của khách hàng
                    if (SessionManager.getMaQuyen(SplashActivity.this) == 4) {
                        intent = new Intent(SplashActivity.this, CustomerHomeActivity.class);
                    } else {
                        // Nếu là nhân viên hoặc quản lý thì vào trang chủ hệ thống
                        intent = new Intent(SplashActivity.this, HomeActivity.class);
                    }
                } else {
                    // Nếu chưa đăng nhập thì chuyển đến màn hình Welcome
                    intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                }
                
                // Thiết lập cờ để xóa sạch lịch sử các Activity trước đó, ngăn người dùng quay lại màn hình Splash
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                
                // Áp dụng hiệu ứng mờ dần (fade in/out) khi chuyển Activity
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish(); // Đóng SplashActivity để giải phóng bộ nhớ
            }
        }, SPLASH_TIMER);
    }

    /**
     * Thực hiện đồng bộ dữ liệu ngầm tự động.
     * Tránh gọi API liên tục bằng cách kiểm tra mốc thời gian đồng bộ cuối cùng (tối thiểu cách nhau 10 phút).
     */
    private void startSilentSync() {
        android.content.SharedPreferences sharedPreferences = getSharedPreferences("app_sync_prefs", MODE_PRIVATE);
        long lastSyncTime = sharedPreferences.getLong("last_sync_timestamp", 0);
        long currentTime = System.currentTimeMillis();

        // 10 phút = 10 * 60 * 1000 = 600.000 ms. Nếu thời gian trôi qua chưa đủ 10 phút thì không đồng bộ lại.
        if (currentTime - lastSyncTime < 600000) {
            return;
        }

        // Khởi tạo các dịch vụ kết nối API và cơ sở dữ liệu local
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(this);

        // 1. Lấy danh sách Bàn ăn từ Server về và lưu trữ vào SQLite local
        apiService.getTables().enqueue(new retrofit2.Callback<java.util.List<com.sinhvien.orderdrinkapp.Api.TableResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.sinhvien.orderdrinkapp.Api.TableResponse>> call, retrofit2.Response<java.util.List<com.sinhvien.orderdrinkapp.Api.TableResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Lưu dữ liệu trong luồng nền (background thread) để không gây lag giao diện
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncTables(response.body());
                    });
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.sinhvien.orderdrinkapp.Api.TableResponse>> call, Throwable t) {
                t.printStackTrace(); // Ghi nhận lỗi nếu kết nối thất bại
            }
        });

        // 2. Lấy danh sách Nhân viên từ Server về và cập nhật SQLite local
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

        // 3. Đồng bộ danh mục nhóm món ăn (Category) và toàn bộ các món ăn cụ thể tương ứng
        apiService.getCategories().enqueue(new retrofit2.Callback<java.util.List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>> call, retrofit2.Response<java.util.List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> categories = response.body();
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncCategories(categories);
                        // Cập nhật mốc thời gian đồng bộ thành công mới nhất
                        sharedPreferences.edit().putLong("last_sync_timestamp", System.currentTimeMillis()).apply();

                        // Lặp qua từng danh mục để lấy danh sách món ăn thuộc về danh mục đó
                        for (com.sinhvien.orderdrinkapp.Api.LoaiMonResponse cat : categories) {
                            apiService.getDishes(cat.getMaLoai(), 1, 1000, "").enqueue(new retrofit2.Callback<com.sinhvien.orderdrinkapp.Api.DishPageResponse>() {
                                @Override
                                public void onResponse(retrofit2.Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> call, retrofit2.Response<com.sinhvien.orderdrinkapp.Api.DishPageResponse> response) {
                                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                                        java.util.List<com.sinhvien.orderdrinkapp.Api.MonResponse> dishes = response.body().getData();
                                        if (dishes != null) {
                                            LocalDatabaseHelper.getExecutor().execute(() -> {
                                                // Lưu danh sách món ăn đã đồng bộ vào SQLite local
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