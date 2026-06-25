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
 * HomeViewModel - Lớp ViewModel phục vụ màn hình chính (Home Fragment/Activity).
 * Quản lý danh sách đơn hàng đã hoàn tất thanh toán trong ngày hôm nay để hiển thị
 * biểu đồ hoặc danh sách đơn hàng gần đây trên bảng điều khiển (Dashboard).
 * Có cơ chế tự động giới hạn tần suất gọi API (cooldown 30 giây) tránh spam request.
 */
public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HomeViewModel";

    // Danh sách đơn hàng trong ngày hôm nay
    private final MutableLiveData<List<DonDatDTO>> todayOrdersLiveData = new MutableLiveData<>(new ArrayList<>());
    // Trạng thái đang tải dữ liệu từ server
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    // Thời điểm cuối cùng thực hiện tải dữ liệu (đơn vị: ms)
    private long lastLoadTime = 0;

    public HomeViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<DonDatDTO>> getTodayOrders() {
        return todayOrdersLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    /**
     * Tải danh sách đơn hàng đã thanh toán trong ngày hôm nay.
     * @param forceRefresh Ép buộc tải lại dữ liệu mà không quan tâm đến cooldown 30 giây.
     */
    public void fetchTodayOrders(boolean forceRefresh) {
        long currentTime = System.currentTimeMillis();
        // Áp dụng cơ chế cooldown 30 giây để tránh việc gửi yêu cầu API dồn dập
        if (!forceRefresh && (currentTime - lastLoadTime < 30000) && todayOrdersLiveData.getValue() != null && !todayOrdersLiveData.getValue().isEmpty()) {
            return;
        }

        isLoadingLiveData.setValue(true);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String ngaydat = dateFormat.format(calendar.getTime());

        SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String queryDate = apiDateFormat.format(calendar.getTime());

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getPaidOrders(queryDate).enqueue(new Callback<List<OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderResponse>> call, Response<List<OrderResponse>> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<DonDatDTO> list = new ArrayList<>();
                    SimpleDateFormat cloudFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    
                    for (OrderResponse res : response.body()) {
                        String orderDateStr = res.getNgayDat();
                        try {
                            Date d = cloudFormat.parse(res.getNgayDat());
                            orderDateStr = dateFormat.format(d);
                        } catch (Exception ignored) {}

                        // Lọc các đơn hàng đúng trong ngày hôm nay
                        if (ngaydat.equals(orderDateStr)) {
                            DonDatDTO dto = new DonDatDTO();
                            dto.setMaDonDat(res.getMaDonDat());
                            dto.setMaNV(res.getMaNV());
                            dto.setMaBan(res.getMaBan());
                            dto.setTongTien(String.valueOf(res.getTongTien()));
                            dto.setTinhTrang("true");
                            dto.setNgayDat(orderDateStr);
                            dto.setTenNV(res.getHoTenNV());
                            dto.setTenBan(res.getTenBan());
                            list.add(dto);
                        }
                    }
                    todayOrdersLiveData.postValue(list);
                    lastLoadTime = System.currentTimeMillis();
                }
            }

            @Override
            public void onFailure(Call<List<OrderResponse>> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                Log.e(TAG, "Lỗi tải đơn hàng trong ngày ở trang chủ: " + t.getMessage());
            }
        });
    }
}
