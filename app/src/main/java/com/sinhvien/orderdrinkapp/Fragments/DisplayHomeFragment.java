package com.sinhvien.orderdrinkapp.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.sinhvien.orderdrinkapp.Activities.AddCategoryActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterRecycleViewCategory;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterRecycleViewStatistic;
import com.sinhvien.orderdrinkapp.DTO.DonDatDTO;
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.LoaiMonResponse;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import android.util.Log;
import androidx.lifecycle.ViewModelProvider;
import com.sinhvien.orderdrinkapp.ViewModel.CategoryViewModel;
import com.sinhvien.orderdrinkapp.ViewModel.HomeViewModel;

public class DisplayHomeFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "DisplayHomeFragment";
    private CategoryViewModel categoryViewModel;
    private HomeViewModel homeViewModel;

    RecyclerView rcv_display_HomeCategoryList, rcv_display_HomeOrderToday;
    MaterialCardView layout_display_HomeStatistic, layout_display_HomeViewTable, layout_display_HomeViewMenu, layout_display_HomeViewStaff;
    TextView txt_display_HomeViewAllCategory, txt_display_HomeViewAllStatistic;
    List<LoaiMonDTO> loaiMonDTOList;
    List<DonDatDTO> donDatDTOS;
    AdapterRecycleViewCategory adapterRecycleViewCategory;
    AdapterRecycleViewStatistic adapterRecycleViewStatistic;
    android.widget.ScrollView sv_display_home;
    private int savedScrollY = -1;

    public static void clearCache() {
        // ViewModel handles lifecycle now
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.displayhome_layout, container, false);
        if (getActivity() != null && ((HomeActivity) getActivity()).getSupportActionBar() != null) {
            ((HomeActivity) getActivity()).getSupportActionBar().setTitle(R.string.app_name);
        }
        setHasOptionsMenu(true);

        //region Bind views
        sv_display_home = view.findViewById(R.id.sv_display_home);
        rcv_display_HomeCategoryList = view.findViewById(R.id.rcv_display_HomeCategoryList);
        rcv_display_HomeOrderToday = view.findViewById(R.id.rcv_display_HomeOrderToday);
        layout_display_HomeStatistic = view.findViewById(R.id.layout_display_HomeStatistic);
        layout_display_HomeViewTable = view.findViewById(R.id.layout_display_HomeViewTable);
        layout_display_HomeViewMenu = view.findViewById(R.id.layout_display_HomeViewMenu);
        layout_display_HomeViewStaff = view.findViewById(R.id.layout_display_HomeViewStaff);
        txt_display_HomeViewAllCategory = view.findViewById(R.id.txt_display_HomeViewAllCategory);
        txt_display_HomeViewAllStatistic = view.findViewById(R.id.txt_display_HomeViewAllStatistic);
        
        TextView txt_welcome_UserName = view.findViewById(R.id.txt_welcome_UserName);
        //endregion

        if (savedInstanceState != null) {
            savedScrollY = savedInstanceState.getInt("saved_scroll_y", -1);
        }

        // Hiển thị tên người dùng từ Session
        String fullName = SessionManager.getFullName(getActivity());
        if (fullName != null && !fullName.isEmpty()) {
            txt_welcome_UserName.setText(fullName);
        }

        // Phân quyền và căn chỉnh Grid cho Nhân viên
        if (!SessionManager.isAdmin(getActivity())) {
            layout_display_HomeViewStaff.setVisibility(View.GONE);
            layout_display_HomeStatistic.setVisibility(View.GONE);
            txt_display_HomeViewAllStatistic.setVisibility(View.GONE);
            
            // Chuyển sang 1 cột để 2 nút to ra và lấp đầy khoảng trống
            android.widget.GridLayout gridLayout = view.findViewById(R.id.grid_display_HomeActions);
            gridLayout.setColumnCount(1);

            // Chuyển 130dp sang pixel để set height
            int heightPx = (int) (130 * getResources().getDisplayMetrics().density);

            ViewGroup.LayoutParams paramsTable = layout_display_HomeViewTable.getLayoutParams();
            paramsTable.height = heightPx; 
            layout_display_HomeViewTable.setLayoutParams(paramsTable);

            ViewGroup.LayoutParams paramsMenu = layout_display_HomeViewMenu.getLayoutParams();
            paramsMenu.height = heightPx;
            layout_display_HomeViewMenu.setLayoutParams(paramsMenu);
        }

        // Initialize lists and adapters once
        rcv_display_HomeCategoryList.setHasFixedSize(true);
        rcv_display_HomeCategoryList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        loaiMonDTOList = new ArrayList<>();
        adapterRecycleViewCategory = new AdapterRecycleViewCategory(getActivity(), R.layout.custom_layout_displaycategory, loaiMonDTOList);
        rcv_display_HomeCategoryList.setAdapter(adapterRecycleViewCategory);

        rcv_display_HomeOrderToday.setHasFixedSize(true);
        rcv_display_HomeOrderToday.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        donDatDTOS = new ArrayList<>();
        adapterRecycleViewStatistic = new AdapterRecycleViewStatistic(getActivity(), R.layout.custom_layout_displaystatistic, donDatDTOS);
        rcv_display_HomeOrderToday.setAdapter(adapterRecycleViewStatistic);

        layout_display_HomeStatistic.setOnClickListener(this);
        layout_display_HomeViewTable.setOnClickListener(this);
        layout_display_HomeViewMenu.setOnClickListener(this);
        layout_display_HomeViewStaff.setOnClickListener(this);
        txt_display_HomeViewAllCategory.setOnClickListener(this);
        txt_display_HomeViewAllStatistic.setOnClickListener(this);

        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), list -> {
            loaiMonDTOList.clear();
            loaiMonDTOList.addAll(list);
            adapterRecycleViewCategory.notifyDataSetChanged();
            checkAndRestoreScroll();
        });

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.getTodayOrders().observe(getViewLifecycleOwner(), list -> {
            donDatDTOS.clear();
            donDatDTOS.addAll(list);
            adapterRecycleViewStatistic.notifyDataSetChanged();
            checkAndRestoreScroll();
        });

        return view;
    }

    private io.socket.client.Socket mSocket;
    private io.socket.emitter.Emitter.Listener onRefreshOrders = new io.socket.emitter.Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HienThiDonTrongNgay(true);
                    }
                });
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (sv_display_home != null) {
            outState.putInt("saved_scroll_y", sv_display_home.getScrollY());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        HienThiDSLoai();
        HienThiDonTrongNgay(false);

        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (mSocket != null) {
            mSocket.on("refresh_orders", onRefreshOrders);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSocket != null) {
            mSocket.off("refresh_orders", onRefreshOrders);
        }
    }

    private void checkAndRestoreScroll() {
        if (savedScrollY != -1 && sv_display_home != null) {
            final int y = savedScrollY;
            savedScrollY = -1;
            sv_display_home.post(new Runnable() {
                @Override
                public void run() {
                    sv_display_home.scrollTo(0, y);
                }
            });
        }
    }

    private void HienThiDSLoai() {
        categoryViewModel.syncCategoriesFromServer(null);
    }

    private void HienThiDonTrongNgay() {
        HienThiDonTrongNgay(false);
    }

    private void HienThiDonTrongNgay(boolean forceRefresh) {
        homeViewModel.fetchTodayOrders(forceRefresh);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (getActivity() instanceof HomeActivity) {
            HomeActivity homeActivity = (HomeActivity) getActivity();
            
            if (id == R.id.layout_display_HomeStatistic || id == R.id.txt_display_HomeViewAllStatistic) {
                homeActivity.selectBottomNavItem(R.id.nav_statistic);
            } else if (id == R.id.layout_display_HomeViewTable) {
                homeActivity.selectBottomNavItem(R.id.nav_table);
            } else if (id == R.id.layout_display_HomeViewMenu) {
                homeActivity.selectBottomNavItem(R.id.nav_category);
            } else if (id == R.id.layout_display_HomeViewStaff) {
                homeActivity.selectBottomNavItem(R.id.nav_staff);
            } else if (id == R.id.txt_display_HomeViewAllCategory) {
                homeActivity.selectBottomNavItem(R.id.nav_category);
            }
        }
    }
}
