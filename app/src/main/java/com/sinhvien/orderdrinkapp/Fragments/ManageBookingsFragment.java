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

public class ManageBookingsFragment extends Fragment {

    private static final String TAG = "ManageBookingsFragment";
    private BookingViewModel bookingViewModel;

    TabLayout tabLayout_bookings;
    RecyclerView rv_manage_bookings;

    List<BookingResponse> allBookings = new ArrayList<>();
    List<BookingResponse> filteredBookings = new ArrayList<>();
    ManageBookingsAdapter adapter;
    private androidx.appcompat.app.AlertDialog loadingDialog;
    private android.os.Parcelable savedLayoutState;

    private io.socket.client.Socket mSocket;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_bookings, container, false);
        if (getActivity() != null && ((HomeActivity) getActivity()).getSupportActionBar() != null) {
            ((HomeActivity) getActivity()).getSupportActionBar().setTitle("Quản lý đặt bàn");
        }

        tabLayout_bookings = view.findViewById(R.id.tabLayout_bookings);
        rv_manage_bookings = view.findViewById(R.id.rv_manage_bookings);

        tabLayout_bookings.addTab(tabLayout_bookings.newTab().setText("Chờ duyệt"));
        tabLayout_bookings.addTab(tabLayout_bookings.newTab().setText("Đã nhận"));
        tabLayout_bookings.addTab(tabLayout_bookings.newTab().setText("Quá giờ"));
        tabLayout_bookings.addTab(tabLayout_bookings.newTab().setText("Đã hủy"));
        tabLayout_bookings.addTab(tabLayout_bookings.newTab().setText("Tất cả"));

        int savedTabPosition = 0;
        if (savedInstanceState != null) {
            savedTabPosition = savedInstanceState.getInt("selected_tab_position", 0);
            savedLayoutState = savedInstanceState.getParcelable("list_state");
        }

        TabLayout.Tab targetTab = tabLayout_bookings.getTabAt(savedTabPosition);
        if (targetTab != null) {
            targetTab.select();
        }

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
        adapter = new ManageBookingsAdapter(getContext(), filteredBookings, () -> loadAllBookings(true));
        rv_manage_bookings.setAdapter(adapter);

        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);
        bookingViewModel.getBookingsAll().observe(getViewLifecycleOwner(), list -> {
            allBookings.clear();
            allBookings.addAll(list);
            filterBookings(tabLayout_bookings.getSelectedTabPosition());
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
        // ViewModel handles lifecycle now
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

    private final io.socket.emitter.Emitter.Listener onBookingStatusUpdated = args -> {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                loadAllBookings(true);
                Toast.makeText(getContext(), "Cập nhật danh sách đặt bàn theo thời gian thực!", Toast.LENGTH_SHORT).show();
            });
        }
    };

    private void loadAllBookings(boolean forceRefresh) {
        bookingViewModel.fetchBookingsAll(forceRefresh);
    }

    private void filterBookings(int tabPosition) {
        filteredBookings.clear();
        for (BookingResponse b : allBookings) {
            String status = b.getTinhtrang();
            if (tabPosition == 0) { // Chờ duyệt
                if ("pending".equalsIgnoreCase(status)) filteredBookings.add(b);
            } else if (tabPosition == 1) { // Đã nhận
                if ("checked_in".equalsIgnoreCase(status)
                    || "confirmed".equalsIgnoreCase(status)) {
                    filteredBookings.add(b);
                }
            } else if (tabPosition == 2) { // Quá giờ
                if ("overdue".equalsIgnoreCase(status)) filteredBookings.add(b);
            } else if (tabPosition == 3) { // Đã hủy
                if ("cancelled".equalsIgnoreCase(status)) filteredBookings.add(b);
            } else { // Tất cả
                filteredBookings.add(b);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
