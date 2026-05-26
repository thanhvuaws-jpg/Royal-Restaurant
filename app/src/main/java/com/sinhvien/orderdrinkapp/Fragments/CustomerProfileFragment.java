package com.sinhvien.orderdrinkapp.Fragments;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.BookingHistoryAdapter;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerProfileFragment extends Fragment {

    TextView txt_profile_name, txt_profile_phone, txt_profile_spending, txt_profile_badge;
    RecyclerView rv_history_bookings;
    BookingHistoryAdapter adapter;
    List<BookingResponse> bookingList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_profile, container, false);

        txt_profile_name = view.findViewById(R.id.txt_profile_name);
        txt_profile_phone = view.findViewById(R.id.txt_profile_phone);
        txt_profile_spending = view.findViewById(R.id.txt_profile_spending);
        txt_profile_badge = view.findViewById(R.id.txt_profile_badge);
        rv_history_bookings = view.findViewById(R.id.rv_history_bookings);

        rv_history_bookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingHistoryAdapter(getContext(), bookingList);
        rv_history_bookings.setAdapter(adapter);

        txt_profile_name.setText(SessionManager.getFullName(getContext()));

        loadCustomerSpendingAndHistory();

        return view;
    }

    private void loadCustomerSpendingAndHistory() {
        int makh = SessionManager.getMaNV(getContext());
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        // Lấy thông tin tài khoản chi tiết từ getStaffById để hiển thị SĐT
        apiService.getStaffById(makh).enqueue(new Callback<com.sinhvien.orderdrinkapp.Api.StaffResponse>() {
            @Override
            public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.StaffResponse> call, Response<com.sinhvien.orderdrinkapp.Api.StaffResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    txt_profile_phone.setText("SĐT: " + response.body().getSdt());
                }
            }

            @Override
            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.StaffResponse> call, Throwable t) {}
        });

        // Tải chi tiêu
        apiService.getCustomerSpending(makh).enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String totalSpentStr = response.body().getTongTien();
                    long totalSpent = 0;
                    try {
                        if (totalSpentStr != null && !totalSpentStr.isEmpty()) {
                            totalSpent = Long.parseLong(totalSpentStr);
                        }
                    } catch (NumberFormatException ignored) {}

                    DecimalFormat formatter = new DecimalFormat("#,###");
                    txt_profile_spending.setText(formatter.format(totalSpent) + " đ");

                    // Cập nhật hạng thành viên
                    updateLoyaltyBadge(totalSpent);
                }
            }

            @Override
            public void onFailure(Call<BookingResponse> call, Throwable t) {}
        });

        // Tải lịch sử
        apiService.getBookings(makh).enqueue(new Callback<List<BookingResponse>>() {
            @Override
            public void onResponse(Call<List<BookingResponse>> call, Response<List<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bookingList.clear();
                    bookingList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<BookingResponse>> call, Throwable t) {}
        });
    }

    private void updateLoyaltyBadge(long spending) {
        String badgeText;
        String badgeColor;
        
        if (spending >= 1000000) {
            badgeText = "Thành viên Vàng (Gold)";
            badgeColor = "#FFD700"; // Gold
        } else if (spending >= 500000) {
            badgeText = "Thành viên Bạc (Silver)";
            badgeColor = "#C0C0C0"; // Silver
        } else {
            badgeText = "Thành viên Đồng (Bronze)";
            badgeColor = "#CD7F32"; // Bronze
        }

        txt_profile_badge.setText(badgeText);
        GradientDrawable drawable = (GradientDrawable) txt_profile_badge.getBackground();
        if (drawable != null) {
            drawable.setColor(Color.parseColor(badgeColor));
        }
    }
}
