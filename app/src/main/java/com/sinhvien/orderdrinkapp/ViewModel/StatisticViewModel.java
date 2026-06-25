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

/**
 * StatisticViewModel - Lớp ViewModel phục vụ cho màn hình Thống kê (Statistic).
 * Cung cấp chức năng lọc danh sách các hóa đơn đã thanh toán theo khoảng thời gian:
 * Hôm nay (0), 7 ngày gần nhất (7), 30 ngày gần nhất (30) hoặc Tất cả (-1).
 * Hỗ trợ chuyển đổi định dạng ngày tháng hiển thị và lưu trữ LiveData.
 */
public class StatisticViewModel extends AndroidViewModel {

    private static final String TAG = "StatisticViewModel";

    // LiveData chứa danh sách các đơn hàng đã thanh toán phù hợp bộ lọc
    private final MutableLiveData<List<DonDatDTO>> statisticOrdersLiveData = new MutableLiveData<>();
    // Trạng thái đang tải dữ liệu từ API
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    // LiveData thông báo lỗi nếu có
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    // Bộ lọc thời gian hiện tại (mặc định lọc 7 ngày gần nhất)
    private int currentFilter = 7;
    // Định dạng hiển thị ngày trên ứng dụng
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

    /**
     * Tải danh sách đơn hàng đã thanh toán từ API Server dựa theo bộ lọc thời gian.
     * @param forceRefresh Ép buộc tải lại dữ liệu mới từ server.
     */
    public void fetchPaidOrders(boolean forceRefresh) {
        if (!forceRefresh && statisticOrdersLiveData.getValue() != null && !statisticOrdersLiveData.getValue().isEmpty()) {
            return;
        }

        isLoadingLiveData.setValue(true);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<OrderResponse>> call;

        String fromDate = null;
        String toDate = null;
        
        // Tạo tham số thời gian gửi lên API theo bộ lọc hiện thời
        if (currentFilter != -1) { // Lọc theo thời gian cụ thể (không phải Tất cả)
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            toDate = apiFormat.format(cal.getTime());
            
            if (currentFilter == 0) { // Hôm nay
                fromDate = toDate;
            } else { // 7 ngày hoặc 30 ngày gần nhất
                cal.add(Calendar.DAY_OF_YEAR, -(currentFilter - 1));
                fromDate = apiFormat.format(cal.getTime());
            }
            call = apiService.getPaidOrders(fromDate, toDate);
        } else { // Tải toàn bộ lịch sử hóa đơn
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

                        // Chuyển định dạng từ "yyyy-MM-dd HH:mm:ss" sang "dd-MM-yyyy"
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
