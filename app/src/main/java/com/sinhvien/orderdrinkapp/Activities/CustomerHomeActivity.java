package com.sinhvien.orderdrinkapp.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.material.navigation.NavigationView;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.Fragments.CustomerBookingFragment;
import com.sinhvien.orderdrinkapp.Fragments.CustomerProfileFragment;
import com.sinhvien.orderdrinkapp.Fragments.CustomerContactFragment;

// Nhập thư viện cần thiết cho SharedPreferences, AlertDialog, API và Socket
import android.content.SharedPreferences;
import android.app.AlertDialog;
import java.util.List;
import java.util.ArrayList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;

/**
 * CustomerHomeActivity - Màn hình chính điều hướng (Dashboard) dành cho Khách hàng (Customer).
 * Chức năng chính:
 * - Hỗ trợ Điều hướng Thích ứng (Adaptive Navigation): Cho phép chuyển đổi giữa Ngăn kéo vuốt (Navigation Drawer) và Thanh điều hướng dưới (Bottom Navigation View).
 * - Quản lý nạp Fragment nội dung tương ứng: Đặt bàn (CustomerBookingFragment), Lịch sử đặt hàng/chi tiêu (CustomerProfileFragment), Liên hệ quán (CustomerContactFragment).
 * - Kết nối Socket.io real-time tự động đăng ký kênh khách hàng (join_customer) và nhận thông báo thay đổi trạng thái đặt bàn trực tiếp.
 * - Quét và so sánh bộ nhớ đệm trạng thái đặt bàn (checkBookingChangesOnResume) khi bật lại app để đưa ra cảnh báo hộp thoại AlertDialog nếu trạng thái đặt bàn đã được thay đổi.
 * - Xử lý chức năng Đăng xuất: xóa cache các Fragment, hủy kết nối Socket và điều hướng lại về WelcomeActivity.
 */
public class CustomerHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Khai báo các đối tượng View giao diện
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    FragmentManager fragmentManager;
    TextView txt_menu_tennv;
    ActionBarDrawerToggle drawerToggle;

    private BottomNavigationView bottomNav;
    private ImageView btnToggleNav;
    private boolean useBottomNav; // Lưu trạng thái lựa chọn kiểu thanh điều hướng của người dùng
    private Fragment currentFragment; // Fragment hiện tại đang hiển thị
    private com.sinhvien.orderdrinkapp.Utils.BookingAlertManager bookingAlertManager;
    private io.socket.emitter.Emitter.Listener connectListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customer_home_layout);

        // Ánh xạ các View của Layout
        drawerLayout = findViewById(R.id.customer_drawer_layout);
        navigationView = findViewById(R.id.navigation_view_customer);
        toolbar = findViewById(R.id.customer_toolbar);

        // Thiết lập Toolbar làm Action Bar cho Activity
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bottomNav = findViewById(R.id.customer_bottom_nav);
        btnToggleNav = findViewById(R.id.btn_toggle_nav);

        // Cài đặt Drawer Toggle để nhấn mở ngăn kéo menu
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Hiển thị tên Khách hàng lên Header của Navigation Drawer
        View headerView = navigationView.getHeaderView(0);
        txt_menu_tennv = headerView.findViewById(R.id.txt_menu_tennv);

        String hoten = SessionManager.getFullName(this);
        if (hoten.isEmpty()) hoten = "Khách hàng";
        txt_menu_tennv.setText(hoten);

        // Khởi tạo kết nối Socket.io real-time
        com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().connect();
        io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (socket != null) {
            // Lắng nghe sự kiện kết nối thành công tới server node.js
            connectListener = args -> {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerHomeActivity.this, "🔌 Đã kết nối Real-time!", Toast.LENGTH_SHORT).show();
                    int makh = SessionManager.getMaNV(CustomerHomeActivity.this);
                    socket.emit("join_customer", makh); // Gửi mã khách hàng để phân phòng nhận tin nhắn
                });
            };
            socket.on(io.socket.client.Socket.EVENT_CONNECT, connectListener);

            // Nhận tin nhắn thông báo cập nhật đặt bàn real-time từ Socket
            socket.on("booking_update", args -> {
                try {
                    org.json.JSONObject data = (org.json.JSONObject) args[0];
                    String body = data.getString("body");
                    runOnUiThread(() ->
                        Toast.makeText(CustomerHomeActivity.this, body, Toast.LENGTH_LONG).show()
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        bookingAlertManager = new com.sinhvien.orderdrinkapp.Utils.BookingAlertManager(this);
        fragmentManager = getSupportFragmentManager();

        // Cài đặt kiểu điều hướng linh hoạt (Dựa vào lưu trữ cấu hình SharedPreferences)
        useBottomNav = SessionManager.isUseBottomNav(this);
        applyNavMode(useBottomNav);

        // Lắng nghe sự kiện nút chuyển đổi kiểu thanh điều hướng (Thanh điều hướng dưới hoặc Ngăn kéo)
        btnToggleNav.setOnClickListener(v -> {
            useBottomNav = !useBottomNav;
            SessionManager.setUseBottomNav(this, useBottomNav);
            applyNavMode(useBottomNav);
        });

        // Thiết lập sự kiện click cho các item của thanh Bottom Navigation
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_customer_booking) {
                navigateTo(new CustomerBookingFragment(), "CustomerBookingFragment");
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Đặt bàn & món");
            } else if (id == R.id.nav_customer_history) {
                navigateTo(new CustomerProfileFragment(), "CustomerProfileFragment");
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Lịch sử & chi tiêu");
            } else if (id == R.id.nav_customer_contact) {
                navigateTo(new CustomerContactFragment(), "CustomerContactFragment");
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Liên hệ quán");
            } else if (id == R.id.nav_logout) {
                logout();
            }
            return true;
        });

        // Khôi phục Fragment đang chạy hoặc nạp Fragment mặc định
        if (savedInstanceState == null) {
            // Mặc định mở fragment đặt bàn khi mới vào trang chủ
            navigateTo(new CustomerBookingFragment(), "CustomerBookingFragment");
            navigationView.setCheckedItem(R.id.nav_customer_booking);
            bottomNav.setSelectedItemId(R.id.nav_customer_booking);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Đặt bàn & món");
        } else {
            String activeTag = savedInstanceState.getString("activeFragmentTag");
            if (activeTag != null) {
                currentFragment = fragmentManager.findFragmentByTag(activeTag);
                if ("CustomerBookingFragment".equals(activeTag)) {
                    navigationView.setCheckedItem(R.id.nav_customer_booking);
                    bottomNav.setSelectedItemId(R.id.nav_customer_booking);
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle("Đặt bàn & món");
                } else if ("CustomerProfileFragment".equals(activeTag)) {
                    navigationView.setCheckedItem(R.id.nav_customer_history);
                    bottomNav.setSelectedItemId(R.id.nav_customer_history);
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle("Lịch sử & chi tiêu");
                } else if ("CustomerContactFragment".equals(activeTag)) {
                    navigationView.setCheckedItem(R.id.nav_customer_contact);
                    bottomNav.setSelectedItemId(R.id.nav_customer_contact);
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle("Liên hệ quán");
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu trữ Tag của Fragment hiện tại để tránh tải lại từ đầu khi xoay thiết bị
        if (currentFragment != null) {
            outState.putString("activeFragmentTag", currentFragment.getTag());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bookingAlertManager != null) {
            bookingAlertManager.startChecking();
        }
        checkBookingChangesOnResume(); // Thực hiện so sánh dữ liệu đặt bàn cũ và mới khi người dùng mở lại Activity
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bookingAlertManager != null) {
            bookingAlertManager.stopChecking();
        }
    }

    /**
     * Thay đổi kiểu hiển thị điều hướng giữa Bottom Navigation và Navigation Drawer.
     */
    private void applyNavMode(boolean useBottom) {
        if (useBottom) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Khóa kéo vuốt ngang
            bottomNav.setVisibility(View.VISIBLE);
            toolbar.setNavigationIcon(null); // Ẩn biểu tượng ngăn kéo trên Toolbar
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED); // Mở ngăn kéo
            bottomNav.setVisibility(View.GONE);
            drawerToggle.syncState(); // Đồng bộ lại biểu tượng ngăn kéo
        }
    }

    /**
     * Xử lý chuyển đổi qua lại giữa các Fragment một cách mượt mà và lưu lại trạng thái (không khởi tạo lại Fragment nếu đã có sẵn).
     */
    private void navigateTo(Fragment newFragment, String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out);
        if (currentFragment != null) {
            transaction.hide(currentFragment); // Ẩn fragment cũ
        }
        Fragment existing = fragmentManager.findFragmentByTag(tag);
        if (existing == null) {
            transaction.add(R.id.customer_contentView, newFragment, tag);
            currentFragment = newFragment;
        } else {
            transaction.show(existing); // Hiển thị lại fragment cũ đã tải
            currentFragment = existing;
        }
        transaction.commit();
    }

    /**
     * Đăng xuất tài khoản khách hàng, xóa toàn bộ cache giao diện cũ và xóa phiên trong SharedPreferences.
     */
    private void logout() {
        com.sinhvien.orderdrinkapp.Fragments.DisplayHomeFragment.clearCache();
        com.sinhvien.orderdrinkapp.Fragments.ManageBookingsFragment.clearCache();
        com.sinhvien.orderdrinkapp.Fragments.DisplayStatisticFragment.clearCache();
        SessionManager.clearSession(this);
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_customer_booking) {
            navigateTo(new CustomerBookingFragment(), "CustomerBookingFragment");
            bottomNav.setSelectedItemId(R.id.nav_customer_booking);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Đặt bàn & món");
        } else if (id == R.id.nav_customer_history) {
            navigateTo(new CustomerProfileFragment(), "CustomerProfileFragment");
            bottomNav.setSelectedItemId(R.id.nav_customer_history);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Lịch sử & chi tiêu");
        } else if (id == R.id.nav_customer_contact) {
            navigateTo(new CustomerContactFragment(), "CustomerContactFragment");
            bottomNav.setSelectedItemId(R.id.nav_customer_contact);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Liên hệ quán");
        } else if (id == R.id.nav_logout) {
            logout();
            return true;
        }

        drawerLayout.closeDrawers();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (socket != null && connectListener != null) {
            socket.off(io.socket.client.Socket.EVENT_CONNECT, connectListener);
            socket.off("booking_update");
        }
        com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().disconnect(); // Hủy kết nối Socket khi thoát ứng dụng
    }

    /**
     * Đồng bộ và quét trạng thái đặt bàn trực tiếp từ Server khi người dùng mở lại ứng dụng.
     * So sánh với trạng thái cũ được lưu trữ trong SharedPreferences để đẩy thông báo AlertDialog popup lên cho khách hàng.
     */
    private void checkBookingChangesOnResume() {
        int makh = SessionManager.getMaNV(this);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getBookings(makh).enqueue(new Callback<List<BookingResponse>>() {
            @Override
            public void onResponse(Call<List<BookingResponse>> call, Response<List<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BookingResponse> bookings = response.body();
                    SharedPreferences prefs = getSharedPreferences("booking_status_cache", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    
                    List<String> messages = new ArrayList<>();
                    
                    for (BookingResponse booking : bookings) {
                        String key = "booking_" + booking.getMaDatBan();
                        String oldStatus = prefs.getString(key, "");
                        String newStatus = booking.getTinhtrang() != null ? booking.getTinhtrang() : "";
                        
                        // Nếu trạng thái mới khác trạng thái cũ -> Tạo thông báo cụ thể
                        if (!oldStatus.isEmpty() && !oldStatus.equalsIgnoreCase(newStatus)) {
                            String tenban = booking.getTenBan() != null ? booking.getTenBan() : "";
                            String thoigianhen = booking.getThoigianhen() != null ? booking.getThoigianhen() : "";
                            
                            if (newStatus.equalsIgnoreCase("confirmed")) {
                                messages.add("✅ Bàn " + tenban + " lúc " + thoigianhen + " đã được xác nhận!");
                            } else if (newStatus.equalsIgnoreCase("cancelled")) {
                                messages.add("❌ Đặt bàn " + tenban + " đã bị hủy.");
                            } else if (newStatus.equalsIgnoreCase("checked_in") || newStatus.equalsIgnoreCase("checkin")) {
                                messages.add("🪑 Bàn " + tenban + " đã sẵn sàng, mời bạn vào!");
                            }
                        }
                        
                        editor.putString(key, newStatus);
                    }
                    
                    editor.apply();
                    
                    // Hiển thị hộp thoại AlertDialog nếu có tin nhắn mới thay đổi
                    if (!messages.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < messages.size(); i++) {
                            sb.append(messages.get(i));
                            if (i < messages.size() - 1) sb.append("\n\n");
                        }
                        
                        new AlertDialog.Builder(CustomerHomeActivity.this)
                            .setTitle("🔔 Thông báo đặt bàn")
                            .setMessage(sb.toString())
                            .setPositiveButton("OK", null)
                            .show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<BookingResponse>> call, Throwable t) {
                // Bỏ qua lỗi kết nối không làm phiền trải nghiệm người dùng
            }
        });
    }
}

