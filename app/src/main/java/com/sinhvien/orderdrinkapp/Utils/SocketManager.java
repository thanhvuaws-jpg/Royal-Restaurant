package com.sinhvien.orderdrinkapp.Utils;

import android.content.Context;
import android.util.Log;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

/**
 * SocketManager — Quản lý kết nối WebSocket thời gian thực (Singleton).
 *
 * Nhiệm vụ: Duy trì một kết nối Socket.io liên tục với Node.js server
 * để nhận thông báo đẩy (push notification) ngay khi có đơn hàng mới,
 * xác nhận thanh toán, hoặc cập nhật trạng thái bàn mà không cần
 * client phải hỏi server liên tục (polling).
 *
 * Kiến thức áp dụng: Design Pattern "Singleton" — toàn app chỉ dùng
 * một kết nối Socket duy nhất để tránh tốn băng thông.
 */
public class SocketManager {
    private static final String TAG = "SocketManager";

    /** Instance duy nhất của SocketManager (Singleton). */
    private static SocketManager instance;

    /** Đối tượng Socket.io thực hiện kết nối tới server. */
    private Socket socket;

    /** Cờ đánh dấu xem việc ngắt kết nối là chủ ý hay do lỗi mạng.
     * true = Chủ ý ngắt (do đăng xuất) → Không tự kết nối lại.
     * false = Mất mạng không chủ ý → Tự động thử kết nối lại. */
    private boolean intentionalDisconnect = false;

    /**
     * Constructor private — chỉ được gọi 1 lần từ bên trong class này.
     * Thiết lập kết nối Socket.io tới server và đăng ký các event listener.
     */
    private SocketManager() {
        try {
            // Lấy địa chỉ IP/domain từ ApiClient và bỏ port 8081 để trỏ về host Apache chính
            String socketUrl = ApiClient.BASE_URL.replace(":8081/", "");
            
            IO.Options options = new IO.Options();
            options.path = "/socket.io/";
            
            Log.d(TAG, "Initializing Socket.io client pointing to: " + socketUrl);
            socket = IO.socket(socketUrl, options);

            // Lắng nghe sự kiện kết nối thành công
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "Socket connected successfully.");
                intentionalDisconnect = false;
            });
            
            // Lắng nghe sự kiện mất kết nối — Tự động kết nối lại sau 3 giây nếu không phải chủ ý
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "Socket disconnected. intentionalDisconnect=" + intentionalDisconnect);
                if (!intentionalDisconnect) {
                    Log.d(TAG, "Reconnecting in 3 seconds...");
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (socket != null && !socket.connected() && !intentionalDisconnect) {
                            socket.connect();
                        }
                    }, 3000); // Chờ 3000ms = 3 giây rồi thử lại
                }
            });
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization failed: " + e.getMessage());
        }
    }

    /**
     * Trả về instance SocketManager duy nhất.
     * Dùng từ khóa synchronized để đảm bảo an toàn khi nhiều luồng (thread)
     * cùng truy cập.
     */
    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    /**
     * Trả về đối tượng Socket.io gốc để các nơi khác có thể
     * đăng ký lắng nghe event tuỳ chỉnh (VD: "new_order", "payment_confirmed").
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Bắt đầu kết nối tới WebSocket server.
     * Gọi khi người dùng đăng nhập thành công vào HomeActivity.
     */
    public void connect() {
        intentionalDisconnect = false;
        if (socket != null && !socket.connected()) {
            socket.connect();
            Log.d(TAG, "Connecting to WebSocket server...");
        }
    }

    /**
     * Ngắt kết nối WebSocket một cách có chủ ý.
     * Gọi khi người dùng đăng xuất để tránh nhận thông báo không cần thiết.
     */
    public void disconnect() {
        intentionalDisconnect = true; // Đánh dấu là ngắt chủ ý → Không tự kết nối lại
        if (socket != null && socket.connected()) {
            socket.disconnect();
            Log.d(TAG, "Disconnecting from WebSocket server...");
        }
    }
}
