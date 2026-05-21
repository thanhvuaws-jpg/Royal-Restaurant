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
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    FragmentManager fragmentManager;
    TextView txt_menu_tennv;

    private Handler sessionHandler;
    private Runnable sessionRunnable;
    private static final int SESSION_CHECK_INTERVAL = 10000; // 10s

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

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar
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

        // Khởi động kiểm tra session sẽ được tự động chạy trong onResume()

        fragmentManager = getSupportFragmentManager();

        // Phân quyền menu
        if (SessionManager.isCashier(this)) {
            // Thu ngân chỉ thấy Trang chủ, Thu ngân, Thống kê, Đăng xuất
            navigationView.getMenu().findItem(R.id.nav_staff).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_table).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_category).setVisible(false);
            
            // Thu ngân mặc định mở trang Thu ngân
            FragmentTransaction tranDisplayCashier = fragmentManager.beginTransaction();
            tranDisplayCashier.replace(R.id.contentView, new DisplayCashierFragment());
            tranDisplayCashier.commit();
            navigationView.setCheckedItem(R.id.nav_cashier);
        } else {
            // Admin hoặc Nhân viên
            navigationView.getMenu().findItem(R.id.nav_cashier).setVisible(false);
            if (!SessionManager.isAdmin(this)) {
                // Nhân viên
                navigationView.getMenu().findItem(R.id.nav_staff).setVisible(false);
                navigationView.getMenu().findItem(R.id.nav_statistic).setVisible(false);
            }
            // Admin và Nhân viên mặc định mở Trang chủ
            FragmentTransaction tranDisplayHome = fragmentManager.beginTransaction();
            tranDisplayHome.replace(R.id.contentView, new DisplayHomeFragment());
            tranDisplayHome.commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }

        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onBackPressed();
                        }
                    });
                } else {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    drawerToggle.syncState();
                    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
                        }
                    });
                }

                // Cập nhật lại màu tô của NavigationView khi bấm Back
                androidx.fragment.app.Fragment currentFragment = fragmentManager.findFragmentById(R.id.contentView);
                if (currentFragment instanceof DisplayHomeFragment) {
                    navigationView.setCheckedItem(R.id.nav_home);
                } else if (currentFragment instanceof DisplayStatisticFragment) {
                    navigationView.setCheckedItem(R.id.nav_statistic);
                } else if (currentFragment instanceof DisplayTableFragment) {
                    navigationView.setCheckedItem(R.id.nav_table);
                } else if (currentFragment instanceof DisplayCategoryFragment) {
                    navigationView.setCheckedItem(R.id.nav_category);
                } else if (currentFragment instanceof DisplayStaffFragment) {
                    navigationView.setCheckedItem(R.id.nav_staff);
                } else if (currentFragment instanceof DisplayCashierFragment) {
                    navigationView.setCheckedItem(R.id.nav_cashier);
                }
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            String tag = "HomeFragment";
            androidx.fragment.app.Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment == null) fragment = new DisplayHomeFragment();
            FragmentTransaction tranDisplayHome = fragmentManager.beginTransaction();
            tranDisplayHome.replace(R.id.contentView,fragment, tag);
            tranDisplayHome.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_statistic) {
            String tag = "StatisticFragment";
            androidx.fragment.app.Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment == null) fragment = new DisplayStatisticFragment();
            FragmentTransaction tranDisplayStatistic = fragmentManager.beginTransaction();
            tranDisplayStatistic.replace(R.id.contentView,fragment, tag);
            tranDisplayStatistic.addToBackStack(null);
            tranDisplayStatistic.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_cashier) {
            String tag = "CashierFragment";
            androidx.fragment.app.Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment == null) fragment = new DisplayCashierFragment();
            FragmentTransaction tranDisplayCashier = fragmentManager.beginTransaction();
            tranDisplayCashier.replace(R.id.contentView,fragment, tag);
            tranDisplayCashier.addToBackStack(null);
            tranDisplayCashier.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_table) {
            String tag = "TableFragment";
            androidx.fragment.app.Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment == null) fragment = new DisplayTableFragment();
            FragmentTransaction tranDisplayTable = fragmentManager.beginTransaction();
            tranDisplayTable.replace(R.id.contentView,fragment, tag);
            tranDisplayTable.addToBackStack(null);
            tranDisplayTable.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_category) {
            String tag = "CategoryFragment";
            androidx.fragment.app.Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment == null) fragment = new DisplayCategoryFragment();
            FragmentTransaction tranDisplayCategory = fragmentManager.beginTransaction();
            tranDisplayCategory.replace(R.id.contentView,fragment, tag);
            tranDisplayCategory.addToBackStack(null);
            tranDisplayCategory.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_staff) {
            String tag = "StaffFragment";
            androidx.fragment.app.Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment == null) fragment = new DisplayStaffFragment();
            FragmentTransaction tranDisplayStaff = fragmentManager.beginTransaction();
            tranDisplayStaff.replace(R.id.contentView,fragment, tag);
            tranDisplayStaff.addToBackStack(null);
            tranDisplayStaff.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_logout) {
            // XÓA PHIÊN ĐĂNG NHẬP
            stopSessionCheck();
            SessionManager.clearSession(this);
            
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSessionCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSessionCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSessionCheck();
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
}