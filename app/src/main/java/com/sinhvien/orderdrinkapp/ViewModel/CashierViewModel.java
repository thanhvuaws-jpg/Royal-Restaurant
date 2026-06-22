package com.sinhvien.orderdrinkapp.ViewModel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.DTO.DonDatDTO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CashierViewModel extends AndroidViewModel {

    private static final String TAG = "CashierViewModel";

    private final MutableLiveData<List<DonDatDTO>> pendingOrdersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<DonDatDTO>> paidOrdersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);

    private final MutableLiveData<Long> todayRevenueLiveData = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> todayCashLiveData = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> todayTransferLiveData = new MutableLiveData<>(0L);

    public CashierViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<DonDatDTO>> getPendingOrders() {
        return pendingOrdersLiveData;
    }

    public LiveData<List<DonDatDTO>> getPaidOrders() {
        return paidOrdersLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<Long> getTodayRevenue() {
        return todayRevenueLiveData;
    }

    public LiveData<Long> getTodayCash() {
        return todayCashLiveData;
    }

    public LiveData<Long> getTodayTransfer() {
        return todayTransferLiveData;
    }

    public void loadPendingOrders(boolean showLoading) {
        if (showLoading && (pendingOrdersLiveData.getValue() == null || pendingOrdersLiveData.getValue().isEmpty())) {
            isLoadingLiveData.setValue(true);
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getPendingOrders().enqueue(new Callback<List<OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderResponse>> call, Response<List<OrderResponse>> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<DonDatDTO> list = new ArrayList<>();
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
                        list.add(dto);
                    }
                    pendingOrdersLiveData.postValue(list);
                }
            }

            @Override
            public void onFailure(Call<List<OrderResponse>> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                Log.e(TAG, "Lỗi tải đơn hàng chờ thanh toán: " + t.getMessage());
            }
        });
    }

    public void loadPaidOrdersToday() {
        SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateOnlyFormat.setTimeZone(TimeZone.getTimeZone("GMT+7"));
        String queryDate = dateOnlyFormat.format(new Date());

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getPaidOrders(queryDate).enqueue(new Callback<List<OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderResponse>> call, Response<List<OrderResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<DonDatDTO> list = new ArrayList<>();
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

                        list.add(dto);

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

                    todayRevenueLiveData.postValue(totalRevenue);
                    todayCashLiveData.postValue(totalCash);
                    todayTransferLiveData.postValue(totalTransfer);
                    paidOrdersLiveData.postValue(list);
                }
            }

            @Override
            public void onFailure(Call<List<OrderResponse>> call, Throwable t) {
                Log.e(TAG, "Lỗi tải lịch sử hóa đơn: " + t.getMessage());
            }
        });
    }
}
