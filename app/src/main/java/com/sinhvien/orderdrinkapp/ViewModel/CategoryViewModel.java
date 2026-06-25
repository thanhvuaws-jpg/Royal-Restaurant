package com.sinhvien.orderdrinkapp.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.LoaiMonResponse;
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CategoryViewModel - Lớp ViewModel quản lý danh mục nhóm món ăn (Category).
 * Kết hợp tải dữ liệu danh mục từ SQLite nội bộ (để hiển thị nhanh offline)
 * và đồng bộ cập nhật dữ liệu mới từ Server VPS về SQLite.
 */
public class CategoryViewModel extends AndroidViewModel {

    // LiveData nắm giữ danh sách danh mục món ăn
    private final MutableLiveData<List<LoaiMonDTO>> categoriesLiveData = new MutableLiveData<>();
    // Đối tượng truy vấn cơ sở dữ liệu SQLite cục bộ
    private final LocalDatabaseHelper dbHelper;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        dbHelper = LocalDatabaseHelper.getInstance(application);
    }

    /**
     * Lấy LiveData danh sách danh mục món ăn.
     * Tự động tải từ SQLite nếu LiveData hiện thời đang trống.
     */
    public LiveData<List<LoaiMonDTO>> getCategories() {
        if (categoriesLiveData.getValue() == null) {
            loadCategoriesFromLocal();
        }
        return categoriesLiveData;
    }

    /**
     * Tải danh sách danh mục món ăn từ SQLite lên LiveData thông qua luồng chạy nền (Executor).
     */
    public void loadCategoriesFromLocal() {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<LoaiMonDTO> cachedList = dbHelper.getCategories();
            categoriesLiveData.postValue(cachedList);
        });
    }

    /**
     * Đồng bộ danh sách danh mục từ API Server về lưu trữ SQLite cục bộ.
     * @param callback Callback thông báo kết quả đồng bộ thành công hay thất bại.
     */
    public void syncCategoriesFromServer(OnSyncCallback callback) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCategories().enqueue(new Callback<List<LoaiMonResponse>>() {
            @Override
            public void onResponse(Call<List<LoaiMonResponse>> call, Response<List<LoaiMonResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LoaiMonResponse> listServer = response.body();
                    // Lưu danh mục đồng bộ từ server vào SQLite trên Background Thread
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncCategories(listServer);
                        List<LoaiMonDTO> updatedList = dbHelper.getCategories();
                        categoriesLiveData.postValue(updatedList);
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
            public void onFailure(Call<List<LoaiMonResponse>> call, Throwable t) {
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }

    /**
     * Giao diện Callback nhận kết quả đồng bộ danh mục.
     */
    public interface OnSyncCallback {
        void onSuccess();
        void onError(String errorMsg);
    }
}
