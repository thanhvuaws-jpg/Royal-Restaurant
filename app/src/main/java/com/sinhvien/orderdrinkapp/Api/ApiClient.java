package com.sinhvien.orderdrinkapp.Api;

import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ApiClient — Lớp cấu hình kết nối mạng (Retrofit Singleton).
 *
 * Nhiệm vụ: Tạo ra và quản lý một đối tượng Retrofit DUY NHẤT
 * được dùng xuyên suốt toàn bộ ứng dụng để gọi API lên VPS.
 *
 * Kiến thức áp dụng: Design Pattern "Singleton" — chỉ tạo một
 * instance duy nhất để tiết kiệm tài nguyên và tránh tạo kết nối thừa.
 */
public class ApiClient {
    /** Địa chỉ gốc của VPS (IP:Port). Mọi endpoint API đều ghép sau URL này. */
    public static final String BASE_URL = "http://103.157.204.120:8081/";



    /** Đối tượng Retrofit duy nhất — dùng chung toàn app (Singleton). */
    private static Retrofit retrofit = null;

    /**
     * Trả về đối tượng Retrofit đã được cấu hình sẵn.
     * Nếu chưa được tạo lần nào thì mới khởi tạo, còn không thì dùng lại cái cũ.
     * @return Retrofit instance đã cấu hình timeout và JSON converter.
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            // Cấu hình Timeout 30 giây để tránh lỗi khi mạng yếu
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS) // Giới hạn thời gian chờ kết nối tới server
                    .readTimeout(30, TimeUnit.SECONDS)    // Giới hạn thời gian chờ đọc dữ liệu từ server
                    .writeTimeout(30, TimeUnit.SECONDS)   // Giới hạn thời gian chờ gửi dữ liệu lên server
                    .build();

            // Cấu hình Gson để chuyển đổi JSON linh hoạt hơn (setLenient bỏ qua lỗi JSON không chặt chẽ)
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            // Tạo đối tượng Retrofit và gắn URL gốc + HTTP Client + JSON Converter vào
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    /**
     * Trả về URL gốc của server.
     * Dùng để ghép đường dẫn ảnh khi ảnh không có URL đầy đủ từ Cloudinary.
     * @return Chuỗi BASE_URL.
     */
    public static String getBaseUrl() {
        return BASE_URL;
    }
}
