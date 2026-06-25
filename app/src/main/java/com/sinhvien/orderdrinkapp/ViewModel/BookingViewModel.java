package com.sinhvien.orderdrinkapp.ViewModel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * BookingViewModel - Lớp ViewModel quản lý dữ liệu Đặt bàn (Booking).
 * Chịu trách nhiệm gọi API lấy danh sách đặt bàn (toàn bộ hoặc theo khách hàng),
 * lưu trữ trạng thái tải dữ liệu (loading) và cập nhật UI thông qua LiveData.
 */
public class BookingViewModel extends AndroidViewModel {

    private static final String TAG = "BookingViewModel";

    // Danh sách toàn bộ lịch đặt bàn (cho Admin/Nhân viên quản lý)
    private final MutableLiveData<List<BookingResponse>> bookingsAllLiveData = new MutableLiveData<>(new ArrayList<>());
    // Danh sách lịch đặt bàn riêng của một Khách hàng
    private final MutableLiveData<List<BookingResponse>> customerBookingsLiveData = new MutableLiveData<>(new ArrayList<>());
    // Trạng thái đang tải dữ liệu từ API
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);

    public BookingViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Lấy LiveData chứa toàn bộ danh sách đặt bàn.
     */
    public LiveData<List<BookingResponse>> getBookingsAll() {
        return bookingsAllLiveData;
    }

    /**
     * Lấy LiveData chứa danh sách đặt bàn của khách hàng cụ thể.
     */
    public LiveData<List<BookingResponse>> getCustomerBookings() {
        return customerBookingsLiveData;
    }

    /**
     * Lấy LiveData trạng thái loading.
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    /**
     * Tải toàn bộ danh sách đặt bàn từ server.
     * @param forceRefresh Ép buộc tải lại dữ liệu mới từ server, không dùng dữ liệu cache.
     */
    public void fetchBookingsAll(boolean forceRefresh) {
        if (!forceRefresh && bookingsAllLiveData.getValue() != null && !bookingsAllLiveData.getValue().isEmpty()) {
            return;
        }

        isLoadingLiveData.setValue(true);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getBookings(0).enqueue(new Callback<List<BookingResponse>>() {
            @Override
            public void onResponse(Call<List<BookingResponse>> call, Response<List<BookingResponse>> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    bookingsAllLiveData.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<BookingResponse>> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                Log.e(TAG, "Lỗi tải toàn bộ danh sách đặt bàn: " + t.getMessage());
            }
        });
    }

    /**
     * Tải danh sách đặt bàn riêng của một khách hàng cụ thể.
     * @param makh ID của khách hàng thành viên.
     * @param forceRefresh Ép buộc tải lại dữ liệu mới từ server.
     */
    public void fetchBookingsForCustomer(int makh, boolean forceRefresh) {
        if (!forceRefresh && customerBookingsLiveData.getValue() != null && !customerBookingsLiveData.getValue().isEmpty()) {
            return;
        }

        isLoadingLiveData.setValue(true);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getBookings(makh).enqueue(new Callback<List<BookingResponse>>() {
            @Override
            public void onResponse(Call<List<BookingResponse>> call, Response<List<BookingResponse>> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    customerBookingsLiveData.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<BookingResponse>> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                Log.e(TAG, "Lỗi tải lịch sử đặt bàn khách hàng: " + t.getMessage());
            }
        });
    }
}
