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

public class CustomerHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    FragmentManager fragmentManager;
    TextView txt_menu_tennv;
    ActionBarDrawerToggle drawerToggle;

    private BottomNavigationView bottomNav;
    private ImageView btnToggleNav;
    private boolean useBottomNav;
    private Fragment currentFragment;
    private com.sinhvien.orderdrinkapp.Utils.BookingAlertManager bookingAlertManager;
    private io.socket.emitter.Emitter.Listener connectListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customer_home_layout);

        drawerLayout = findViewById(R.id.customer_drawer_layout);
        navigationView = findViewById(R.id.navigation_view_customer);
        toolbar = findViewById(R.id.customer_toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bottomNav = findViewById(R.id.customer_bottom_nav);
        btnToggleNav = findViewById(R.id.btn_toggle_nav);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        txt_menu_tennv = headerView.findViewById(R.id.txt_menu_tennv);

        String hoten = SessionManager.getFullName(this);
        if (hoten.isEmpty()) hoten = "Khách hàng";
        txt_menu_tennv.setText(hoten);

        // Khởi tạo kết nối Socket.io real-time
        com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().connect();
        io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (socket != null) {
            connectListener = args -> {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerHomeActivity.this, "🔌 Đã kết nối Real-time!", Toast.LENGTH_SHORT).show();
                    int makh = SessionManager.getMaNV(CustomerHomeActivity.this);
                    socket.emit("join_customer", makh);
                });
            };
            socket.on(io.socket.client.Socket.EVENT_CONNECT, connectListener);
        }

        bookingAlertManager = new com.sinhvien.orderdrinkapp.Utils.BookingAlertManager(this);

        fragmentManager = getSupportFragmentManager();

        // Setup Adaptive Navigation
        useBottomNav = SessionManager.isUseBottomNav(this);
        applyNavMode(useBottomNav);

        btnToggleNav.setOnClickListener(v -> {
            useBottomNav = !useBottomNav;
            SessionManager.setUseBottomNav(this, useBottomNav);
            applyNavMode(useBottomNav);
        });

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_customer_booking) {
                navigateTo(new CustomerBookingFragment(), "CustomerBookingFragment");
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Đặt bàn & món");
            } else if (id == R.id.nav_customer_history) {
                navigateTo(new CustomerProfileFragment(), "CustomerProfileFragment");
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Lịch sử & chi tiêu");
            } else if (id == R.id.nav_logout) {
                logout();
            }
            return true;
        });

        // Mặc định mở fragment đặt bàn
        navigateTo(new CustomerBookingFragment(), "CustomerBookingFragment");
        navigationView.setCheckedItem(R.id.nav_customer_booking);
        bottomNav.setSelectedItemId(R.id.nav_customer_booking);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Đặt bàn & món");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bookingAlertManager != null) {
            bookingAlertManager.startChecking();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bookingAlertManager != null) {
            bookingAlertManager.stopChecking();
        }
    }

    private void applyNavMode(boolean useBottom) {
        if (useBottom) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            bottomNav.setVisibility(View.VISIBLE);
            toolbar.setNavigationIcon(null);
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            bottomNav.setVisibility(View.GONE);
            drawerToggle.syncState();
        }
    }

    private void navigateTo(Fragment newFragment, String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out);
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        Fragment existing = fragmentManager.findFragmentByTag(tag);
        if (existing == null) {
            transaction.add(R.id.customer_contentView, newFragment, tag);
            currentFragment = newFragment;
        } else {
            transaction.show(existing);
            currentFragment = existing;
        }
        transaction.commit();
    }

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
        }
        com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().disconnect();
    }
}
