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

public class ManageBookingsFragment extends Fragment {

    TabLayout tabLayout_bookings;
    RecyclerView rv_manage_bookings;

    List<BookingResponse> allBookings = new ArrayList<>();
    List<BookingResponse> filteredBookings = new ArrayList<>();
    ManageBookingsAdapter adapter;

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

        return view;
    }

    private static List<BookingResponse> cachedBookings = java.util.Collections.synchronizedList(new ArrayList<>());
    private static long lastLoadTime = 0;

    public static void clearCache() {
        cachedBookings.clear();
        lastLoadTime = 0;
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
        long currentTime = System.currentTimeMillis();
        if (!forceRefresh && !cachedBookings.isEmpty() && (currentTime - lastLoadTime < 30000)) {
            allBookings.clear();
            allBookings.addAll(cachedBookings);
            filterBookings(tabLayout_bookings.getSelectedTabPosition());
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getBookings(0).enqueue(new Callback<List<BookingResponse>>() {
            @Override
            public void onResponse(Call<List<BookingResponse>> call, Response<List<BookingResponse>> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    cachedBookings.clear();
                    cachedBookings.addAll(response.body());
                    lastLoadTime = System.currentTimeMillis();

                    allBookings.clear();
                    allBookings.addAll(cachedBookings);
                    filterBookings(tabLayout_bookings.getSelectedTabPosition());
                }
            }

            @Override
            public void onFailure(Call<List<BookingResponse>> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi tải lịch đặt bàn: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void filterBookings(int tabPosition) {
        filteredBookings.clear();
        for (BookingResponse b : allBookings) {
            String status = b.getTinhtrang();
            if (tabPosition == 0) { // Chờ duyệt
                if ("pending".equalsIgnoreCase(status)) filteredBookings.add(b);
            } else if (tabPosition == 1) { // Đã nhận
                if ("checked_in".equalsIgnoreCase(status)) filteredBookings.add(b);
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
