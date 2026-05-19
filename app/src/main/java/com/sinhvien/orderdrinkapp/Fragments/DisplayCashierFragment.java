package com.sinhvien.orderdrinkapp.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.sinhvien.orderdrinkapp.Activities.CashierConfirmActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayStatistic;
import com.sinhvien.orderdrinkapp.DTO.DonDatDTO;
import com.sinhvien.orderdrinkapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DisplayCashierFragment extends Fragment {

    RecyclerView rv_cashier_OrderList;
    List<DonDatDTO> donDatDTOList;
    AdapterDisplayStatistic adapter;
    View view;
    View layout_empty_state;
    TextView txt_empty_StateTitle, txt_empty_StateDesc;

    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private static final int POLLING_INTERVAL = 5000; // 5 seconds
    private boolean isPolling = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.displaycashier_layout, container, false);

        if (getActivity() != null && ((HomeActivity) getActivity()).getSupportActionBar() != null) {
            ((HomeActivity) getActivity()).getSupportActionBar().setTitle("Đơn Chờ Thanh Toán");
        }

        rv_cashier_OrderList = view.findViewById(R.id.rv_cashier_OrderList);
        rv_cashier_OrderList.setLayoutManager(new LinearLayoutManager(getActivity()));
        layout_empty_state = view.findViewById(R.id.layout_empty_state);
        txt_empty_StateTitle = view.findViewById(R.id.txt_empty_StateTitle);
        txt_empty_StateDesc = view.findViewById(R.id.txt_empty_StateDesc);

        donDatDTOList = new ArrayList<>();
        adapter = new AdapterDisplayStatistic(getActivity(), donDatDTOList);
        rv_cashier_OrderList.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {
            DonDatDTO don = donDatDTOList.get(position);
            Intent intent = new Intent(getActivity(), CashierConfirmActivity.class);
            intent.putExtra("madon", don.getMaDonDat());
            intent.putExtra("manv", don.getMaNV());
            intent.putExtra("maban", don.getMaBan());
            intent.putExtra("ngaydat", don.getNgayDat());
            intent.putExtra("tongtien", don.getTongTien());
            intent.putExtra("tennv", don.getTenNV());
            intent.putExtra("tenban", don.getTenBan());
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    private void startPolling() {
        if (isPolling) return;
        isPolling = true;
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                loadPendingOrders();
                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    private void stopPolling() {
        if (!isPolling) return;
        isPolling = false;
        if (pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    private void loadPendingOrders() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getPendingOrders().enqueue(new Callback<List<OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderResponse>> call, Response<List<OrderResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    donDatDTOList.clear();
                    SimpleDateFormat cloudFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat appFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
                    
                    for (OrderResponse res : response.body()) {
                        DonDatDTO dto = new DonDatDTO();
                        dto.setMaDonDat(res.getMaDonDat());
                        dto.setMaNV(res.getMaNV());
                        dto.setMaBan(res.getMaBan());
                        dto.setTongTien(String.valueOf(res.getTongTien()));
                        dto.setTinhTrang("pending");
                        dto.setTenNV(res.getHoTenNV());
                        dto.setTenBan(res.getTenBan());
                        try {
                            Date d = cloudFormat.parse(res.getNgayDat());
                            dto.setNgayDat(appFormat.format(d));
                        } catch (Exception e) {
                            dto.setNgayDat(res.getNgayDat());
                        }
                        donDatDTOList.add(dto);
                    }
                    adapter.notifyDataSetChanged();
                    
                    if (donDatDTOList.isEmpty()) {
                        rv_cashier_OrderList.setVisibility(View.GONE);
                        layout_empty_state.setVisibility(View.VISIBLE);
                        txt_empty_StateTitle.setText("Không có đơn nào");
                        txt_empty_StateDesc.setText("Hiện tại không có đơn hàng nào đang chờ thanh toán.");
                    } else {
                        rv_cashier_OrderList.setVisibility(View.VISIBLE);
                        layout_empty_state.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<OrderResponse>> call, Throwable t) {
                // Do nothing on failure to avoid annoying toasts during polling
            }
        });
    }
}
