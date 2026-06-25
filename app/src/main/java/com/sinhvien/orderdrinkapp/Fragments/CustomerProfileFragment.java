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

/**
 * CustomerProfileFragment - Màn hình Hồ sơ Khách hàng.
 * Hiển thị thông tin cá nhân của khách hàng: họ tên, số điện thoại, tổng mức chi tiêu tích lũy,
 * thẻ hạng thành viên (Đồng/Bạc/Vàng) dựa trên mức chi tiêu, và lịch sử các lần đặt bàn trước đây.
 */
public class CustomerProfileFragment extends Fragment {

    // ViewModel quản lý thông tin hồ sơ của khách hàng
    private CustomerProfileViewModel customerProfileViewModel;

    // Các thành phần giao diện hiển thị thông tin
    TextView txt_profile_name, txt_profile_phone, txt_profile_spending, txt_profile_badge;
    // RecyclerView hiển thị danh sách lịch sử đặt bàn
    RecyclerView rv_history_bookings;
    // Adapter phục vụ hiển thị lịch sử đặt bàn
    BookingHistoryAdapter adapter;
    // Danh sách lịch đặt bàn
    List<BookingResponse> bookingList = new ArrayList<>();
    // ScrollView điều khiển cuộn
    private androidx.core.widget.NestedScrollView nsv_customer_profile;
    // Lưu trữ vị trí cuộn khi Fragment tái tạo (VD: xoay màn hình)
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

        // Khôi phục trạng thái giao diện đã lưu
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
                String badgeColor = "#CD7F32"; // Đồng
                if (badgeText.contains("Vàng")) badgeColor = "#FFD700"; // Vàng
                else if (badgeText.contains("Bạc")) badgeColor = "#C0C0C0"; // Bạc
                GradientDrawable drawable = (GradientDrawable) txt_profile_badge.getBackground();
                if (drawable != null) {
                    drawable.setColor(Color.parseColor(badgeColor));
                }
            }
            savedScrollY = savedInstanceState.getInt("saved_scroll_y", -1);
        }

        // Thiết lập RecyclerView hiển thị lịch sử đặt bàn
        rv_history_bookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingHistoryAdapter(getContext(), bookingList);
        rv_history_bookings.setAdapter(adapter);

        // Hiển thị họ tên lấy trực tiếp từ Session đã lưu
        txt_profile_name.setText(SessionManager.getFullName(getContext()));

        // Khôi phục vị trí cuộn trang
        if (savedScrollY != -1 && nsv_customer_profile != null) {
            final int y = savedScrollY;
            nsv_customer_profile.post(new Runnable() {
                @Override
                public void run() {
                    nsv_customer_profile.scrollTo(0, y);
                }
            });
        }

        // Đăng ký quan sát dữ liệu LiveData từ CustomerProfileViewModel
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

            // Định dạng hiển thị tiền tệ VNĐ (ví dụ: 1,500,000 đ)
            DecimalFormat formatter = new DecimalFormat("#,###");
            txt_profile_spending.setText(formatter.format(totalSpent) + " đ");

            // Cập nhật nhãn phân hạng thành viên
            updateLoyaltyBadge(totalSpent);

            // Cập nhật danh sách lịch sử đặt bàn
            bookingList.clear();
            if (profile.getBookings() != null) {
                bookingList.addAll(profile.getBookings());
            }
            adapter.notifyDataSetChanged();
            
            // Khôi phục vị trí cuộn sau khi cập nhật dữ liệu
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

        // Tải dữ liệu từ server
        loadCustomerSpendingAndHistory();

        return view;
    }

    /**
     * Lưu trạng thái giao diện Fragment trước khi bị tạm dừng/hủy.
     */
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

    /**
     * Thực hiện yêu cầu ViewModel tải thông tin hồ sơ của khách hàng từ server.
     */
    private void loadCustomerSpendingAndHistory() {
        int makh = SessionManager.getMaNV(getContext());
        customerProfileViewModel.fetchCustomerProfile(makh, false);
    }

    /**
     * Phân cấp và cập nhật giao diện hạng thành viên dựa trên tổng mức chi tiêu.
     * - Hạng Vàng: Chi tiêu >= 1.000.000 VNĐ
     * - Hạng Bạc: Chi tiêu >= 500.000 VNĐ
     * - Hạng Đồng: Chi tiêu < 500.000 VNĐ
     */
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
