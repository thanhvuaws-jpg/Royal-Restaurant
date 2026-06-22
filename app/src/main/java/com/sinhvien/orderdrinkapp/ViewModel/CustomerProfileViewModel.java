package com.sinhvien.orderdrinkapp.ViewModel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.CustomerProfileResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerProfileViewModel extends AndroidViewModel {

    private static final String TAG = "CustomerProfileViewModel";

    private final MutableLiveData<CustomerProfileResponse> profileLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);

    public CustomerProfileViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<CustomerProfileResponse> getProfile() {
        return profileLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public void fetchCustomerProfile(int makh, boolean forceRefresh) {
        if (!forceRefresh && profileLiveData.getValue() != null) {
            return;
        }

        isLoadingLiveData.setValue(true);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCustomerProfile(makh).enqueue(new Callback<CustomerProfileResponse>() {
            @Override
            public void onResponse(Call<CustomerProfileResponse> call, Response<CustomerProfileResponse> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    profileLiveData.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<CustomerProfileResponse> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                Log.e(TAG, "Lỗi tải thông tin khách hàng: " + t.getMessage());
            }
        });
    }
}
