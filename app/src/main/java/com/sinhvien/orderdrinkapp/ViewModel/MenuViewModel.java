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

/**
 * MenuViewModel - Lớp ViewModel quản lý danh sách Món ăn (Dish/Drink Menu).
 * Hỗ trợ lọc theo loại món (maloai), tìm kiếm món ăn bằng từ khóa, 
 * phân trang tải dữ liệu (pagination) và đồng bộ hóa lưu xuống SQLite.
 */
public class MenuViewModel extends AndroidViewModel {

    // LiveData chứa danh sách các món ăn hiển thị trên UI
    private final MutableLiveData<List<MonDTO>> dishesLiveData = new MutableLiveData<>();
    // Đối tượng Helper tương tác với SQLite
    private final LocalDatabaseHelper dbHelper;

    // Trang dữ liệu hiện tại để tải từ API phân trang
    private int currentPage = 1;
    // Cờ đánh dấu còn dữ liệu để tải tiếp hay không
    private boolean hasMore = true;
    // Cờ đánh dấu trạng thái đang gọi API tải dữ liệu tránh gọi trùng lặp
    private boolean isLoading = false;

    public MenuViewModel(@NonNull Application application) {
        super(application);
        dbHelper = LocalDatabaseHelper.getInstance(application);
    }

    /**
     * Lấy LiveData danh sách món ăn.
     * Tự động tải từ SQLite nếu dữ liệu LiveData hiện thời đang rỗng.
     * @param maloai Mã loại món cần lọc.
     * @param query Từ khóa tìm kiếm món ăn.
     */
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

    /**
     * Tải danh sách món ăn từ SQLite nội bộ lên LiveData để hiển thị ngay lập tức (offline).
     */
    public void loadDishesFromLocal(int maloai, String query) {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<MonDTO> cachedList = dbHelper.getDishes(maloai, query);
            dishesLiveData.postValue(cachedList);
        });
    }

    /**
     * Tải trang tiếp theo của danh sách món ăn từ Server, đồng bộ hóa vào SQLite cục bộ.
     * @param maloai Mã loại món ăn cần lọc.
     * @param query Từ khóa tìm kiếm món ăn.
     * @param forceRefresh Ép buộc tải lại từ trang đầu tiên (reset trang về 1).
     * @param callback Callback nhận thông tin trạng thái tải hoàn tất.
     */
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
                        // Thực hiện ghi và đồng bộ dữ liệu món ăn vào SQLite cục bộ ở Background Thread
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

    /**
     * Interface Callback xử lý sự kiện phân trang tải thêm món ăn hoàn tất.
     */
    public interface OnLoadMoreCallback {
        void onFinished(boolean success);
    }
}
