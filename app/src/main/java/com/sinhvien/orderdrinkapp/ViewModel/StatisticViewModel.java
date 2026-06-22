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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticViewModel extends AndroidViewModel {

    private static final String TAG = "StatisticViewModel";

    private final MutableLiveData<List<DonDatDTO>> statisticOrdersLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private int currentFilter = 7;
    private final SimpleDateFormat DB_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    public StatisticViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<DonDatDTO>> getStatisticOrders() {
        return statisticOrdersLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public int getCurrentFilter() {
        return currentFilter;
    }

    public void setCurrentFilter(int filter) {
        this.currentFilter = filter;
    }

    public void fetchPaidOrders(boolean forceRefresh) {
        if (!forceRefresh && statisticOrdersLiveData.getValue() != null && !statisticOrdersLiveData.getValue().isEmpty()) {
            return;
        }

        isLoadingLiveData.setValue(true);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<OrderResponse>> call;

        String fromDate = null;
        String toDate = null;
        if (currentFilter != -1) { // not FILTER_ALL
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            toDate = apiFormat.format(cal.getTime());
            
            if (currentFilter == 0) { // FILTER_TODAY
                fromDate = toDate;
            } else {
                cal.add(Calendar.DAY_OF_YEAR, -(currentFilter - 1));
                fromDate = apiFormat.format(cal.getTime());
            }
            call = apiService.getPaidOrders(fromDate, toDate);
        } else {
            call = apiService.getPaidOrders();
        }

        call.enqueue(new Callback<List<OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderResponse>> call, Response<List<OrderResponse>> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<DonDatDTO> list = new ArrayList<>();
                    SimpleDateFormat cloudFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    for (OrderResponse res : response.body()) {
                        DonDatDTO dto = new DonDatDTO();
                        dto.setMaDonDat(res.getMaDonDat());
                        dto.setMaNV(res.getMaNV());
                        dto.setMaBan(res.getMaBan());
                        dto.setTongTien(String.valueOf(res.getTongTien()));
                        dto.setTinhTrang("true");
                        dto.setTenNV(res.getHoTenNV());
                        dto.setTenBan(res.getTenBan());

                        try {
                            Date d = cloudFormat.parse(res.getNgayDat());
                            dto.setNgayDat(DB_FORMAT.format(d));
                        } catch (Exception e) {
                            dto.setNgayDat(res.getNgayDat());
                        }
                        list.add(dto);
                    }
                    statisticOrdersLiveData.postValue(list);
                } else {
                    errorLiveData.postValue("Lỗi lấy dữ liệu từ máy chủ");
                }
            }

            @Override
            public void onFailure(Call<List<OrderResponse>> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                errorLiveData.postValue(t.getMessage());
            }
        });
    }
}
