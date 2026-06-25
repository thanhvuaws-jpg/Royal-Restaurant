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

/**
 * DisplayHomeFragment - Màn hình Trang chủ (Dashboard) của ứng dụng dành cho nhân viên / quản lý.
 * - Hiển thị lời chào cá nhân hóa (Welcome User) kèm họ tên người dùng.
 * - Phân quyền giao diện (Admin vs Nhân viên):
 *   + Ẩn các chức năng Thống kê, Quản lý Nhân viên đối với nhân viên thường.
 *   + Tự động thay đổi bố cục GridLayout từ 2 cột sang 1 cột cho nút chức năng để lấp đầy khoảng trống UI.
 * - Hiển thị danh mục món ăn (Category) dạng trượt ngang.
 * - Hiển thị danh sách các đơn hàng đã thanh toán hôm nay dạng trượt ngang.
 * - Tự động đồng bộ các đơn hàng hôm nay theo thời gian thực bằng Socket.io.
 * - Quản lý điều hướng mượt mà đến các tab tương ứng ở thanh Bottom Navigation.
 */
public class DisplayHomeFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "DisplayHomeFragment";
    
    // ViewModel quản lý loại món ăn
    private CategoryViewModel categoryViewModel;
    // ViewModel quản lý đơn đặt món trong ngày tại trang chủ
    private HomeViewModel homeViewModel;

    // Hai danh sách RecyclerView trượt ngang
    RecyclerView rcv_display_HomeCategoryList, rcv_display_HomeOrderToday;
    // Thẻ chức năng điều hướng nhanh
    MaterialCardView layout_display_HomeStatistic, layout_display_HomeViewTable, layout_display_HomeViewMenu, layout_display_HomeViewStaff;
    // Nút "Xem tất cả"
    TextView txt_display_HomeViewAllCategory, txt_display_HomeViewAllStatistic;
    
    List<LoaiMonDTO> loaiMonDTOList;
    List<DonDatDTO> donDatDTOS;
    AdapterRecycleViewCategory adapterRecycleViewCategory;
    AdapterRecycleViewStatistic adapterRecycleViewStatistic;
    
    android.widget.ScrollView sv_display_home;
    // Lưu lại vị trí cuộn trang dọc khi Fragment tái tạo
    private int savedScrollY = -1;

    public static void clearCache() {
        // ViewModel xử lý vòng đời dữ liệu, không cần lưu cache tĩnh
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.displayhome_layout, container, false);
        
        // Thiết lập tiêu đề trên thanh ActionBar
        if (getActivity() != null && ((HomeActivity) getActivity()).getSupportActionBar() != null) {
            ((HomeActivity) getActivity()).getSupportActionBar().setTitle(R.string.app_name);
        }
        setHasOptionsMenu(true);

        // Khởi tạo các view trên giao diện
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

        if (savedInstanceState != null) {
            savedScrollY = savedInstanceState.getInt("saved_scroll_y", -1);
        }

        // Lấy tên đầy đủ của người dùng hiển thị lời chào mừng
        String fullName = SessionManager.getFullName(getActivity());
        if (fullName != null && !fullName.isEmpty()) {
            txt_welcome_UserName.setText(fullName);
        }

        // Thực hiện phân quyền chức năng: Nhân viên vs Quản lý (Admin)
        if (!SessionManager.isAdmin(getActivity())) {
            // Ẩn tính năng quản lý nhân viên và thống kê doanh thu đối với nhân viên thường
            layout_display_HomeViewStaff.setVisibility(View.GONE);
            layout_display_HomeStatistic.setVisibility(View.GONE);
            txt_display_HomeViewAllStatistic.setVisibility(View.GONE);
            
            // Định dạng lại Grid để trải đều giao diện: chuyển sang hiển thị 1 cột duy nhất
            android.widget.GridLayout gridLayout = view.findViewById(R.id.grid_display_HomeActions);
            gridLayout.setColumnCount(1);

            // Nới rộng kích thước chiều cao các nút lớn gấp đôi để bố cục cân đối (130dp)
            int heightPx = (int) (130 * getResources().getDisplayMetrics().density);

            ViewGroup.LayoutParams paramsTable = layout_display_HomeViewTable.getLayoutParams();
            paramsTable.height = heightPx; 
            layout_display_HomeViewTable.setLayoutParams(paramsTable);

            ViewGroup.LayoutParams paramsMenu = layout_display_HomeViewMenu.getLayoutParams();
            paramsMenu.height = heightPx;
            layout_display_HomeViewMenu.setLayoutParams(paramsMenu);
        }

        // Cài đặt RecyclerView trượt ngang hiển thị Danh mục
        rcv_display_HomeCategoryList.setHasFixedSize(true);
        rcv_display_HomeCategoryList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        loaiMonDTOList = new ArrayList<>();
        adapterRecycleViewCategory = new AdapterRecycleViewCategory(getActivity(), R.layout.custom_layout_displaycategory, loaiMonDTOList);
        rcv_display_HomeCategoryList.setAdapter(adapterRecycleViewCategory);

        // Cài đặt RecyclerView trượt ngang hiển thị Hóa đơn hôm nay
        rcv_display_HomeOrderToday.setHasFixedSize(true);
        rcv_display_HomeOrderToday.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        donDatDTOS = new ArrayList<>();
        adapterRecycleViewStatistic = new AdapterRecycleViewStatistic(getActivity(), R.layout.custom_layout_displaystatistic, donDatDTOS);
        rcv_display_HomeOrderToday.setAdapter(adapterRecycleViewStatistic);

        // Đăng ký sự kiện click cho các nút điều hướng nhanh
        layout_display_HomeStatistic.setOnClickListener(this);
        layout_display_HomeViewTable.setOnClickListener(this);
        layout_display_HomeViewMenu.setOnClickListener(this);
        layout_display_HomeViewStaff.setOnClickListener(this);
        txt_display_HomeViewAllCategory.setOnClickListener(this);
        txt_display_HomeViewAllStatistic.setOnClickListener(this);

        // Đăng ký quan sát dữ liệu danh mục món ăn
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), list -> {
            loaiMonDTOList.clear();
            loaiMonDTOList.addAll(list);
            adapterRecycleViewCategory.notifyDataSetChanged();
            checkAndRestoreScroll();
        });

        // Đăng ký quan sát dữ liệu hóa đơn hôm nay
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
    
    // Tự động làm mới danh sách hóa đơn trong ngày khi nhận tín hiệu Socket.io
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

    /**
     * Khôi phục vị trí cuộn ScrollView sau khi nạp dữ liệu xong.
     */
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

    /**
     * Yêu cầu ViewModel cập nhật thông tin loại món ăn.
     */
    private void HienThiDSLoai() {
        categoryViewModel.syncCategoriesFromServer(null);
    }

    private void HienThiDonTrongNgay() {
        HienThiDonTrongNgay(false);
    }

    /**
     * Yêu cầu ViewModel cập nhật thông tin hóa đơn trong ngày.
     */
    private void HienThiDonTrongNgay(boolean forceRefresh) {
        homeViewModel.fetchTodayOrders(forceRefresh);
    }

    /**
     * Xử lý sự kiện click trên các nút điều hướng nhanh ở trang chủ.
     * Liên kết chuyển tab Bottom Navigation thông qua HomeActivity.
     */
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
