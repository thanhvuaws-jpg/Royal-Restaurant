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

import com.google.android.material.navigation.NavigationView;
import com.sinhvien.orderdrinkapp.Fragments.DisplayCategoryFragment;
import com.sinhvien.orderdrinkapp.Fragments.DisplayHomeFragment;
import com.sinhvien.orderdrinkapp.Fragments.DisplayStaffFragment;
import com.sinhvien.orderdrinkapp.Fragments.DisplayStatisticFragment;
import com.sinhvien.orderdrinkapp.Fragments.DisplayTableFragment;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    FragmentManager fragmentManager;
    TextView txt_menu_tennv;

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

        // Phân quyền menu
        if (!SessionManager.isAdmin(this)) {
            navigationView.getMenu().findItem(R.id.nav_staff).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_statistic).setVisible(false);
        }

        fragmentManager = getSupportFragmentManager();
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
            }
        });

        FragmentTransaction tranDisplayHome = fragmentManager.beginTransaction();
        DisplayHomeFragment displayHomeFragment = new DisplayHomeFragment();
        tranDisplayHome.replace(R.id.contentView,displayHomeFragment);
        tranDisplayHome.commit();
        navigationView.setCheckedItem(R.id.nav_home);
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
            FragmentTransaction tranDisplayHome = fragmentManager.beginTransaction();
            DisplayHomeFragment displayHomeFragment = new DisplayHomeFragment();
            tranDisplayHome.replace(R.id.contentView,displayHomeFragment);
            tranDisplayHome.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_statistic) {
            FragmentTransaction tranDisplayStatistic = fragmentManager.beginTransaction();
            DisplayStatisticFragment displayStatisticFragment = new DisplayStatisticFragment();
            tranDisplayStatistic.replace(R.id.contentView,displayStatisticFragment);
            tranDisplayStatistic.addToBackStack(null);
            tranDisplayStatistic.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_table) {
            FragmentTransaction tranDisplayTable = fragmentManager.beginTransaction();
            DisplayTableFragment displayTableFragment = new DisplayTableFragment();
            tranDisplayTable.replace(R.id.contentView,displayTableFragment);
            tranDisplayTable.addToBackStack(null);
            tranDisplayTable.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_category) {
            FragmentTransaction tranDisplayCategory = fragmentManager.beginTransaction();
            DisplayCategoryFragment displayCategoryFragment = new DisplayCategoryFragment();
            tranDisplayCategory.replace(R.id.contentView,displayCategoryFragment);
            tranDisplayCategory.addToBackStack(null);
            tranDisplayCategory.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_staff) {
            FragmentTransaction tranDisplayStaff = fragmentManager.beginTransaction();
            DisplayStaffFragment displayStaffFragment = new DisplayStaffFragment();
            tranDisplayStaff.replace(R.id.contentView,displayStaffFragment);
            tranDisplayStaff.addToBackStack(null);
            tranDisplayStaff.commit();
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_logout) {
            // XÓA PHIÊN ĐĂNG NHẬP
            SessionManager.clearSession(this);
            
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        return true;
    }
}