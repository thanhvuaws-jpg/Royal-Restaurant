package com.sinhvien.orderdrinkapp.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.KhoResponse;
import com.sinhvien.orderdrinkapp.Api.NguyenLieuResponse;
import com.sinhvien.orderdrinkapp.Api.NhomNguyenLieuResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * KhoViewModel - Quản lý trạng thái phân trang, tìm kiếm, lọc và bộ nhớ đệm
 * cho màn hình Kho nguyên liệu trên Android.
 * Tuân thủ yêu cầu:
 * - 20 mục / trang (MOI_TRANG = 20)
 * - Tối đa 3 trang trong bộ nhớ (60 mục)
 * - Hủy yêu cầu mạng khi rời màn hình (cancelPendingCalls)
 */
public class KhoViewModel extends AndroidViewModel {

    public static final int MOI_TRANG = 20;
    public static final int MAX_TRANG_BO_NHO = 3;
    public static final int MAX_ITEMS_BO_NHO = MOI_TRANG * MAX_TRANG_BO_NHO; // 60

    private final MutableLiveData<List<NguyenLieuResponse>> nguyenLieuListLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<KhoResponse.DemTon> demTonLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> canhBaoHanLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Double> giaTriTonLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<NhomNguyenLieuResponse.NhomItem>> nhomListLiveData = new MutableLiveData<>(new ArrayList<>());

    private final List<NguyenLieuResponse> currentItems = new ArrayList<>();

    private int currentPage = 1;
    private boolean hasMore = true;
    private Integer filterMaNhom = null;
    private String filterTuKhoa = "";
    private boolean filterChiCanhBao = false;

    private Call<KhoResponse> currentCall = null;
    private Call<NhomNguyenLieuResponse> nhomCall = null;

    public KhoViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<NguyenLieuResponse>> getNguyenLieuList() {
        return nguyenLieuListLiveData;
    }

    public LiveData<KhoResponse.DemTon> getDemTon() {
        return demTonLiveData;
    }

    public LiveData<Integer> getCanhBaoHan() {
        return canhBaoHanLiveData;
    }

    public LiveData<Double> getGiaTriTon() {
        return giaTriTonLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public LiveData<List<NhomNguyenLieuResponse.NhomItem>> getNhomList() {
        return nhomListLiveData;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void loadNhom() {
        if (nhomCall != null && !nhomCall.isCanceled()) {
            nhomCall.cancel();
        }
        ApiService api = ApiClient.getApiService();
        nhomCall = api.getKhoNhom();
        nhomCall.enqueue(new Callback<NhomNguyenLieuResponse>() {
            @Override
            public void onResponse(Call<NhomNguyenLieuResponse> call, Response<NhomNguyenLieuResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NhomNguyenLieuResponse.NhomItem> list = response.body().getDanhSach();
                    if (list != null) {
                        nhomListLiveData.setValue(list);
                    }
                }
            }

            @Override
            public void onFailure(Call<NhomNguyenLieuResponse> call, Throwable t) {
                // Hủy hoặc lỗi mạng nhẹ: giữ nguyên danh mục hiện có
            }
        });
    }

    public void loadFirstPage() {
        currentPage = 1;
        hasMore = true;
        fetchData(1);
    }

    public void loadNextPage() {
        if (Boolean.TRUE.equals(isLoadingLiveData.getValue()) || !hasMore) {
            return;
        }
        fetchData(currentPage + 1);
    }

    public void applyFilter(Integer maNhom, String tuKhoa, boolean chiCanhBao) {
        this.filterMaNhom = (maNhom != null && maNhom > 0) ? maNhom : null;
        this.filterTuKhoa = tuKhoa != null ? tuKhoa.trim() : "";
        this.filterChiCanhBao = chiCanhBao;
        loadFirstPage();
    }

    private void fetchData(int page) {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }

        isLoadingLiveData.setValue(true);
        ApiService api = ApiClient.getApiService();

        String canhBaoParam = filterChiCanhBao ? "1" : null;
        currentCall = api.getKhoDanhSach(page, MOI_TRANG, filterMaNhom, filterTuKhoa, canhBaoParam);

        currentCall.enqueue(new Callback<KhoResponse>() {
            @Override
            public void onResponse(Call<KhoResponse> call, Response<KhoResponse> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    KhoResponse data = response.body();
                    currentPage = page;
                    hasMore = data.isConNua();

                    demTonLiveData.setValue(data.getDemTon());
                    giaTriTonLiveData.setValue(data.getGiaTriTon());
                    KhoResponse.CanhBaoHan cb = data.getCanhBaoHan();
                    canhBaoHanLiveData.setValue(cb == null ? 0 : cb.getHetHan() + cb.getSapHetHan());

                    List<NguyenLieuResponse> newItems = data.getDanhSach();
                    if (newItems == null) {
                        newItems = new ArrayList<>();
                    }

                    if (page == 1) {
                        currentItems.clear();
                        currentItems.addAll(newItems);
                    } else {
                        currentItems.addAll(newItems);
                        // Giữ tối đa 3 trang trong bộ nhớ: nếu vượt quá 60 mục, cắt bỏ các mục cũ nhất
                        if (currentItems.size() > MAX_ITEMS_BO_NHO) {
                            int removeCount = currentItems.size() - MAX_ITEMS_BO_NHO;
                            for (int i = 0; i < removeCount; i++) {
                                currentItems.remove(0);
                            }
                        }
                    }

                    nguyenLieuListLiveData.setValue(new ArrayList<>(currentItems));
                } else {
                    errorMessageLiveData.setValue("Không thể tải danh sách kho (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<KhoResponse> call, Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                isLoadingLiveData.setValue(false);
                errorMessageLiveData.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void cancelPendingCalls() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
        if (nhomCall != null && !nhomCall.isCanceled()) {
            nhomCall.cancel();
        }
        isLoadingLiveData.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelPendingCalls();
    }
}
