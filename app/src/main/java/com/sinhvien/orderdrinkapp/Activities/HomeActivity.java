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
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.Fragments.DisplayCashierFragment;
import com.sinhvien.orderdrinkapp.Fragments.DisplayCategoryFragment;
import com.sinhvien.orderdrinkapp.Fragments.DisplayHomeFragment;
import com.sinhvien.orderdrinkapp.Fragments.DisplayStaffFragment;
import com.sinhvien.orderdrinkapp.Fragments.DisplayStatisticFragment;
import com.sinhvien.orderdrinkapp.Fragments.DisplayTableFragment;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

// [FIX BUG 3] Imports
import android.content.SharedPreferences;
import java.util.List;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    FragmentManager fragmentManager;
    TextView txt_menu_tennv;
    ActionBarDrawerToggle drawerToggle;

    private BottomNavigationView bottomNav;
    private ImageView btnToggleNav;
    private boolean useBottomNav;
    private boolean bypassMoreSheet = false;
    private boolean isSyncingNav = false;
    private BottomSheetDialog moreBottomSheet;
    private Fragment currentFragment;

    private Handler sessionHandler;
    private Runnable sessionRunnable;
    private static final int SESSION_CHECK_INTERVAL = 10000; // 10s
    private com.sinhvien.orderdrinkapp.Utils.BookingAlertManager bookingAlertManager;
    private io.socket.emitter.Emitter.Listener connectListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view_trangchu);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bottomNav = findViewById(R.id.bottom_nav);
        btnToggleNav = findViewById(R.id.btn_toggle_nav);

        drawerToggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar
                ,R.string.open,R.string.close){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        txt_menu_tennv = headerView.findViewById(R.id.txt_menu_tennv);

        // Lấy tên hiển thị từ Session thay vì Intent để hỗ trợ Auto-login
        String hoten = SessionManager.getFullName(this);
        if (hoten.isEmpty()) hoten = "Nhân viên";
        txt_menu_tennv.setText(hoten);

        // Khởi tạo kết nối Socket.io real-time
        com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().connect();
        io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (socket != null) {
            connectListener = args -> {
                // Đăng ký phòng tương ứng quyền để nhận sự kiện phù hợp (Chạy ngay lập tức, không đợi UI thread)
                if (SessionManager.isCashier(HomeActivity.this)) {
                    socket.emit("join_cashier");
                } else {
                    // [FIX] Admin (1) và NV (2) đều join admin_room
                    socket.emit("join_admin");
                }
                
                runOnUiThread(() -> {
                    Toast.makeText(HomeActivity.this, "🔌 Đã kết nối Real-time!", Toast.LENGTH_SHORT).show();
                });
            };
            socket.on(io.socket.client.Socket.EVENT_CONNECT, connectListener);

            // [FIX BUG 1] Socket đã connected sẵn → join room ngay
            if (socket.connected()) {
                if (SessionManager.isCashier(HomeActivity.this)) {
                    socket.emit("join_cashier");
                } else {
                    // [FIX] Admin (1) và NV (2) đều join admin_room
                    socket.emit("join_admin");
                }
            }
        }

        // Khởi động kiểm tra session sẽ được tự động chạy trong onResume()
        if (SessionManager.isAdmin(this) || SessionManager.isCashier(this)
                || SessionManager.getMaQuyen(this) == 2) {
            // [FIX] Thêm NV (maquyen=2) để nhận notify_prepare_table
            bookingAlertManager = new com.sinhvien.orderdrinkapp.Utils.BookingAlertManager(this);
        }

        fragmentManager = getSupportFragmentManager();

        // Setup Bottom Nav and Toggle
        useBottomNav = SessionManager.isUseBottomNav(this);
        applyNavMode(useBottomNav);

        btnToggleNav.setOnClickListener(v -> {
            useBottomNav = !useBottomNav;
            SessionManager.setUseBottomNav(this, useBottomNav);
            applyNavMode(useBottomNav);
        });

         bottomNav.setOnItemSelectedListener(item -> {
            if (isSyncingNav) {
                return true;
            }
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Fragment f = fragmentManager.findFragmentByTag("HomeFragment");
                navigateTo(f != null ? f : new DisplayHomeFragment(), "HomeFragment");
            } else if (id == R.id.nav_table) {
                Fragment f = fragmentManager.findFragmentByTag("TableFragment");
                navigateTo(f != null ? f : new DisplayTableFragment(), "TableFragment");
            } else if (id == R.id.nav_category) {
                Fragment f = fragmentManager.findFragmentByTag("CategoryFragment");
                navigateTo(f != null ? f : new DisplayCategoryFragment(), "CategoryFragment");
            } else if (id == R.id.nav_statistic) {
                Fragment f = fragmentManager.findFragmentByTag("StatisticFragment");
                navigateTo(f != null ? f : new DisplayStatisticFragment(), "StatisticFragment");
            } else if (id == R.id.nav_more) {
                if (bypassMoreSheet) {
                    bypassMoreSheet = false;
                } else {
                    showMoreBottomSheet();
                }
            }
            return true;
        });

        // Phân quyền menu
        if (SessionManager.isCashier(this)) {
            // Thu ngân
            navigationView.getMenu().findItem(R.id.nav_staff).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_table).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_category).setVisible(false);
            
            bottomNav.getMenu().findItem(R.id.nav_table).setVisible(false);
            bottomNav.getMenu().findItem(R.id.nav_more).setVisible(false);
            
            if (savedInstanceState == null) {
                navigateTo(new DisplayCashierFragment(), "CashierFragment");
                navigationView.setCheckedItem(R.id.nav_cashier);
            }
        } else {
            // Admin hoặc Nhân viên
            navigationView.getMenu().findItem(R.id.nav_cashier).setVisible(false);
            if (!SessionManager.isAdmin(this)) {
                // Nhân viên
                navigationView.getMenu().findItem(R.id.nav_staff).setVisible(false);
                navigationView.getMenu().findItem(R.id.nav_statistic).setVisible(false);
                
                bottomNav.getMenu().findItem(R.id.nav_statistic).setVisible(false);
            }
            if (savedInstanceState == null) {
                navigateTo(new DisplayHomeFragment(), "HomeFragment");
                navigationView.setCheckedItem(R.id.nav_home);
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        }

        if (savedInstanceState != null) {
            for (Fragment f : fragmentManager.getFragments()) {
                if (f != null && f.isAdded() && !f.isHidden()) {
                    currentFragment = f;
                    break;
                }
            }
            if (currentFragment != null) {
                updateToolbarTitle(currentFragment);
                syncNavSelection();
            }
        }

        fragmentManager.addOnBackStackChangedListener(() -> {
            // Find the visible fragment to update currentFragment
            for (Fragment f : fragmentManager.getFragments()) {
                if (f != null && f.isVisible()) {
                    currentFragment = f;
                    break;
                }
            }

            if (fragmentManager.getBackStackEntryCount() > 0) {
                drawerToggle.setDrawerIndicatorEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            } else {
                // Backstack trống → sync lại currentFragment và UI
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                if (useBottomNav) {
                    drawerToggle.setDrawerIndicatorEnabled(false);
                    toolbar.setNavigationIcon(null);
                } else {
                    drawerToggle.setDrawerIndicatorEnabled(true);
                    drawerToggle.syncState();
                    toolbar.setNavigationOnClickListener(v ->
                        drawerLayout.openDrawer(androidx.core.view.GravityCompat.START));
                }
                // Sync BottomNav với currentFragment
                syncNavSelection();
            }
            if (currentFragment != null) {
                updateToolbarTitle(currentFragment);
            }
        });

        // Removed default home fragment load to prevent overriding the role-based logic above
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return true;
        }
        return super.onSupportNavigateUp();
    }

    private void applyNavMode(boolean useBottom) {
        if (useBottom) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            bottomNav.setVisibility(View.VISIBLE);
            if (fragmentManager.getBackStackEntryCount() == 0) {
                drawerToggle.setDrawerIndicatorEnabled(false);
                toolbar.setNavigationIcon(null);
            }
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            bottomNav.setVisibility(View.GONE);
            if (fragmentManager.getBackStackEntryCount() == 0) {
                drawerToggle.setDrawerIndicatorEnabled(true);
                drawerToggle.syncState();
                toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(androidx.core.view.GravityCompat.START));
            }
        }
    }

    private void navigateTo(Fragment newFragment, String tag) {
        if (currentFragment != null && tag.equals(currentFragment.getTag())) {
            return;
        }

        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out);
        
        for (Fragment f : fragmentManager.getFragments()) {
            if (f != null && f.isAdded()) {
                transaction.hide(f);
            }
        }

        Fragment existing = fragmentManager.findFragmentByTag(tag);
        if (existing == null) {
            transaction.add(R.id.contentView, newFragment, tag);
            currentFragment = newFragment;
        } else {
            transaction.show(existing);
            currentFragment = existing;
        }
        transaction.commit();
        updateToolbarTitle(currentFragment);
    }

    public void navigateToSubFragment(Fragment fragment, String tag) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                               R.anim.slide_in_left, R.anim.slide_out_right);
        if (currentFragment != null) {
            tx.hide(currentFragment);
        }
        tx.add(R.id.contentView, fragment, tag);
        tx.addToBackStack(tag);
        tx.commit();
        currentFragment = fragment;
        updateToolbarTitle(currentFragment);
    }

    private void updateToolbarTitle(Fragment fragment) {
        if (getSupportActionBar() == null) return;
        if (fragment instanceof DisplayHomeFragment) {
            getSupportActionBar().setTitle(R.string.app_name);
        } else if (fragment instanceof DisplayTableFragment) {
            getSupportActionBar().setTitle(R.string.table_list_title);
        } else if (fragment instanceof DisplayCategoryFragment || fragment instanceof com.sinhvien.orderdrinkapp.Fragments.DisplayMenuFragment) {
            getSupportActionBar().setTitle(R.string.nav_menu);
        } else if (fragment instanceof DisplayStatisticFragment) {
            getSupportActionBar().setTitle(R.string.statistic_title);
        } else if (fragment instanceof DisplayCashierFragment) {
            getSupportActionBar().setTitle("Bảng Điều Khiển Thu Ngân");
        } else if (fragment instanceof com.sinhvien.orderdrinkapp.Fragments.ManageBookingsFragment) {
            getSupportActionBar().setTitle("Quản lý đặt bàn");
        } else if (fragment instanceof DisplayStaffFragment) {
            getSupportActionBar().setTitle("Quản lý nhân viên");
        }
    }

    private void syncNavSelection() {
        isSyncingNav = true;
        try {
            if (currentFragment instanceof DisplayHomeFragment) {
                bottomNav.setSelectedItemId(R.id.nav_home);
                navigationView.setCheckedItem(R.id.nav_home);
            } else if (currentFragment instanceof DisplayTableFragment) {
                bottomNav.setSelectedItemId(R.id.nav_table);
                navigationView.setCheckedItem(R.id.nav_table);
            } else if (currentFragment instanceof DisplayCategoryFragment) {
                bottomNav.setSelectedItemId(R.id.nav_category);
                navigationView.setCheckedItem(R.id.nav_category);
            } else if (currentFragment instanceof DisplayStatisticFragment) {
                bottomNav.setSelectedItemId(R.id.nav_statistic);
                navigationView.setCheckedItem(R.id.nav_statistic);
            } else if (currentFragment instanceof DisplayStaffFragment) {
                bypassMoreSheet = true;
                bottomNav.setSelectedItemId(R.id.nav_more);
                navigationView.setCheckedItem(R.id.nav_staff);
            } else if (currentFragment instanceof com.sinhvien.orderdrinkapp.Fragments.ManageBookingsFragment) {
                bypassMoreSheet = true;
                bottomNav.setSelectedItemId(R.id.nav_more);
                navigationView.setCheckedItem(R.id.nav_manage_bookings);
            }
        } finally {
            isSyncingNav = false;
        }
    }

    public void selectBottomNavItem(int menuId) {
        if (useBottomNav && bottomNav != null) {
            if (menuId == R.id.nav_staff) {
                Fragment f = fragmentManager.findFragmentByTag("StaffFragment");
                navigateTo(f != null ? f : new DisplayStaffFragment(), "StaffFragment");
                isSyncingNav = true;
                bypassMoreSheet = true;
                bottomNav.setSelectedItemId(R.id.nav_more);
                isSyncingNav = false;
            } else if (menuId == R.id.nav_manage_bookings) {
                Fragment f = fragmentManager.findFragmentByTag("ManageBookingsFragment");
                navigateTo(f != null ? f : new com.sinhvien.orderdrinkapp.Fragments.ManageBookingsFragment(), "ManageBookingsFragment");
                isSyncingNav = true;
                bypassMoreSheet = true;
                bottomNav.setSelectedItemId(R.id.nav_more);
                isSyncingNav = false;
            } else {
                if (menuId == R.id.nav_home) {
                    Fragment f = fragmentManager.findFragmentByTag("HomeFragment");
                    navigateTo(f != null ? f : new DisplayHomeFragment(), "HomeFragment");
                } else if (menuId == R.id.nav_table) {
                    Fragment f = fragmentManager.findFragmentByTag("TableFragment");
                    navigateTo(f != null ? f : new DisplayTableFragment(), "TableFragment");
                } else if (menuId == R.id.nav_category) {
                    Fragment f = fragmentManager.findFragmentByTag("CategoryFragment");
                    navigateTo(f != null ? f : new DisplayCategoryFragment(), "CategoryFragment");
                } else if (menuId == R.id.nav_statistic) {
                    Fragment f = fragmentManager.findFragmentByTag("StatisticFragment");
                    navigateTo(f != null ? f : new DisplayStatisticFragment(), "StatisticFragment");
                }
                isSyncingNav = true;
                bottomNav.setSelectedItemId(menuId);
                isSyncingNav = false;
            }
        } else {
            MenuItem item = navigationView.getMenu().findItem(menuId);
            if (item != null) {
                onNavigationItemSelected(item);
                navigationView.setCheckedItem(menuId);
            }
        }
    }

    private void showMoreBottomSheet() {
        if (moreBottomSheet == null) {
            moreBottomSheet = new BottomSheetDialog(this);
            View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_more, null);
            
            View itemManageBookings = view.findViewById(R.id.item_manage_bookings);
            View itemStaff = view.findViewById(R.id.item_staff);
            View itemLogout = view.findViewById(R.id.item_logout);

            if (SessionManager.isCashier(this)) {
                itemManageBookings.setVisibility(View.GONE);
                itemStaff.setVisibility(View.GONE);
            } else if (!SessionManager.isAdmin(this)) {
                itemStaff.setVisibility(View.GONE);
            }

            itemManageBookings.setOnClickListener(v -> {
                navigateTo(new com.sinhvien.orderdrinkapp.Fragments.ManageBookingsFragment(), "ManageBookingsFragment");
                moreBottomSheet.dismiss();
            });
            itemStaff.setOnClickListener(v -> {
                navigateTo(new DisplayStaffFragment(), "StaffFragment");
                moreBottomSheet.dismiss();
            });
            itemLogout.setOnClickListener(v -> {
                moreBottomSheet.dismiss();
                logout();
            });
            moreBottomSheet.setContentView(view);
        }
        moreBottomSheet.show();
    }

    private void logout() {
        stopSessionCheck();
        com.sinhvien.orderdrinkapp.Fragments.DisplayHomeFragment.clearCache();
        com.sinhvien.orderdrinkapp.Fragments.ManageBookingsFragment.clearCache();
        com.sinhvien.orderdrinkapp.Fragments.DisplayStatisticFragment.clearCache();
        SessionManager.clearSession(this);
        
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (isSyncingNav) {
            return true;
        }
        int id = item.getItemId();
        isSyncingNav = true;
        try {
            if (id == R.id.nav_home) {
                Fragment f = fragmentManager.findFragmentByTag("HomeFragment");
                navigateTo(f != null ? f : new DisplayHomeFragment(), "HomeFragment");
                bottomNav.setSelectedItemId(R.id.nav_home);
            } else if (id == R.id.nav_statistic) {
                Fragment f = fragmentManager.findFragmentByTag("StatisticFragment");
                navigateTo(f != null ? f : new DisplayStatisticFragment(), "StatisticFragment");
                bottomNav.setSelectedItemId(R.id.nav_statistic);
            } else if (id == R.id.nav_cashier) {
                Fragment f = fragmentManager.findFragmentByTag("CashierFragment");
                navigateTo(f != null ? f : new DisplayCashierFragment(), "CashierFragment");
            } else if (id == R.id.nav_table) {
                Fragment f = fragmentManager.findFragmentByTag("TableFragment");
                navigateTo(f != null ? f : new DisplayTableFragment(), "TableFragment");
                bottomNav.setSelectedItemId(R.id.nav_table);
            } else if (id == R.id.nav_manage_bookings) {
                Fragment f = fragmentManager.findFragmentByTag("ManageBookingsFragment");
                navigateTo(f != null ? f : new com.sinhvien.orderdrinkapp.Fragments.ManageBookingsFragment(), "ManageBookingsFragment");
            } else if (id == R.id.nav_category) {
                Fragment f = fragmentManager.findFragmentByTag("CategoryFragment");
                navigateTo(f != null ? f : new DisplayCategoryFragment(), "CategoryFragment");
                bottomNav.setSelectedItemId(R.id.nav_category);
            } else if (id == R.id.nav_staff) {
                Fragment f = fragmentManager.findFragmentByTag("StaffFragment");
                navigateTo(f != null ? f : new DisplayStaffFragment(), "StaffFragment");
            } else if (id == R.id.nav_logout) {
                isSyncingNav = false;
                logout();
                return true;
            }
        } finally {
            isSyncingNav = false;
        }
        drawerLayout.closeDrawers();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSessionCheck();
        if (bookingAlertManager != null) {
            bookingAlertManager.startChecking();
        }
        checkMissedBookingNotifications(); // [FIX BUG 3]
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSessionCheck();
        if (bookingAlertManager != null) {
            bookingAlertManager.stopChecking();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSessionCheck();
        
        // [FIX] Hủy listener notify_prepare_table hoàn toàn khi thoát app
        if (bookingAlertManager != null) {
            bookingAlertManager.destroy();
        }

        io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (socket != null && connectListener != null) {
            socket.off(io.socket.client.Socket.EVENT_CONNECT, connectListener);
        }
        com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().disconnect();
    }

    private void startSessionCheck() {
        final int manv = SessionManager.getMaNV(this);
        final String token = SessionManager.getToken(this);
        
        if (manv == 0 || token.isEmpty()) return;

        sessionHandler = new Handler(Looper.getMainLooper());
        sessionRunnable = new Runnable() {
            @Override
            public void run() {
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                apiService.checkSession(manv, token).enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                        if (isFinishing() || isDestroyed()) return;
                        
                        boolean isSessionValid = false;
                        if (response.isSuccessful() && response.body() != null) {
                            if ("success".equals(response.body().getStatus())) {
                                isSessionValid = true;
                            }
                        }
                        
                        if (!isSessionValid) {
                            stopSessionCheck();
                            SessionManager.clearSession(HomeActivity.this);
                            Toast.makeText(HomeActivity.this, "Tài khoản của bạn đã được đăng nhập từ thiết bị khác!", Toast.LENGTH_LONG).show();
                            
                            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            if (sessionHandler != null && sessionRunnable != null) {
                                sessionHandler.postDelayed(sessionRunnable, SESSION_CHECK_INTERVAL);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<OrderResponse> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        if (sessionHandler != null && sessionRunnable != null) {
                            sessionHandler.postDelayed(sessionRunnable, SESSION_CHECK_INTERVAL);
                        }
                    }
                });
            }
        };
        sessionHandler.post(sessionRunnable);
    }

    private void stopSessionCheck() {
        if (sessionHandler != null && sessionRunnable != null) {
            sessionHandler.removeCallbacks(sessionRunnable);
            sessionHandler = null;
            sessionRunnable = null;
        }
    }

    // [FIX BUG 3] Hàm backup kiểm tra thông báo đặt bàn bị nhỡ
    private void checkMissedBookingNotifications() {
        if (!SessionManager.isAdmin(this) && SessionManager.getMaQuyen(this) != 2) {
            return; // Chỉ chạy cho Admin và Nhân Viên
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getBookings(0).enqueue(new Callback<List<BookingResponse>>() {
            @Override
            public void onResponse(Call<List<BookingResponse>> call, Response<List<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        List<BookingResponse> bookings = response.body();
                        SharedPreferences prefs = getSharedPreferences("nv_booking_cache", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();

                        for (BookingResponse booking : bookings) {
                            String key = "booking_" + booking.getMaDatBan();
                            String oldStatus = prefs.getString(key, "");
                            String newStatus = booking.getTinhtrang() != null ? booking.getTinhtrang() : "";

                            if (!oldStatus.isEmpty() && !oldStatus.equalsIgnoreCase(newStatus)) {
                                String tenban = booking.getTenBan() != null ? booking.getTenBan() : "";
                                String thoigianhen = booking.getThoigianhen() != null ? booking.getThoigianhen() : "";

                                if (newStatus.equalsIgnoreCase("confirmed") && oldStatus.equalsIgnoreCase("pending")) {
                                    String msg = "📋 Bàn " + tenban + " lúc " + thoigianhen + " đã được xác nhận, cần chuẩn bị!";
                                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_LONG).show());
                                } else if (newStatus.equalsIgnoreCase("checkin") || newStatus.equalsIgnoreCase("checked_in")) {
                                    String msg = "🪑 Bàn " + tenban + " khách đã check-in!";
                                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_LONG).show());
                                }
                            }
                            editor.putString(key, newStatus);
                        }
                        editor.apply();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<BookingResponse>> call, Throwable t) {
                // Ignore failure
            }
        });
    }
}