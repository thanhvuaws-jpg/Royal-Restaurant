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

import androidx.lifecycle.ViewModelProvider;
import com.sinhvien.orderdrinkapp.ViewModel.CustomerProfileViewModel;

public class CustomerProfileFragment extends Fragment {

    private CustomerProfileViewModel customerProfileViewModel;

    TextView txt_profile_name, txt_profile_phone, txt_profile_spending, txt_profile_badge;
    RecyclerView rv_history_bookings;
    BookingHistoryAdapter adapter;
    List<BookingResponse> bookingList = new ArrayList<>();
    private androidx.core.widget.NestedScrollView nsv_customer_profile;
    private int savedScrollY = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_profile, container, false);

        txt_profile_name = view.findViewById(R.id.txt_profile_name);
        txt_profile_phone = view.findViewById(R.id.txt_profile_phone);
        txt_profile_spending = view.findViewById(R.id.txt_profile_spending);
        txt_profile_badge = view.findViewById(R.id.txt_profile_badge);
        rv_history_bookings = view.findViewById(R.id.rv_history_bookings);
        nsv_customer_profile = view.findViewById(R.id.nsv_customer_profile);

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
            txt_profile_phone.setText(savedInstanceState.getString("saved_phone", ""));
            txt_profile_spending.setText(savedInstanceState.getString("saved_spending", ""));
            
            String badgeText = savedInstanceState.getString("saved_badge", "");
            txt_profile_badge.setText(badgeText);
            if (!badgeText.isEmpty()) {
                String badgeColor = "#CD7F32";
                if (badgeText.contains("Vàng")) badgeColor = "#FFD700";
                else if (badgeText.contains("Bạc")) badgeColor = "#C0C0C0";
                GradientDrawable drawable = (GradientDrawable) txt_profile_badge.getBackground();
                if (drawable != null) {
                    drawable.setColor(Color.parseColor(badgeColor));
                }
            }
            savedScrollY = savedInstanceState.getInt("saved_scroll_y", -1);
        }

        rv_history_bookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingHistoryAdapter(getContext(), bookingList);
        rv_history_bookings.setAdapter(adapter);

        txt_profile_name.setText(SessionManager.getFullName(getContext()));

        if (savedScrollY != -1 && nsv_customer_profile != null) {
            final int y = savedScrollY;
            nsv_customer_profile.post(new Runnable() {
                @Override
                public void run() {
                    nsv_customer_profile.scrollTo(0, y);
                }
            });
        }

        customerProfileViewModel = new ViewModelProvider(this).get(CustomerProfileViewModel.class);
        customerProfileViewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) return;
            txt_profile_phone.setText("SĐT: " + (profile.getSdt() != null ? profile.getSdt() : ""));

            String totalSpentStr = profile.getSpending();
            long totalSpent = 0;
            try {
                if (totalSpentStr != null && !totalSpentStr.isEmpty()) {
                    totalSpent = Long.parseLong(totalSpentStr);
                }
            } catch (NumberFormatException ignored) {}

            DecimalFormat formatter = new DecimalFormat("#,###");
            txt_profile_spending.setText(formatter.format(totalSpent) + " đ");

            updateLoyaltyBadge(totalSpent);

            bookingList.clear();
            if (profile.getBookings() != null) {
                bookingList.addAll(profile.getBookings());
            }
            adapter.notifyDataSetChanged();
            if (savedScrollY != -1 && nsv_customer_profile != null) {
                final int y = savedScrollY;
                nsv_customer_profile.post(new Runnable() {
                    @Override
                    public void run() {
                        nsv_customer_profile.scrollTo(0, y);
                    }
                });
                savedScrollY = -1;
            }
        });

        loadCustomerSpendingAndHistory();

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bookingList != null && !bookingList.isEmpty()) {
            String json = new com.google.gson.Gson().toJson(bookingList);
            outState.putString("saved_bookings", json);
        }
        if (txt_profile_phone != null) outState.putString("saved_phone", txt_profile_phone.getText().toString());
        if (txt_profile_spending != null) outState.putString("saved_spending", txt_profile_spending.getText().toString());
        if (txt_profile_badge != null) outState.putString("saved_badge", txt_profile_badge.getText().toString());
        if (nsv_customer_profile != null) {
            outState.putInt("saved_scroll_y", nsv_customer_profile.getScrollY());
        }
    }

    private void loadCustomerSpendingAndHistory() {
        int makh = SessionManager.getMaNV(getContext());
        customerProfileViewModel.fetchCustomerProfile(makh, false);
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
