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

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.Activities.CashierConfirmActivity;
import com.sinhvien.orderdrinkapp.Activities.DetailStatisticActivity;
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

import com.google.android.material.tabs.TabLayout;

public class DisplayCashierFragment extends Fragment {

    private static final String TAG = "DisplayCashierFragment";

    RecyclerView rv_cashier_OrderList;
    List<DonDatDTO> donDatDTOList;
    List<DonDatDTO> paidOrdersList;
    AdapterDisplayStatistic adapter;
    View view;
    View layout_empty_state;
    TextView txt_empty_StateTitle, txt_empty_StateDesc;

    TextView txt_cashier_TodayRevenue, txt_cashier_TodayCash, txt_cashier_TodayTransfer;
    TabLayout tab_cashier_Toggle;
    private int currentTab = 0;

    private boolean isFirstLoad = true;
    private androidx.appcompat.app.AlertDialog loadingDialog;

    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private static final int POLLING_INTERVAL = 5000; // 5 seconds
    private boolean isPolling = false;

    private io.socket.client.Socket mSocket;
    private io.socket.emitter.Emitter.Listener onRefreshOrders = new io.socket.emitter.Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadPendingOrders();
                    }
                });
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.displaycashier_layout, container, false);

        if (getActivity() != null && ((HomeActivity) getActivity()).getSupportActionBar() != null) {
            ((HomeActivity) getActivity()).getSupportActionBar().setTitle("Bảng Điều Khiển Thu Ngân");
        }

        rv_cashier_OrderList = view.findViewById(R.id.rv_cashier_OrderList);
        rv_cashier_OrderList.setLayoutManager(new LinearLayoutManager(getActivity()));
        layout_empty_state = view.findViewById(R.id.layout_empty_state);
        txt_empty_StateTitle = view.findViewById(R.id.txt_empty_StateTitle);
        txt_empty_StateDesc = view.findViewById(R.id.txt_empty_StateDesc);

        txt_cashier_TodayRevenue = view.findViewById(R.id.txt_cashier_TodayRevenue);
        txt_cashier_TodayCash = view.findViewById(R.id.txt_cashier_TodayCash);
        txt_cashier_TodayTransfer = view.findViewById(R.id.txt_cashier_TodayTransfer);
        tab_cashier_Toggle = view.findViewById(R.id.tab_cashier_Toggle);

        tab_cashier_Toggle.addTab(tab_cashier_Toggle.newTab().setText("Chờ thanh toán (0)"));
        tab_cashier_Toggle.addTab(tab_cashier_Toggle.newTab().setText("Lịch sử hôm nay (0)"));

        donDatDTOList = new ArrayList<>();
        paidOrdersList = new ArrayList<>();

        tab_cashier_Toggle.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateRecyclerView();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        updateRecyclerView();

        return view;
    }

    private void updateRecyclerView() {
        if (getActivity() == null) return;
        
        List<DonDatDTO> activeList = (currentTab == 0) ? donDatDTOList : paidOrdersList;
        if (adapter == null) {
            adapter = new AdapterDisplayStatistic(getActivity(), activeList);
            rv_cashier_OrderList.setAdapter(adapter);
            adapter.setOnItemClickListener(position -> {
                List<DonDatDTO> currentList = (currentTab == 0) ? donDatDTOList : paidOrdersList;
                if (position < 0 || position >= currentList.size()) return;
                DonDatDTO don = currentList.get(position);
                if (currentTab == 0) {
                    Intent intent = new Intent(getActivity(), CashierConfirmActivity.class);
                    intent.putExtra("madon", don.getMaDonDat());
                    intent.putExtra("manv", don.getMaNV());
                    intent.putExtra("maban", don.getMaBan());
                    intent.putExtra("ngaydat", don.getNgayDat());
                    intent.putExtra("tongtien", don.getTongTien());
                    intent.putExtra("tennv", don.getTenNV());
                    intent.putExtra("tenban", don.getTenBan());
                    intent.putExtra("phuongthuc", don.getPhuongThucTT());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getActivity(), DetailStatisticActivity.class);
                    intent.putExtra("madon", don.getMaDonDat());
                    intent.putExtra("manv", don.getMaNV());
                    intent.putExtra("maban", don.getMaBan());
                    intent.putExtra("ngaydat", don.getNgayDat());
                    intent.putExtra("tongtien", don.getTongTien());
                    intent.putExtra("tennv", don.getTenNV());
                    intent.putExtra("tenban", don.getTenBan());
                    startActivity(intent);
                }
            });
        } else {
            adapter.updateData(activeList);
        }

        if (activeList.isEmpty()) {
            rv_cashier_OrderList.setVisibility(View.GONE);
            layout_empty_state.setVisibility(View.VISIBLE);
            if (currentTab == 0) {
                txt_empty_StateTitle.setText("Không có đơn nào");
                txt_empty_StateDesc.setText("Hiện tại không có đơn hàng nào đang chờ thanh toán.");
            } else {
                txt_empty_StateTitle.setText("Trống");
                txt_empty_StateDesc.setText("Chưa có hóa đơn nào được thanh toán hôm nay.");
            }
        } else {
            rv_cashier_OrderList.setVisibility(View.VISIBLE);
            layout_empty_state.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Chỉ hiện loading dialog lần đầu tiên, khi list đang trống
        if (isFirstLoad && donDatDTOList.isEmpty()) {
            loadingDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(getActivity(), "Đang tải dữ liệu...");
            if (loadingDialog != null) loadingDialog.show();
        }
        loadPendingOrders();
        
        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (mSocket != null) {
            mSocket.on("refresh_orders", onRefreshOrders);
            // Backup polling chạy mỗi 30s để dự phòng mất mạng đột ngột
            startPolling(30000);
        } else {
            // Không có socket -> polling 5s như cũ
            startPolling(5000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
        if (mSocket != null) {
            mSocket.off("refresh_orders", onRefreshOrders);
        }
    }

    private void startPolling(final int interval) {
        if (isPolling) {
            stopPolling();
        }
        isPolling = true;
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                loadPendingOrders();
                pollingHandler.postDelayed(this, interval);
            }
        };
        pollingHandler.postDelayed(pollingRunnable, interval);
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
        
        // 1. Load pending orders
        apiService.getPendingOrders().enqueue(new Callback<List<OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderResponse>> call, Response<List<OrderResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                // Ẩn loading dialog khi lần đầu có dữ liệu về
                if (isFirstLoad) {
                    isFirstLoad = false;
                    if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
                }
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Tải đơn hàng chờ thanh toán thành công: count=" + response.body().size());
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
                        dto.setPhuongThucTT(res.getPhuongThuc());
                        try {
                            Date d = cloudFormat.parse(res.getNgayDat());
                            dto.setNgayDat(appFormat.format(d));
                        } catch (Exception e) {
                            dto.setNgayDat(res.getNgayDat());
                        }
                        donDatDTOList.add(dto);
                    }
                    
                    if (tab_cashier_Toggle.getTabAt(0) != null) {
                        tab_cashier_Toggle.getTabAt(0).setText("Chờ thanh toán (" + donDatDTOList.size() + ")");
                    }
                    if (currentTab == 0) {
                        updateRecyclerView();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<OrderResponse>> call, Throwable t) {
                Log.e(TAG, "Lỗi kết nối API tải đơn hàng chờ thanh toán: " + t.getMessage());
                if (isFirstLoad) {
                    isFirstLoad = false;
                    if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
                }
            }
        });

        // 2. Load paid orders for today summary and history tab
        SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateOnlyFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT+7"));
        String queryDate = dateOnlyFormat.format(new Date());

        apiService.getPaidOrders(queryDate).enqueue(new Callback<List<OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderResponse>> call, Response<List<OrderResponse>> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Tải lịch sử hóa đơn hôm nay thành công: count=" + response.body().size());
                    paidOrdersList.clear();
                    
                    long totalRevenue = 0;
                    long totalCash = 0;
                    long totalTransfer = 0;
                    
                    SimpleDateFormat cloudFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat appFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
                    
                    for (OrderResponse res : response.body()) {
                        DonDatDTO dto = new DonDatDTO();
                        dto.setMaDonDat(res.getMaDonDat());
                        dto.setMaNV(res.getMaNV());
                        dto.setMaBan(res.getMaBan());
                        dto.setTongTien(String.valueOf(res.getTongTien()));
                        dto.setTinhTrang("true");
                        dto.setTenNV(res.getHoTenNV());
                        dto.setTenBan(res.getTenBan());
                        dto.setPhuongThucTT(res.getPhuongThuc());
                        try {
                            Date d = cloudFormat.parse(res.getNgayDat());
                            dto.setNgayDat(appFormat.format(d));
                        } catch (Exception e) {
                            dto.setNgayDat(res.getNgayDat());
                        }

                        paidOrdersList.add(dto);
                        
                        long orderAmount = 0;
                        try {
                            orderAmount = Long.parseLong(res.getTongTien());
                        } catch (Exception ignored) {}
                        
                        totalRevenue += orderAmount;
                        if ("Chuyển khoản".equals(res.getPhuongThuc())) {
                            totalTransfer += orderAmount;
                        } else {
                            totalCash += orderAmount;
                        }
                    }
                    
                    txt_cashier_TodayRevenue.setText(String.format("%,d", totalRevenue) + " VNĐ");
                    txt_cashier_TodayCash.setText(String.format("%,d", totalCash) + " VNĐ");
                    txt_cashier_TodayTransfer.setText(String.format("%,d", totalTransfer) + " VNĐ");

                    if (tab_cashier_Toggle.getTabAt(1) != null) {
                        tab_cashier_Toggle.getTabAt(1).setText("Lịch sử hôm nay (" + paidOrdersList.size() + ")");
                    }
                    if (currentTab == 1) {
                        updateRecyclerView();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<OrderResponse>> call, Throwable t) {
                Log.e(TAG, "Lỗi kết nối API tải lịch sử hóa đơn: " + t.getMessage());
            }
        });
    }
}
