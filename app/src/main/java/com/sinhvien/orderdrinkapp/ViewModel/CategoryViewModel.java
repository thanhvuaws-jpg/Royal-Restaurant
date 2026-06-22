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

public class CategoryViewModel extends AndroidViewModel {

    private final MutableLiveData<List<LoaiMonDTO>> categoriesLiveData = new MutableLiveData<>();
    private final LocalDatabaseHelper dbHelper;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        dbHelper = LocalDatabaseHelper.getInstance(application);
    }

    public LiveData<List<LoaiMonDTO>> getCategories() {
        if (categoriesLiveData.getValue() == null) {
            loadCategoriesFromLocal();
        }
        return categoriesLiveData;
    }

    public void loadCategoriesFromLocal() {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<LoaiMonDTO> cachedList = dbHelper.getCategories();
            categoriesLiveData.postValue(cachedList);
        });
    }

    public void syncCategoriesFromServer(OnSyncCallback callback) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCategories().enqueue(new Callback<List<LoaiMonResponse>>() {
            @Override
            public void onResponse(Call<List<LoaiMonResponse>> call, Response<List<LoaiMonResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LoaiMonResponse> listServer = response.body();
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

    public interface OnSyncCallback {
        void onSuccess();
        void onError(String errorMsg);
    }
}
