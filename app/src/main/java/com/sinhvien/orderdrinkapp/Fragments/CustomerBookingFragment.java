package com.sinhvien.orderdrinkapp.Fragments;

import android.content.Intent;
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

import com.google.android.material.button.MaterialButton;
import com.sinhvien.orderdrinkapp.Activities.CustomerBookingActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.BookingHistoryAdapter;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.lifecycle.ViewModelProvider;
import com.sinhvien.orderdrinkapp.ViewModel.BookingViewModel;

public class CustomerBookingFragment extends Fragment {

    private BookingViewModel bookingViewModel;
    MaterialButton btn_new_booking;
    RecyclerView rv_active_bookings;
    BookingHistoryAdapter adapter;
    List<BookingResponse> bookingList = new ArrayList<>();
    private androidx.core.widget.NestedScrollView nsv_customer_booking;
    private int savedScrollY = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_booking, container, false);

        btn_new_booking = view.findViewById(R.id.btn_new_booking);
        rv_active_bookings = view.findViewById(R.id.rv_active_bookings);
        nsv_customer_booking = view.findViewById(R.id.nsv_customer_booking);

        if (savedInstanceState != null) {
            String json = savedInstanceState.getString("saved_bookings");
            if (json != null && !json.isEmpty()) {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<BookingResponse>>(){}.getType();
                List<BookingResponse> restored = new com.google.gson.Gson().fromJson(json, type);
                if (restored != null) {
                    bookingList.clear();
                    bookingList.addAll(restored);
                }
            }
            savedScrollY = savedInstanceState.getInt("saved_scroll_y", -1);
        }

        rv_active_bookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingHistoryAdapter(getContext(), bookingList);
        rv_active_bookings.setAdapter(adapter);

        if (savedScrollY != -1 && nsv_customer_booking != null) {
            final int y = savedScrollY;
            nsv_customer_booking.post(new Runnable() {
                @Override
                public void run() {
                    nsv_customer_booking.scrollTo(0, y);
                }
            });
        }

        btn_new_booking.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CustomerBookingActivity.class);
            startActivity(intent);
        });

        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);
        bookingViewModel.getCustomerBookings().observe(getViewLifecycleOwner(), list -> {
            bookingList.clear();
            bookingList.addAll(list);
            adapter.notifyDataSetChanged();
            if (savedScrollY != -1 && nsv_customer_booking != null) {
                final int y = savedScrollY;
                nsv_customer_booking.post(new Runnable() {
                    @Override
                    public void run() {
                        nsv_customer_booking.scrollTo(0, y);
                    }
                });
                savedScrollY = -1;
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bookingList != null && !bookingList.isEmpty()) {
            String json = new com.google.gson.Gson().toJson(bookingList);
            outState.putString("saved_bookings", json);
        }
        if (nsv_customer_booking != null) {
            outState.putInt("saved_scroll_y", nsv_customer_booking.getScrollY());
        }
    }

    private io.socket.client.Socket mSocket;

    @Override
    public void onResume() {
        super.onResume();
        loadBookings();
        setupSocketListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSocket != null) {
            mSocket.off("booking_status_updated");
        }
    }

    private void setupSocketListener() {
        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (mSocket != null) {
            mSocket.on("booking_status_updated", args -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadBookings();
                        Toast.makeText(getContext(), "Lịch đặt bàn của bạn đã được cập nhật trạng thái!", Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private void loadBookings() {
        int makh = SessionManager.getMaNV(getContext());
        bookingViewModel.fetchBookingsForCustomer(makh, false);
    }
}
