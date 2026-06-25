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

/**
 * TableViewModel - Lớp ViewModel quản lý danh sách Bàn ăn (Table).
 * Phục vụ việc hiển thị danh sách bàn ăn, trạng thái sử dụng bàn (trống/có khách/đã đặt trước)
 * từ SQLite cục bộ và đồng bộ liên tục với API server.
 */
public class TableViewModel extends AndroidViewModel {

    // LiveData lưu trữ danh sách bàn ăn lấy từ SQLite
    private final MutableLiveData<List<BanAnDTO>> tablesLiveData = new MutableLiveData<>();
    // LiveData lưu trữ thông tin tình trạng đặt bàn từ Server
    private final MutableLiveData<List<TableResponse>> reservedTablesLiveData = new MutableLiveData<>();
    // Đối tượng Helper hỗ trợ SQLite
    private final LocalDatabaseHelper dbHelper;

    public TableViewModel(@NonNull Application application) {
        super(application);
        dbHelper = LocalDatabaseHelper.getInstance(application);
    }

    /**
     * Lấy LiveData danh sách các bàn ăn.
     * Tự động tải từ SQLite nếu LiveData hiện thời đang trống.
     */
    public LiveData<List<BanAnDTO>> getTables() {
        if (tablesLiveData.getValue() == null) {
            loadTablesFromLocal();
        }
        return tablesLiveData;
    }

    /**
     * Lấy LiveData chứa thông tin trạng thái đặt bàn trước từ Server.
     */
    public LiveData<List<TableResponse>> getReservedTables() {
        return reservedTablesLiveData;
    }

    /**
     * Tải danh sách bàn ăn từ SQLite cục bộ lên LiveData trên Background Thread.
     */
    public void loadTablesFromLocal() {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<BanAnDTO> cachedList = dbHelper.getTables();
            tablesLiveData.postValue(cachedList);
        });
    }

    /**
     * Đồng bộ danh sách bàn ăn từ API Server về SQLite cục bộ.
     * @param fetchReserved Nếu true, sau khi đồng bộ bàn xong sẽ tự động tải trạng thái các bàn được đặt trước.
     * @param callback Callback thông báo kết quả đồng bộ.
     */
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

    /**
     * Tải thông tin các bàn đang có người đặt trước (Reserved Status) từ Server VPS.
     */
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

    /**
     * Giao diện Callback đồng bộ thông tin bàn ăn.
     */
    public interface OnSyncCallback {
        void onSuccess();
        void onError(String errorMsg);
    }
}
