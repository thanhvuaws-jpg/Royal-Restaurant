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
import android.widget.TextView;
import android.widget.Toast;

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
    private com.sinhvien.orderdrinkapp.Utils.BookingAlertManager bookingAlertManager;

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

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
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
            socket.on(io.socket.client.Socket.EVENT_CONNECT, args -> {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerHomeActivity.this, "🔌 Đã kết nối Real-time!", Toast.LENGTH_SHORT).show();
                    int makh = SessionManager.getMaNV(CustomerHomeActivity.this);
                    socket.emit("join_customer", makh);
                });
            });
        }

        bookingAlertManager = new com.sinhvien.orderdrinkapp.Utils.BookingAlertManager(this);

        fragmentManager = getSupportFragmentManager();

        // Mặc định mở fragment đặt bàn
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.customer_contentView, new CustomerBookingFragment());
        transaction.commit();
        navigationView.setCheckedItem(R.id.nav_customer_booking);
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (id == R.id.nav_customer_booking) {
            transaction.replace(R.id.customer_contentView, new CustomerBookingFragment());
            transaction.commit();
            getSupportActionBar().setTitle("Đặt bàn & món");
        } else if (id == R.id.nav_customer_history) {
            transaction.replace(R.id.customer_contentView, new CustomerProfileFragment());
            transaction.commit();
            getSupportActionBar().setTitle("Lịch sử & chi tiêu");
        } else if (id == R.id.nav_logout) {
            SessionManager.clearSession(this);
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawers();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().disconnect();
    }
}
