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

public class StaffViewModel extends AndroidViewModel {

    private final MutableLiveData<List<NhanVienDTO>> staffLiveData = new MutableLiveData<>();
    private final LocalDatabaseHelper dbHelper;

    public StaffViewModel(@NonNull Application application) {
        super(application);
        dbHelper = LocalDatabaseHelper.getInstance(application);
    }

    public LiveData<List<NhanVienDTO>> getStaff() {
        if (staffLiveData.getValue() == null) {
            loadStaffFromLocal();
        }
        return staffLiveData;
    }

    public void loadStaffFromLocal() {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<NhanVienDTO> cachedList = dbHelper.getStaff();
            staffLiveData.postValue(cachedList);
        });
    }

    public void syncStaffFromServer(OnSyncCallback callback) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getStaff().enqueue(new Callback<List<StaffResponse>>() {
            @Override
            public void onResponse(Call<List<StaffResponse>> call, Response<List<StaffResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<StaffResponse> listServer = response.body();
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

    public interface OnSyncCallback {
        void onSuccess();
        void onError(String errorMsg);
    }
}
