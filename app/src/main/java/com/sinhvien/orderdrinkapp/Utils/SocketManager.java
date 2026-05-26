package com.sinhvien.orderdrinkapp.Utils;

import android.content.Context;
import android.util.Log;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket socket;

    private SocketManager() {
        try {
            // Lấy địa chỉ IP/domain từ ApiClient và bỏ port 8081 để trỏ về host Apache chính
            String socketUrl = ApiClient.BASE_URL.replace(":8081/", "");
            
            IO.Options options = new IO.Options();
            options.path = "/socket.io/";
            
            Log.d(TAG, "Initializing Socket.io client pointing to: " + socketUrl);
            socket = IO.socket(socketUrl, options);
            
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "Socket disconnected. Reconnecting in 3 seconds...");
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (socket != null && !socket.connected()) {
                        socket.connect();
                    }
                }, 3000);
            });
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization failed: " + e.getMessage());
        }
    }

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public Socket getSocket() {
        return socket;
    }

    public void connect() {
        if (socket != null && !socket.connected()) {
            socket.connect();
            Log.d(TAG, "Connecting to WebSocket server...");
        }
    }

    public void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
            Log.d(TAG, "Disconnecting from WebSocket server...");
        }
    }
}
