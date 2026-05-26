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
    private boolean intentionalDisconnect = false;

    private SocketManager() {
        try {
            // Lấy địa chỉ IP/domain từ ApiClient và bỏ port 8081 để trỏ về host Apache chính
            String socketUrl = ApiClient.BASE_URL.replace(":8081/", "");
            
            IO.Options options = new IO.Options();
            options.path = "/socket.io/";
            
            Log.d(TAG, "Initializing Socket.io client pointing to: " + socketUrl);
            socket = IO.socket(socketUrl, options);

            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "Socket connected successfully.");
                intentionalDisconnect = false;
            });
            
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "Socket disconnected. intentionalDisconnect=" + intentionalDisconnect);
                if (!intentionalDisconnect) {
                    Log.d(TAG, "Reconnecting in 3 seconds...");
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (socket != null && !socket.connected() && !intentionalDisconnect) {
                            socket.connect();
                        }
                    }, 3000);
                }
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
        intentionalDisconnect = false;
        if (socket != null && !socket.connected()) {
            socket.connect();
            Log.d(TAG, "Connecting to WebSocket server...");
        }
    }

    public void disconnect() {
        intentionalDisconnect = true;
        if (socket != null && socket.connected()) {
            socket.disconnect();
            Log.d(TAG, "Disconnecting from WebSocket server...");
        }
    }
}
