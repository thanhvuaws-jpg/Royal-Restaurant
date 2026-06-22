package com.sinhvien.orderdrinkapp.Api;

import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Địa chỉ VPS của bạn
    public static final String BASE_URL = "http://103.157.204.120:8081/";
    public static final String BANK_ID = "BIDV";
    public static final String BANK_ACC = "6151099464";
    public static final String BANK_NAME = "VU THANH";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Cấu hình Timeout 30 giây để tránh lỗi khi mạng yếu
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    // Link gốc để tải ảnh
    public static String getBaseUrl() {
        return BASE_URL;
    }
}
