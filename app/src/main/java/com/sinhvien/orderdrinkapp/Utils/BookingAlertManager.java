package com.sinhvien.orderdrinkapp.Utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import java.lang.ref.WeakReference;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * BookingAlertManager - Bộ quản lý và cảnh báo Đặt bàn quá hạn hoặc chuẩn bị bàn.
 * Tự động chạy nền kiểm tra định kỳ hoặc thông qua Socket IO để thông báo đẩy (Notification) hoặc hiện AlertDialog nhắc nhở nhân viên.
 */
public class BookingAlertManager {

    // ID kênh thông báo cho đặt bàn
    private static final String CHANNEL_ID = "booking_alerts";
    // Tên hiển thị của kênh thông báo
    private static final String CHANNEL_NAME = "Nhắc nhở đặt bàn";
    // Khoảng thời gian giữa các lần quét (quét mỗi 60 giây / 1 phút)
    private static final int CHECK_INTERVAL = 60000; 

    // Sử dụng WeakReference tránh rò rỉ bộ nhớ (memory leak) đối với Context của Activity
    private WeakReference<Context> contextRef;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    // Cờ trạng thái cho biết bộ quét cảnh báo có đang chạy hay không
    private boolean isRunning = false;

    /**
     * Khởi tạo BookingAlertManager.
     * @param context Context của màn hình/ứng dụng (nên là Activity Context để hiện dialog).
     */
    public BookingAlertManager(Context context) {
        this.contextRef = new WeakReference<>(context); // [FIX] Bỏ getApplicationContext() để AlertDialog có thể dùng được Context này
        createNotificationChannel();
    }

    /**
     * Tạo Kênh Thông Báo (Notification Channel) cho Android 8.0 trở lên.
     * Thiết lập độ ưu tiên cao HIGH để thông báo có thể hiển thị dạng banner trên màn hình.
     */
    private void createNotificationChannel() {
        Context context = contextRef.get();
        if (context == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Cảnh báo quá giờ nhận bàn");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // Bộ lắng nghe sự kiện khi trạng thái đặt bàn bị thay đổi trên Socket IO
    private io.socket.emitter.Emitter.Listener onBookingStatusUpdated = new io.socket.emitter.Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Khi có thay đổi, quét kiểm tra bàn quá hạn ngay lập tức
            checkOverdueBookings();
        }
    };

    // Bộ lắng nghe yêu cầu thu ngân nhắc nhở chuẩn bị bàn ăn
    private io.socket.emitter.Emitter.Listener onNotifyPrepareTable = new io.socket.emitter.Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (args.length > 0 && args[0] != null) {
                String tenBan = "";
                try {
                    if (args[0] instanceof org.json.JSONObject) {
                        tenBan = ((org.json.JSONObject) args[0]).getString("tenban");
                    } else if (args[0] instanceof String) {
                        org.json.JSONObject data = new org.json.JSONObject((String) args[0]);
                        tenBan = data.getString("tenban");
                    } else {
                        tenBan = args[0].toString();
                    }
                } catch (Exception e) {
                    tenBan = args[0].toString();
                }
                
                // Hiển thị thông báo và Dialog cảnh báo nhân viên chuẩn bị bàn ăn
                showPrepareTableNotification(tenBan);
            }
        }
    };

    /**
     * Bắt đầu kiểm tra và lắng nghe sự kiện cảnh báo.
     * Đăng ký Socket Listener và bắt đầu vòng lặp Runnable chạy định kỳ.
     */
    public void startChecking() {
        if (isRunning) return;
        isRunning = true;

        io.socket.client.Socket socket = SocketManager.getInstance().getSocket();
        if (socket != null) {
            socket.on("booking_status_updated", onBookingStatusUpdated);
            socket.off("notify_prepare_table", onNotifyPrepareTable); // [FIX] Tránh trùng lặp đăng ký
            socket.on("notify_prepare_table", onNotifyPrepareTable);
        }

        // Tạo vòng lặp kiểm tra cục bộ dự phòng trong trường hợp Socket IO mất kết nối
        runnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                
                io.socket.client.Socket currentSocket = SocketManager.getInstance().getSocket();
                if (currentSocket == null || !currentSocket.connected()) {
                    checkOverdueBookings();
                }
                
                // Lên lịch chạy lại sau CHECK_INTERVAL
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        handler.post(runnable);
    }

    /**
     * Dừng việc kiểm tra định kỳ để giải phóng tài nguyên.
     */
    public void stopChecking() {
        isRunning = false;
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
        io.socket.client.Socket socket = SocketManager.getInstance().getSocket();
        if (socket != null) {
            socket.off("booking_status_updated", onBookingStatusUpdated);
            // [FIX] Xóa socket.off("notify_prepare_table") ở đây để nó vẫn nhận thông báo khi onPause
        }
    }

    /**
     * Xóa sạch các Listener và giải phóng Socket (dùng khi Activity bị hủy).
     */
    public void destroy() {
        stopChecking();
        io.socket.client.Socket socket = SocketManager.getInstance().getSocket();
        if (socket != null) {
            socket.off("notify_prepare_table", onNotifyPrepareTable);
        }
    }

    /**
     * Truy vấn danh sách lịch hẹn đặt bàn từ server và lọc ra các lịch hẹn quá hạn 15 phút mà khách chưa nhận bàn.
     */
    private void checkOverdueBookings() {
        Context context = contextRef.get();
        if (context == null) return;
        
        int makh = SessionManager.isCustomer(context) ? SessionManager.getMaNV(context) : 0;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getBookings(makh).enqueue(new Callback<List<BookingResponse>>() {
            @Override
            public void onResponse(Call<List<BookingResponse>> call, Response<List<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Calendar now = Calendar.getInstance();

                    StringBuilder sb = new StringBuilder();
                    for (BookingResponse booking : response.body()) {
                        if ("pending".equalsIgnoreCase(booking.getTinhtrang())) {
                            try {
                                Date bookingDate = sdf.parse(booking.getThoigianhen());
                                if (bookingDate != null) {
                                    // Kiểm tra xem đã quá giờ hẹn chưa (Quá 15 phút)
                                    long diffInMillis = now.getTimeInMillis() - bookingDate.getTime();
                                    long diffInMinutes = diffInMillis / 60000;

                                    if (diffInMinutes >= 15) {
                                        if (sb.length() > 0) {
                                            sb.append(",");
                                        }
                                        sb.append(booking.getMaDatBan());

                                        // Hiển thị thông báo đẩy lên thiết bị
                                        showOverdueNotification(booking);
                                    }
                                }
                            } catch (ParseException ignored) {}
                        }
                    }
                    if (sb.length() > 0) {
                        // Gọi API cập nhật trạng thái hàng loạt sang "overdue"
                        updateBatchBookingStatus(sb.toString(), "overdue");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<BookingResponse>> call, Throwable t) {}
        });
    }

    /**
     * Gọi API cập nhật hàng loạt trạng thái của các lịch đặt bàn.
     */
    private void updateBatchBookingStatus(String madatbans, String status) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.batchUpdateBookingStatus(madatbans, status).enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {}
            @Override
            public void onFailure(Call<BookingResponse> call, Throwable t) {}
        });
    }

    /**
     * Hiển thị thông báo đẩy (System Notification) khi một bàn đặt bị quá hạn.
     */
    private void showOverdueNotification(BookingResponse booking) {
        Context context = contextRef.get();
        if (context == null) return;
        
        String title = "Cảnh báo quá giờ đặt bàn!";
        String content = "Lịch hẹn " + (booking.getTenBan() != null ? booking.getTenBan() : "Bàn #" + booking.getMaBan()) + " lúc " + booking.getThoigianhen() + " đã quá 15 phút mà khách chưa đến.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(booking.getMaDatBan(), builder.build());
        } catch (SecurityException ignored) {
            // Android 13+ requires post notifications permission, fail gracefully
        }
    }

    /**
     * Hiển thị cảnh báo thông qua AlertDialog và Notification khi có nhắc nhở chuẩn bị bàn từ Thu ngân.
     */
    private void showPrepareTableNotification(String tenBan) {
        Context context = contextRef.get();
        if (context == null) return;
        
        String title = "Chuẩn bị bàn đặt trước!";
        String content = "Thu ngân nhắc chuẩn bị " + tenBan + " cho khách đặt trước. Vui lòng kiểm tra!";

        // Hiển thị AlertDialog trực tiếp trên UI Thread
        try {
            new Handler(Looper.getMainLooper()).post(() -> {
                Context currentContext = contextRef.get();
                if (currentContext != null) {
                    try {
                        new androidx.appcompat.app.AlertDialog.Builder(currentContext)
                                .setTitle("🔔 Nhắc nhở từ Thu ngân")
                                .setMessage("Thu ngân nhắc chuẩn bị " + tenBan + " cho khách đặt trước!")
                                .setPositiveButton("OK", null)
                                .show();
                    } catch (Exception e) {
                        e.printStackTrace(); // Bắt lỗi bad token nếu context không phải là Activity
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            int notificationId = (tenBan != null) ? tenBan.hashCode() : (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Android 13+ permission fallback
        }
    }
}
