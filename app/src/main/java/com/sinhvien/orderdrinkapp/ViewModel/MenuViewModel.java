package com.sinhvien.orderdrinkapp.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.DishPageResponse;
import com.sinhvien.orderdrinkapp.Api.MonResponse;
import com.sinhvien.orderdrinkapp.DTO.MonDTO;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuViewModel extends AndroidViewModel {

    private final MutableLiveData<List<MonDTO>> dishesLiveData = new MutableLiveData<>();
    private final LocalDatabaseHelper dbHelper;

    private int currentPage = 1;
    private boolean hasMore = true;
    private boolean isLoading = false;

    public MenuViewModel(@NonNull Application application) {
        super(application);
        dbHelper = LocalDatabaseHelper.getInstance(application);
    }

    public LiveData<List<MonDTO>> getDishes(int maloai, String query) {
        if (dishesLiveData.getValue() == null) {
            loadDishesFromLocal(maloai, query);
        }
        return dishesLiveData;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }

    public void loadDishesFromLocal(int maloai, String query) {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<MonDTO> cachedList = dbHelper.getDishes(maloai, query);
            dishesLiveData.postValue(cachedList);
        });
    }

    public void loadMoreDishes(int maloai, String query, boolean forceRefresh, OnLoadMoreCallback callback) {
        if (isLoading) {
            if (callback != null) callback.onFinished(false);
            return;
        }

        if (forceRefresh) {
            currentPage = 1;
            hasMore = true;
        }

        if (!hasMore && !forceRefresh) {
            if (callback != null) callback.onFinished(false);
            return;
        }

        isLoading = true;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getDishes(maloai, currentPage, 1000, query).enqueue(new Callback<DishPageResponse>() {
            @Override
            public void onResponse(Call<DishPageResponse> call, Response<DishPageResponse> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    List<MonResponse> newItems = response.body().getData();
                    hasMore = response.body().isHasMore();

                    if (newItems != null) {
                        currentPage++;
                        LocalDatabaseHelper.getExecutor().execute(() -> {
                            dbHelper.syncDishes(maloai, newItems, currentPage == 2);
                            List<MonDTO> updatedList = dbHelper.getDishes(maloai, query);
                            dishesLiveData.postValue(updatedList);
                            if (callback != null) callback.onFinished(true);
                        });
                    } else {
                        if (callback != null) callback.onFinished(true);
                    }
                } else {
                    if (callback != null) callback.onFinished(false);
                }
            }

            @Override
            public void onFailure(Call<DishPageResponse> call, Throwable t) {
                isLoading = false;
                if (callback != null) callback.onFinished(false);
            }
        });
    }

    public interface OnLoadMoreCallback {
        void onFinished(boolean success);
    }
}
