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
    /**
     * Địa chỉ gốc của máy chủ. Mọi endpoint API đều ghép sau URL này theo
     * dạng BASE_URL + "api/<tên tệp>.php".
     *
     * 10.0.2.2 là địa chỉ mà máy ảo Android dùng để gọi về "localhost" của
     * máy thật — trên thiết bị thật phải đổi thành IP LAN của máy chủ.
     *
     * Cổng 8000 là container web (Apache), không phải container API. Apache
     * chuyển tiếp /api/ sang REST API và /socket.io/ sang Socket.IO server,
     * nên toàn hệ thống chỉ còn MỘT cổng vào duy nhất cho cả app lẫn web.
     * Trước đây app gọi thẳng cổng 8081 của container API, bỏ qua Apache —
     * khiến app và web đi hai đường khác nhau và khó cấu hình thống nhất.
     *
     * Địa chỉ lấy từ BuildConfig.MAY_CHU (app/build.gradle), mặc định
     * "http://10.0.2.2:8000/". Build với -PmayChu=http://10.0.2.2:8097/ là
     * được bản trỏ vào sân thử (CSDL bản sao) mà không phải sửa dòng mã nào.
     */
    public static final String BASE_URL = com.sinhvien.orderdrinkapp.BuildConfig.MAY_CHU + "api/";

    /**
     * Địa chỉ Socket.IO. Tách thành hằng riêng thay vì suy ra từ BASE_URL
     * bằng phép thay chuỗi như trước (BASE_URL.replace(":8081/", "")) —
     * cách cũ phụ thuộc vào việc BASE_URL phải chứa đúng ":8081/", nên chỉ
     * cần đổi cổng là hỏng ngầm mà không báo lỗi. Cùng máy chủ với BASE_URL,
     * bỏ dấu "/" cuối.
     */
    public static final String SOCKET_URL =
            com.sinhvien.orderdrinkapp.BuildConfig.MAY_CHU.replaceAll("/+$", "");

    /** Đối tượng Retrofit duy nhất — dùng chung toàn app (Singleton). */
    private static Retrofit retrofit = null;

    // -----------------------------------------------------------------
    // Thông tin phiên dùng cho xác thực
    // -----------------------------------------------------------------
    // Máy chủ yêu cầu hai header X-Manv và X-Token ở hầu hết endpoint
    // (xem api/require_auth.php). Gắn chúng bằng Interceptor tại đây là
    // sửa MỘT chỗ duy nhất, thay vì thêm tham số cho hơn 30 phương thức
    // trong ApiService.
    //
    // Dùng biến tĩnh vì getClient() được gọi từ rất nhiều nơi mà không có
    // Context, trong khi SessionManager lại cần Context để đọc
    // SharedPreferences. SessionManager có nhiệm vụ đồng bộ hai giá trị
    // này mỗi khi phiên thay đổi; SplashActivity khôi phục chúng lúc khởi
    // động ứng dụng (phòng trường hợp tiến trình bị hệ thống thu hồi).
    private static volatile String authManv  = "";
    private static volatile String authToken = "";

    /** Ghi nhận phiên hiện tại để mọi lời gọi sau đó mang theo. */
    public static void setAuth(int manv, String token) {
        authManv  = manv > 0 ? String.valueOf(manv) : "";
        authToken = token != null ? token : "";
    }

    /** Xóa phiên khi đăng xuất. */
    public static void clearAuth() {
        authManv  = "";
        authToken = "";
    }

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
                    // Tự động đính kèm thông tin phiên vào MỌI yêu cầu.
                    // Chỉ gắn khi đã có phiên: các endpoint công khai như
                    // login.php vẫn gọi được bình thường lúc chưa đăng nhập.
                    .addInterceptor(chain -> {
                        okhttp3.Request goc = chain.request();
                        if (authManv.isEmpty() || authToken.isEmpty()) {
                            return chain.proceed(goc);
                        }
                        okhttp3.Request coXacThuc = goc.newBuilder()
                                .header("X-Manv", authManv)
                                .header("X-Token", authToken)
                                .build();
                        return chain.proceed(coXacThuc);
                    })
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

    /**
     * Trả về ApiService đã khởi tạo sẵn.
     */
    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}

