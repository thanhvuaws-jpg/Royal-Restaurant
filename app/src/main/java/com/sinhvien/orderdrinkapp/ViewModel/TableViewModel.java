package com.sinhvien.orderdrinkapp.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.TableResponse;
import com.sinhvien.orderdrinkapp.DTO.BanAnDTO;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TableViewModel extends AndroidViewModel {

    private final MutableLiveData<List<BanAnDTO>> tablesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<TableResponse>> reservedTablesLiveData = new MutableLiveData<>();
    private final LocalDatabaseHelper dbHelper;

    public TableViewModel(@NonNull Application application) {
        super(application);
        dbHelper = LocalDatabaseHelper.getInstance(application);
    }

    public LiveData<List<BanAnDTO>> getTables() {
        if (tablesLiveData.getValue() == null) {
            loadTablesFromLocal();
        }
        return tablesLiveData;
    }

    public LiveData<List<TableResponse>> getReservedTables() {
        return reservedTablesLiveData;
    }

    public void loadTablesFromLocal() {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<BanAnDTO> cachedList = dbHelper.getTables();
            tablesLiveData.postValue(cachedList);
        });
    }

    public void syncTablesFromServer(boolean fetchReserved, OnSyncCallback callback) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getTables().enqueue(new Callback<List<TableResponse>>() {
            @Override
            public void onResponse(Call<List<TableResponse>> call, Response<List<TableResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TableResponse> listServer = response.body();
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncTables(listServer);
                        List<BanAnDTO> updatedList = dbHelper.getTables();
                        tablesLiveData.postValue(updatedList);
                        if (fetchReserved) {
                            fetchReservedTables();
                        }
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
            public void onFailure(Call<List<TableResponse>> call, Throwable t) {
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }

    public void fetchReservedTables() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getTableBookingStatus().enqueue(new Callback<List<TableResponse>>() {
            @Override
            public void onResponse(Call<List<TableResponse>> call, Response<List<TableResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reservedTablesLiveData.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<TableResponse>> call, Throwable t) {}
        });
    }

    public interface OnSyncCallback {
        void onSuccess();
        void onError(String errorMsg);
    }
}
