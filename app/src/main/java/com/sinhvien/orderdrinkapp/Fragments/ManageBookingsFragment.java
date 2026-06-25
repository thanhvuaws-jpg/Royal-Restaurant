package com.sinhvien.orderdrinkapp.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.ManageBookingsAdapter;
import com.sinhvien.orderdrinkapp.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;
import androidx.lifecycle.ViewModelProvider;
import com.sinhvien.orderdrinkapp.ViewModel.BookingViewModel;

/**
 * ManageBookingsFragment - Màn hình Quản lý Danh sách Đặt bàn từ phía nhà hàng (Restaurant Booking Management).
 * - Sử dụng TabLayout phân tách danh sách đặt bàn thành 5 trạng thái:
 *   1. Chờ duyệt (pending)
 *   2. Đã nhận (confirmed/checked_in)
 *   3. Quá giờ (overdue)
 *   4. Đã hủy (cancelled)
 *   5. Tất cả
 * - Hỗ trợ khôi phục vị trí cuộn danh sách (savedLayoutState) và khôi phục tab đang chọn khi xoay màn hình hoặc cấu hình hệ thống thay đổi.
 * - Lắng nghe thay đổi đặt bàn từ phía khách hàng theo thời gian thực thông qua Socket.io sự kiện ("booking_status_updated").
 * - Sử dụng BookingViewModel để tương tác và đồng bộ dữ liệu.
 */
public class ManageBookingsFragment extends Fragment {

    private static final String TAG = "ManageBookingsFragment";
    
    // ViewModel quản lý đặt bàn của quán ăn
    private BookingViewModel bookingViewModel;

    //TabLayout phân loại các hóa đơn đặt bàn và RecyclerView hiển thị danh sách
    TabLayout tabLayout_bookings;
    RecyclerView rv_manage_bookings;

    // Danh sách tổng hợp toàn bộ các lượt đặt bàn
    List<BookingResponse> allBookings = new ArrayList<>();
    // Danh sách sau khi lọc theo tab hiện hành
    List<BookingResponse> filteredBookings = new ArrayList<>();
    ManageBookingsAdapter adapter;
    private androidx.appcompat.app.AlertDialog loadingDialog;
    
    // Lưu trữ trạng thái cuộn của LayoutManager
    private android.os.Parcelable savedLayoutState;

    private io.socket.client.Socket mSocket;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_bookings, container, false);
        
        // Thiết lập tiêu đề trên ActionBar
        if (getActivity() != null && ((HomeActivity) getActivity()).getSupportActionBar() != null) {
            ((HomeActivity) getActivity()).getSupportActionBar().setTitle("Quản lý đặt bàn");
        }

        tabLayout_bookings = view.findViewById(R.id.tabLayout_bookings);
        rv_manage_bookings = view.findViewById(R.id.rv_manage_bookings);

        // Khởi tạo các Tab tương ứng với trạng thái đặt bàn
        tabLayout_bookings.addTab(tabLayout_bookings.newTab().setText("Chờ duyệt"));
        tabLayout_bookings.addTab(tabLayout_bookings.newTab().setText("Đã nhận"));
        tabLayout_bookings.addTab(tabLayout_bookings.newTab().setText("Quá giờ"));
        tabLayout_bookings.addTab(tabLayout_bookings.newTab().setText("Đã hủy"));
        tabLayout_bookings.addTab(tabLayout_bookings.newTab().setText("Tất cả"));

        int savedTabPosition = 0;
        // Khôi phục lại trạng thái cũ nếu có (ví dụ khi xoay điện thoại)
        if (savedInstanceState != null) {
            savedTabPosition = savedInstanceState.getInt("selected_tab_position", 0);
            savedLayoutState = savedInstanceState.getParcelable("list_state");
        }

        TabLayout.Tab targetTab = tabLayout_bookings.getTabAt(savedTabPosition);
        if (targetTab != null) {
            targetTab.select();
        }

        // Đăng ký sự kiện click tab để lọc danh sách
        tabLayout_bookings.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterBookings(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        rv_manage_bookings.setLayoutManager(new LinearLayoutManager(getContext()));
        // Khởi tạo Adapter với callback hỗ trợ reload lại toàn bộ danh sách khi có thay đổi trạng thái
        adapter = new ManageBookingsAdapter(getContext(), filteredBookings, () -> loadAllBookings(true));
        rv_manage_bookings.setAdapter(adapter);

        // Khởi tạo BookingViewModel và theo dõi dữ liệu đặt bàn
        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);
        bookingViewModel.getBookingsAll().observe(getViewLifecycleOwner(), list -> {
            allBookings.clear();
            allBookings.addAll(list);
            filterBookings(tabLayout_bookings.getSelectedTabPosition());
            
            // Khôi phục lại trạng thái cuộn của RecyclerView sau khi dữ liệu tải xong
            if (savedLayoutState != null && rv_manage_bookings.getLayoutManager() != null) {
                rv_manage_bookings.getLayoutManager().onRestoreInstanceState(savedLayoutState);
                savedLayoutState = null;
            }
        });
        bookingViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                if (loadingDialog == null && getActivity() != null) {
                    loadingDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(getActivity(), "Đang tải dữ liệu...");
                }
                if (loadingDialog != null && !loadingDialog.isShowing()) {
                    loadingDialog.show();
                }
            } else {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            }
        });

        return view;
    }

    public static void clearCache() {
        // ViewModel quản lý vòng đời dữ liệu, không cần lưu cache tĩnh
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (tabLayout_bookings != null) {
            outState.putInt("selected_tab_position", tabLayout_bookings.getSelectedTabPosition());
        }
        if (rv_manage_bookings != null && rv_manage_bookings.getLayoutManager() != null) {
            outState.putParcelable("list_state", rv_manage_bookings.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllBookings(false);

        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (mSocket != null) {
            mSocket.on("booking_status_updated", onBookingStatusUpdated);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSocket != null) {
            mSocket.off("booking_status_updated", onBookingStatusUpdated);
        }
    }

    // Sự kiện lắng nghe từ Socket.io khi khách hàng gửi yêu cầu đặt bàn hoặc cập nhật trạng thái
    private final io.socket.emitter.Emitter.Listener onBookingStatusUpdated = args -> {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                loadAllBookings(true);
                Toast.makeText(getContext(), "Cập nhật danh sách đặt bàn theo thời gian thực!", Toast.LENGTH_SHORT).show();
            });
        }
    };

    /**
     * Yêu cầu ViewModel tải danh sách đặt bàn.
     */
    private void loadAllBookings(boolean forceRefresh) {
        bookingViewModel.fetchBookingsAll(forceRefresh);
    }

    /**
     * Lọc danh sách lượt đặt bàn theo Tab được lựa chọn.
     * - Tab 0: Chờ duyệt ("pending")
     * - Tab 1: Đã xác nhận / Đã nhận bàn ("confirmed" hoặc "checked_in")
     * - Tab 2: Quá giờ ("overdue")
     * - Tab 3: Đã hủy ("cancelled")
     * - Tab 4: Tất cả danh sách
     */
    private void filterBookings(int tabPosition) {
        filteredBookings.clear();
        for (BookingResponse b : allBookings) {
            String status = b.getTinhtrang();
            if (tabPosition == 0) { 
                if ("pending".equalsIgnoreCase(status)) filteredBookings.add(b);
            } else if (tabPosition == 1) { 
                if ("checked_in".equalsIgnoreCase(status)
                    || "confirmed".equalsIgnoreCase(status)) {
                    filteredBookings.add(b);
                }
            } else if (tabPosition == 2) { 
                if ("overdue".equalsIgnoreCase(status)) filteredBookings.add(b);
            } else if (tabPosition == 3) { 
                if ("cancelled".equalsIgnoreCase(status)) filteredBookings.add(b);
            } else { 
                filteredBookings.add(b);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
