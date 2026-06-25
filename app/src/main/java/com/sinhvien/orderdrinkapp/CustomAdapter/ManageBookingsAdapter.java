package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;

/**
 * ManageBookingsAdapter - Adapter xử lý và hiển thị danh sách yêu cầu đặt bàn của khách hàng từ góc nhìn Quản lý/Nhân viên.
 * - Hỗ trợ phân tích trạng thái lịch đặt để đổi màu văn bản và ẩn/hiện động các nút thao tác nghiệp vụ:
 *   + pending (Chờ duyệt - Màu cam): Hiện nút "Xác nhận duyệt" (btn_checkin_booking) và "Hủy đặt" (btn_cancel_booking).
 *   + confirmed (Đã xác nhận - Màu xanh dương): Hiện nút "Khách đã đến nhận bàn" (btn_customer_arrived) và "Hủy đặt".
 *   + checked_in (Đã nhận bàn - Màu xanh lá): Ẩn toàn bộ thanh công cụ thao tác vì quy trình đã chuyển sang phục vụ tại bàn.
 *   + completed (Đã hoàn thành - Màu xanh dương): Ẩn toàn bộ thanh thao tác.
 *   + overdue (Quá giờ hẹn - Màu đỏ): Hiện nút "Xác nhận duyệt" (dành cho trường hợp châm chước) và "Hủy đặt".
 *   + cancelled (Đã hủy - Màu xám): Ẩn toàn bộ thanh thao tác.
 * - Tránh lỗi nhấn đúp (Double-click spam) bằng cách sử dụng tiện ích ViewUtils.isFastDoubleClick().
 * - Đồng bộ thời gian thực: Khi nhân viên thực hiện thao tác (Duyệt, Nhận bàn, Hủy), adapter tự động gọi API tương ứng,
 *   phát tín hiệu Socket.io ("booking_status_updated", "refresh_orders") cập nhật tức thì đến toàn hệ thống và lưu cache SharedPreferences nội bộ.
 */
public class ManageBookingsAdapter extends RecyclerView.Adapter<ManageBookingsAdapter.ViewHolder> {

    private static final String TAG = "ManageBookingsAdapter";

    private final Context context;
    private final List<BookingResponse> bookingList;
    private final OnBookingActionListener actionListener;

    // Interface callback thông báo cập nhật dữ liệu thành công cho Fragment chủ quản
    public interface OnBookingActionListener {
        void onActionSuccess();
    }

    public ManageBookingsAdapter(Context context, List<BookingResponse> bookingList, OnBookingActionListener actionListener) {
        this.context = context;
        this.bookingList = bookingList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingResponse booking = bookingList.get(position);

        holder.txt_booking_table.setText(booking.getTenBan() != null ? booking.getTenBan() : "Bàn #" + booking.getMaBan());
        holder.txt_booking_customer.setText("Mã Khách hàng: #" + booking.getMaKH());
        holder.txt_booking_time.setText("Giờ hẹn: " + booking.getThoigianhen());

        // Hiển thị số tiền đặt trước món ăn nếu có
        if (booking.getTongTien() != null && !booking.getTongTien().isEmpty() && !"0".equals(booking.getTongTien())) {
            holder.txt_booking_dishes.setText("Món đặt trước: " + booking.getTongTien() + " đ");
        } else {
            holder.txt_booking_dishes.setText("Món đặt trước: Không có");
        }

        // Định dạng trạng thái và cập nhật hiển thị giao diện tương tác động
        String status = booking.getTinhtrang();
        if ("pending".equalsIgnoreCase(status)) {
            holder.txt_booking_status.setText("Chờ nhận bàn");
            holder.txt_booking_status.setTextColor(Color.parseColor("#FFAB40")); 
            holder.layout_booking_actions.setVisibility(View.VISIBLE);
            holder.btn_checkin_booking.setVisibility(View.VISIBLE);
            holder.btn_cancel_booking.setVisibility(View.VISIBLE);
            holder.btn_customer_arrived.setVisibility(View.GONE);
        } else if ("confirmed".equalsIgnoreCase(status)) {
            holder.txt_booking_status.setText("Đã xác nhận");
            holder.txt_booking_status.setTextColor(Color.parseColor("#1E88E5")); 
            holder.layout_booking_actions.setVisibility(View.VISIBLE);
            holder.btn_checkin_booking.setVisibility(View.GONE);
            holder.btn_cancel_booking.setVisibility(View.VISIBLE);
            holder.btn_customer_arrived.setVisibility(View.VISIBLE);
        } else if ("checked_in".equalsIgnoreCase(status)) {
            holder.txt_booking_status.setText("Đã nhận bàn");
            holder.txt_booking_status.setTextColor(Color.parseColor("#43A047")); 
            holder.layout_booking_actions.setVisibility(View.GONE);
        } else if ("completed".equalsIgnoreCase(status)) {
            holder.txt_booking_status.setText("Đã hoàn thành");
            holder.txt_booking_status.setTextColor(Color.parseColor("#1E88E5")); 
            holder.layout_booking_actions.setVisibility(View.GONE);
        } else if ("overdue".equalsIgnoreCase(status)) {
            holder.txt_booking_status.setText("Quá giờ hẹn");
            holder.txt_booking_status.setTextColor(Color.parseColor("#E53935")); 
            holder.layout_booking_actions.setVisibility(View.VISIBLE);
            holder.btn_checkin_booking.setVisibility(View.VISIBLE);
            holder.btn_cancel_booking.setVisibility(View.VISIBLE);
            holder.btn_customer_arrived.setVisibility(View.GONE);
        } else if ("cancelled".equalsIgnoreCase(status)) {
            holder.txt_booking_status.setText("Đã hủy");
            holder.txt_booking_status.setTextColor(Color.parseColor("#9E9E9E")); 
            holder.layout_booking_actions.setVisibility(View.GONE);
        } else {
            holder.txt_booking_status.setText(status);
            holder.txt_booking_status.setTextColor(Color.parseColor("#9E9E9E"));
            holder.layout_booking_actions.setVisibility(View.GONE);
        }

        // Bắt sự kiện click các nút (có chống đúp click)
        holder.btn_checkin_booking.setOnClickListener(v -> {
            if (ViewUtils.isFastDoubleClick()) return; 
            performConfirmBooking(booking.getMaDatBan());
        });
        holder.btn_customer_arrived.setOnClickListener(v -> {
            if (ViewUtils.isFastDoubleClick()) return; 
            performCheckIn(booking);
        });
        holder.btn_cancel_booking.setOnClickListener(v -> {
            if (ViewUtils.isFastDoubleClick()) return; 
            promptCancelBooking(booking.getMaDatBan());
        });
    }

    /**
     * Xác nhận duyệt đơn đặt bàn của khách từ trạng thái Chờ duyệt -> Xác nhận.
     */
    private void performConfirmBooking(int madatban) {
        androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(context, "Đang xác nhận đặt bàn...");
        progressDialog.show();

        int manv_staff = SessionManager.getMaNV(context);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.confirmBooking(madatban, manv_staff).enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    Log.d(TAG, "Xác nhận đặt bàn thành công: madatban=" + madatban);
                    Toast.makeText(context, "Đã xác nhận đặt bàn!", Toast.LENGTH_SHORT).show();
                    
                    // Gửi Socket cập nhật real-time
                    io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                    if (socket != null && socket.connected()) {
                        socket.emit("booking_status_updated");
                        socket.emit("refresh_orders");
                    }

                    if (actionListener != null) {
                        actionListener.onActionSuccess();
                    }
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Lỗi xác nhận đặt bàn";
                    Log.w(TAG, "Xác nhận đặt bàn thất bại: " + msg);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BookingResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Lỗi kết nối API xác nhận đặt bàn: " + t.getMessage());
                Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Xác nhận khách đã đến trực tiếp nhà hàng và mở khóa bàn phục vụ (Check-in).
     * Hệ thống sẽ tự động chuyển trạng thái bàn sang "true" (Đang dùng) và tạo hóa đơn (Order).
     */
    private void performCheckIn(BookingResponse booking) {
        int madatban = booking.getMaDatBan();
        androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(context, "Đang xử lý nhận bàn...");
        progressDialog.show();

        int manv_staff = SessionManager.getMaNV(context);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.checkinBooking(madatban, manv_staff).enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    Log.d(TAG, "Nhận bàn thành công: madatban=" + madatban);
                    Toast.makeText(context, "Nhận bàn thành công! Hóa đơn đã được tạo.", Toast.LENGTH_SHORT).show();
                    
                    // Phát sự kiện Socket real-time
                    io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                    if (socket != null && socket.connected()) {
                        socket.emit("booking_status_updated");
                        socket.emit("refresh_orders");
                    }

                    // Lưu trạng thái vào SharedPreferences cục bộ nhằm tránh trùng thông báo Toast
                    try {
                        android.content.SharedPreferences prefs = context.getSharedPreferences("nv_booking_cache", android.content.Context.MODE_PRIVATE);
                        prefs.edit().putString("booking_" + booking.getMaDatBan(), "checked_in").apply();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (actionListener != null) {
                        actionListener.onActionSuccess();
                    }
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Lỗi nhận bàn";
                    Log.w(TAG, "Nhận bàn thất bại: " + msg);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BookingResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Lỗi kết nối API nhận bàn: " + t.getMessage());
                Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Hiển thị AlertDialog yêu cầu xác nhận hủy lượt đặt bàn và gọi API hủy.
     */
    private void promptCancelBooking(int madatban) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận hủy đặt")
                .setMessage("Bạn có chắc chắn muốn hủy lịch hẹn đặt bàn này?")
                .setPositiveButton("Hủy lịch", (dialog, which) -> {
                    androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(context, "Đang hủy lịch đặt...");
                    progressDialog.show();

                    ApiService apiService = ApiClient.getClient().create(ApiService.class);
                    apiService.updateBookingStatus(madatban, "cancelled").enqueue(new Callback<BookingResponse>() {
                        @Override
                        public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {
                            progressDialog.dismiss();
                            if (response.isSuccessful()) {
                                Log.d(TAG, "Hủy lịch đặt bàn thành công: madatban=" + madatban);
                                Toast.makeText(context, "Đã hủy lịch hẹn đặt bàn!", Toast.LENGTH_SHORT).show();
                                
                                // Phát Socket
                                io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                                if (socket != null && socket.connected()) {
                                    socket.emit("booking_status_updated");
                                    socket.emit("refresh_orders");
                                }

                                if (actionListener != null) {
                                    actionListener.onActionSuccess();
                                }
                            } else {
                                Log.w(TAG, "Không thể hủy lịch đặt bàn: madatban=" + madatban);
                                Toast.makeText(context, "Không thể hủy lịch đặt!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<BookingResponse> call, Throwable t) {
                            progressDialog.dismiss();
                            Log.e(TAG, "Lỗi kết nối API hủy lịch đặt bàn: " + t.getMessage());
                            Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    /**
     * ViewHolder chứa cấu trúc hiển thị 1 thẻ quản lý đặt bàn.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt_booking_table, txt_booking_status, txt_booking_customer, txt_booking_time, txt_booking_dishes;
        MaterialButton btn_checkin_booking, btn_cancel_booking, btn_customer_arrived;
        LinearLayout layout_booking_actions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txt_booking_table = itemView.findViewById(R.id.txt_booking_table);
            txt_booking_status = itemView.findViewById(R.id.txt_booking_status);
            txt_booking_customer = itemView.findViewById(R.id.txt_booking_customer);
            txt_booking_time = itemView.findViewById(R.id.txt_booking_time);
            txt_booking_dishes = itemView.findViewById(R.id.txt_booking_dishes);
            btn_checkin_booking = itemView.findViewById(R.id.btn_checkin_booking);
            btn_cancel_booking = itemView.findViewById(R.id.btn_cancel_booking);
            btn_customer_arrived = itemView.findViewById(R.id.btn_customer_arrived);
            layout_booking_actions = itemView.findViewById(R.id.layout_booking_actions);
        }
    }
}
