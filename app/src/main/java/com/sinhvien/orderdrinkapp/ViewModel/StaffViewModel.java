package com.sinhvien.orderdrinkapp.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.StaffResponse;
import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * StaffViewModel - Lớp ViewModel quản lý danh sách Nhân viên (Staff).
 * Cung cấp dữ liệu nhân viên từ SQLite nội bộ phục vụ việc hiển thị danh sách offline nhanh chóng,
 * đồng thời hỗ trợ gọi API tải danh sách nhân viên từ Server VPS và cập nhật/đồng bộ vào SQLite.
 */
public class StaffViewModel extends AndroidViewModel {

    // LiveData nắm giữ danh sách nhân viên hiển thị trên UI
    private final MutableLiveData<List<NhanVienDTO>> staffLiveData = new MutableLiveData<>();
    // Đối tượng truy vấn cơ sở dữ liệu SQLite
    private final LocalDatabaseHelper dbHelper;

    public StaffViewModel(@NonNull Application application) {
        super(application);
        dbHelper = LocalDatabaseHelper.getInstance(application);
    }

    /**
     * Lấy LiveData danh sách nhân viên.
     * Tự động tải từ SQLite nếu LiveData hiện thời đang rỗng.
     */
    public LiveData<List<NhanVienDTO>> getStaff() {
        if (staffLiveData.getValue() == null) {
            loadStaffFromLocal();
        }
        return staffLiveData;
    }

    /**
     * Tải danh sách nhân viên từ cơ sở dữ liệu SQLite lên LiveData trên Background Thread.
     */
    public void loadStaffFromLocal() {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<NhanVienDTO> cachedList = dbHelper.getStaff();
            staffLiveData.postValue(cachedList);
        });
    }

    /**
     * Đồng bộ danh sách nhân viên từ API Server về SQLite cục bộ.
     * @param callback Callback nhận kết quả đồng bộ thành công hay lỗi.
     */
    public void syncStaffFromServer(OnSyncCallback callback) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getStaff().enqueue(new Callback<List<StaffResponse>>() {
            @Override
            public void onResponse(Call<List<StaffResponse>> call, Response<List<StaffResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<StaffResponse> listServer = response.body();
                    // Lưu dữ liệu vào SQLite trên Background Thread
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncStaff(listServer);
                        List<NhanVienDTO> updatedList = dbHelper.getStaff();
                        staffLiveData.postValue(updatedList);
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                } else {
                    if (callback != null) {
                        callback.onError("Lỗi phản hồi từ server");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<StaffResponse>> call, Throwable t) {
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }

    /**
     * Giao diện Callback kết quả đồng bộ danh sách nhân viên.
     */
    public interface OnSyncCallback {
        void onSuccess();
        void onError(String errorMsg);
    }

    public void deleteStaff(int manv, OnDeleteCallback callback) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.manageStaff("delete", manv, "", "", "", "", "", "", "", 0).enqueue(new Callback<com.sinhvien.orderdrinkapp.Api.OrderResponse>() {
            @Override
            public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.OrderResponse> call, Response<com.sinhvien.orderdrinkapp.Api.OrderResponse> response) {
                if (response.isSuccessful()) {
                    if (callback != null) callback.onSuccess();
                    // Tải lại danh sách sau khi xóa thành công
                    syncStaffFromServer(null);
                } else {
                    if (callback != null) callback.onError("Xóa thất bại");
                }
            }

            @Override
            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.OrderResponse> call, Throwable t) {
                if (callback != null) callback.onError(t.getMessage());
            }
        });
    }

    public interface OnDeleteCallback {
        void onSuccess();
        void onError(String errorMsg);
    }
}
