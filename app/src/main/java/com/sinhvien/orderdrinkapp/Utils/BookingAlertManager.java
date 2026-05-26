package com.sinhvien.orderdrinkapp.Utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
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

public class BookingAlertManager {

    private static final String CHANNEL_ID = "booking_alerts";
    private static final String CHANNEL_NAME = "Nhắc nhở đặt bàn";
    private static final int CHECK_INTERVAL = 60000; // Quét mỗi 1 phút

    private Context context;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean isRunning = false;

    public BookingAlertManager(Context context) {
        this.context = context.getApplicationContext();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
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

    private io.socket.emitter.Emitter.Listener onBookingStatusUpdated = new io.socket.emitter.Emitter.Listener() {
        @Override
        public void call(Object... args) {
            checkOverdueBookings();
        }
    };

    public void startChecking() {
        if (isRunning) return;
        isRunning = true;

        io.socket.client.Socket socket = SocketManager.getInstance().getSocket();
        if (socket != null) {
            socket.on("booking_status_updated", onBookingStatusUpdated);
        }

        runnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                
                io.socket.client.Socket currentSocket = SocketManager.getInstance().getSocket();
                if (currentSocket == null || !currentSocket.connected()) {
                    checkOverdueBookings();
                }
                
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        handler.post(runnable);
    }

    public void stopChecking() {
        isRunning = false;
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
        io.socket.client.Socket socket = SocketManager.getInstance().getSocket();
        if (socket != null) {
            socket.off("booking_status_updated", onBookingStatusUpdated);
        }
    }

    private void checkOverdueBookings() {
        int makh = SessionManager.isCustomer(context) ? SessionManager.getMaNV(context) : 0;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getBookings(makh).enqueue(new Callback<List<BookingResponse>>() {
            @Override
            public void onResponse(Call<List<BookingResponse>> call, Response<List<BookingResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Calendar now = Calendar.getInstance();

                    for (BookingResponse booking : response.body()) {
                        if ("pending".equalsIgnoreCase(booking.getTinhtrang())) {
                            try {
                                Date bookingDate = sdf.parse(booking.getThoigianhen());
                                if (bookingDate != null) {
                                    // Kiểm tra xem đã quá giờ hẹn chưa (Quá 15 phút)
                                    long diffInMillis = now.getTimeInMillis() - bookingDate.getTime();
                                    long diffInMinutes = diffInMillis / 60000;

                                    if (diffInMinutes >= 15) {
                                        // 1. Cập nhật trạng thái thành overdue lên VPS
                                        updateBookingStatus(booking.getMaDatBan(), "overdue");

                                        // 2. Hiển thị thông báo đẩy lên điện thoại
                                        showOverdueNotification(booking);
                                    }
                                }
                            } catch (ParseException ignored) {}
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<BookingResponse>> call, Throwable t) {}
        });
    }

    private void updateBookingStatus(int madatban, String status) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.updateBookingStatus(madatban, status).enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {}
            @Override
            public void onFailure(Call<BookingResponse> call, Throwable t) {}
        });
    }

    private void showOverdueNotification(BookingResponse booking) {
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
}
