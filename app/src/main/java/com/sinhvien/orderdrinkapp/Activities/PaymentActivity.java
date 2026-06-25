package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayPayment;
import com.sinhvien.orderdrinkapp.DTO.ThanhToanDTO;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderDetailResponse;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.ReceiptHelper;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PaymentActivity - Màn hình thanh toán hóa đơn tạm tính của Bàn ăn (Dành cho nhân viên phục vụ / Khách hàng).
 * Chức năng chính:
 * - Hiển thị danh sách món ăn, số lượng và tổng số tiền của đơn hàng thuộc bàn ăn được chọn.
 * - Cung cấp 2 hình thức thanh toán chính:
 *   + Tiền mặt: Gửi yêu cầu thanh toán trực tiếp lên máy thu ngân.
 *   + Chuyển khoản (VietQR): Tạo mã VietQR động theo tiêu chuẩn VietQR.io (chứa số tiền, nội dung thanh toán và thông tin ngân hàng thụ hưởng cấu hình tại ApiClient), hiển thị hình ảnh QR bằng Glide.
 * - Đồng bộ thời gian thực bằng Socket.io (kênh refresh_orders): Khi phát sinh thanh toán, phát tín hiệu lên server để thông báo cho máy thu ngân. Đồng thời lắng nghe phản hồi của Thu ngân duyệt đơn để kết thúc quá trình.
 * - Cơ chế Polling dự phòng (3s/5s): Chủ động gọi API checkOrderStatus liên tục để kiểm tra trạng thái thanh toán đã được thu ngân duyệt thành công chưa.
 * - Xuất và chia sẻ hóa đơn (HienThiHoaDon): Tạo ảnh chụp bitmap biên lai hóa đơn để chia sẻ hoặc lưu trữ dưới dạng ảnh.
 */
public class PaymentActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "PaymentActivity";

    // Khai báo các đối tượng giao diện XML
    ImageView img_payment_BackBtn;
    TextView txt_payment_TableName, txt_payment_OrderDate, txt_payment_TotalAmount;
    RecyclerView rv_payment_DishList;
    Button btn_payment_Pay;
    List<ThanhToanDTO> thanhToanDTOList;
    AdapterDisplayPayment adapterDisplayPayment;
    long tongtien = 0;
    int maban, madondat;
    String tenban, ngaydat;

    // Các biến phục vụ việc Polling & Socket đồng bộ
    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private androidx.appcompat.app.AlertDialog waitingDialog; // Dialog hiển thị chờ thu ngân duyệt
    private boolean isPolling = false;
    private boolean isReceiptShowing = false;
    private boolean shouldShowReceipt = false;
    private android.os.Parcelable savedLayoutState;

    private io.socket.client.Socket mSocket;
    private io.socket.emitter.Emitter.Listener onRefreshOrders = new io.socket.emitter.Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    checkApprovalStatus(); // Kiểm tra trạng thái hóa đơn khi nhận được tin nhắn socket
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_layout);

        // Ánh xạ các View XML
        img_payment_BackBtn    = findViewById(R.id.img_payment_BackBtn);
        txt_payment_TableName  = findViewById(R.id.txt_payment_TableName);
        txt_payment_OrderDate  = findViewById(R.id.txt_payment_OrderDate);
        txt_payment_TotalAmount = findViewById(R.id.txt_payment_TotalAmount);
        rv_payment_DishList    = findViewById(R.id.rv_payment_DishList);
        rv_payment_DishList.setLayoutManager(new LinearLayoutManager(this));
        btn_payment_Pay        = findViewById(R.id.btn_payment_Pay);

        // Khôi phục trạng thái cũ (nếu có) khi quay màn hình
        boolean wasPolling = false;
        if (savedInstanceState != null) {
            savedLayoutState = savedInstanceState.getParcelable("list_state");
            wasPolling = savedInstanceState.getBoolean("is_polling", false);
            shouldShowReceipt = savedInstanceState.getBoolean("is_receipt_showing", false);
        }

        // Nhận dữ liệu truyền sang từ màn hình chính
        maban   = getIntent().getIntExtra("maban", 0);
        tenban  = getIntent().getStringExtra("tenban");
        ngaydat = getIntent().getStringExtra("ngaydat");
        madondat = getIntent().getIntExtra("madondat", 0);

        txt_payment_TableName.setText(tenban);
        txt_payment_OrderDate.setText(ngaydat);

        // Hiển thị danh sách các món ăn cần thanh toán
        HienThiDSMonThanhToan();

        if (wasPolling) {
            startPollingForApproval();
        }

        img_payment_BackBtn.setOnClickListener(this);
        btn_payment_Pay.setOnClickListener(this);
    }

    /**
     * Tải thông tin chi tiết các món ăn thuộc đơn hàng từ Server API.
     */
    private void HienThiDSMonThanhToan() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getOrderDetails(madondat).enqueue(new Callback<List<OrderDetailResponse>>() {
            @Override
            public void onResponse(Call<List<OrderDetailResponse>> call, Response<List<OrderDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    thanhToanDTOList = new ArrayList<>();
                    tongtien = 0;
                    for (OrderDetailResponse res : response.body()) {
                        ThanhToanDTO tt = new ThanhToanDTO();
                        tt.setTenMon(res.getTenMon());
                        tt.setGiaTien((int) res.getGiaTien());
                        tt.setSoLuong(res.getSoLuong());
                        tt.setHinhAnhPath(res.getHinhAnh());
                        thanhToanDTOList.add(tt);
                        tongtien += ((long) res.getSoLuong() * res.getGiaTien());
                    }
                    capNhatGiaoDien();
                }
            }

            @Override
            public void onFailure(Call<List<OrderDetailResponse>> call, Throwable t) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(PaymentActivity.this, "Lỗi lấy chi tiết đơn: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Đồng bộ nạp danh sách món ăn lên RecyclerView và tính tổng số tiền.
     */
    private void capNhatGiaoDien() {
        adapterDisplayPayment = new AdapterDisplayPayment(this, thanhToanDTOList);
        rv_payment_DishList.setAdapter(adapterDisplayPayment);

        txt_payment_TotalAmount.setText(
                String.format("%,d", tongtien) + " " +
                        getResources().getString(R.string.currency_vnd));

        if (savedLayoutState != null && rv_payment_DishList.getLayoutManager() != null) {
            rv_payment_DishList.getLayoutManager().onRestoreInstanceState(savedLayoutState);
            savedLayoutState = null;
        }

        if (shouldShowReceipt) {
            shouldShowReceipt = false;
            HienThiHoaDon();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_payment_Pay) {
            if (ViewUtils.isFastDoubleClick()) return; // Khóa double click
            hienThiDialogChonPhuongThuc();
        } else if (id == R.id.img_payment_BackBtn) {
            finish();
        }
    }

    /**
     * Mở Dialog lựa chọn phương thức thanh toán.
     */
    private void hienThiDialogChonPhuongThuc() {
        String[] options = {"Tiền mặt", "Chuyển khoản (VietQR)"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn phương thức thanh toán")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        thucHienThanhToan("Tiền mặt");
                    } else {
                        hienThiDialogVietQR();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Mở Dialog chứa mã QR chuyển khoản động (sử dụng VietQR API).
     */
    private void hienThiDialogVietQR() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_vietqr, null);
        ImageView imgQR = dialogView.findViewById(R.id.img_dialogqr_QR);
        TextView txtBank = dialogView.findViewById(R.id.txt_dialogqr_Bank);
        TextView txtAccount = dialogView.findViewById(R.id.txt_dialogqr_Account);
        TextView txtAmount = dialogView.findViewById(R.id.txt_dialogqr_Amount);
        TextView txtMessage = dialogView.findViewById(R.id.txt_dialogqr_Message);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialogqr_Confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialogqr_Cancel);

        // Tạo nội dung chuyển khoản động
        String message = "Thanh toan Ban " + tenban + " Don " + madondat;
        String qrUrl = "";
        try {
            String encodedMsg = java.net.URLEncoder.encode(message, "UTF-8");
            String encodedName = java.net.URLEncoder.encode(ApiClient.BANK_NAME, "UTF-8");
            qrUrl = "https://img.vietqr.io/image/" + ApiClient.BANK_ID + "-" + ApiClient.BANK_ACC + "-compact2.png"
                    + "?amount=" + tongtien
                    + "&addInfo=" + encodedMsg
                    + "&accountName=" + encodedName;
        } catch (Exception e) {
            qrUrl = "https://img.vietqr.io/image/" + ApiClient.BANK_ID + "-" + ApiClient.BANK_ACC + "-compact2.png"
                    + "?amount=" + tongtien
                    + "&addInfo=" + message;
        }

        txtBank.setText(ApiClient.BANK_ID);
        txtAccount.setText(ApiClient.BANK_ACC);
        txtAmount.setText(String.format("%,d", tongtien) + " VNĐ");
        txtMessage.setText(message);

        // Nạp ảnh QR trực tuyến bằng Glide
        com.bumptech.glide.Glide.with(this)
                .load(qrUrl)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(imgQR);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnConfirm.setOnClickListener(v -> {
            com.bumptech.glide.Glide.with(this).clear(imgQR);
            dialog.dismiss();
            thucHienThanhToan("Chuyển khoản"); // Gửi yêu cầu chuyển khoản lên thu ngân duyệt
        });

        btnCancel.setOnClickListener(v -> {
            com.bumptech.glide.Glide.with(this).clear(imgQR);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Gọi API yêu cầu thanh toán (checkoutOrder) gửi lên phía thu ngân phê duyệt.
     */
    private void thucHienThanhToan(String phuongthuc) {
        androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(this, "Đang gửi yêu cầu thanh toán...");
        progressDialog.show();

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.checkoutOrder(madondat, tongtien, phuongthuc).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    Log.d(TAG, "Gửi yêu cầu thanh toán thành công: madon=" + madondat + ", phuongthuc=" + phuongthuc);
                    
                    // Phát sự kiện Socket real-time thông báo Thu Ngân
                    io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                    if (socket != null && socket.connected()) {
                        socket.emit("refresh_orders");
                    }
                    startPollingForApproval(); // Chuyển sang chế độ chờ thu ngân duyệt đơn
                } else {
                    Toast.makeText(PaymentActivity.this, "Lỗi gửi yêu cầu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                Log.e(TAG, "Lỗi gửi yêu cầu thanh toán: " + t.getMessage());
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(PaymentActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Bật cơ chế lắng nghe thu ngân xác nhận duyệt đơn.
     * Sử dụng kết hợp Socket.io và cơ chế Polling (gọi lại định kỳ) để đảm bảo không bị mất gói tin.
     */
    private void startPollingForApproval() {
        waitingDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(this, "Đang chờ Thu ngân xác nhận...");
        waitingDialog.show();

        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (mSocket != null && mSocket.connected()) {
            mSocket.on("refresh_orders", onRefreshOrders);
            // Polling dự phòng chạy mỗi 5s
            isPolling = true;
            pollingRunnable = new Runnable() {
                @Override
                public void run() {
                    checkApprovalStatus();
                    if (isPolling) {
                        pollingHandler.postDelayed(this, 5000);
                    }
                }
            };
            pollingHandler.postDelayed(pollingRunnable, 5000);
        } else {
            // Không có kết nối Socket -> Chạy Polling định kỳ mỗi 3s
            isPolling = true;
            pollingRunnable = new Runnable() {
                @Override
                public void run() {
                    checkApprovalStatus();
                    if (isPolling) {
                        pollingHandler.postDelayed(this, 3000);
                    }
                }
            };
            pollingHandler.post(pollingRunnable);
        }
    }

    /**
     * Dừng lắng nghe duyệt đơn và đóng các hộp thoại chờ.
     */
    private void stopPolling() {
        isPolling = false;
        if (pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
        if (mSocket != null) {
            mSocket.off("refresh_orders", onRefreshOrders);
        }
        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
        }
    }

    /**
     * Gọi API checkOrderStatus để kiểm tra xem đơn hàng đã được Thu ngân chuyển trạng thái sang đã thanh toán (tinhTrang = true) chưa.
     */
    private void checkApprovalStatus() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.checkOrderStatus(madondat).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().getTinhTrang();
                    if ("true".equals(status)) { // Đã được thu ngân xác nhận duyệt
                        stopPolling();
                        HienThiHoaDon(); // Xuất hóa đơn cho khách
                    }
                }
            }
            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                // Bỏ qua lỗi trong lúc polling chờ
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu giữ trạng thái màn hình
        outState.putBoolean("is_polling", isPolling);
        outState.putBoolean("is_receipt_showing", isReceiptShowing);
        if (rv_payment_DishList != null && rv_payment_DishList.getLayoutManager() != null) {
            outState.putParcelable("list_state", rv_payment_DishList.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }

    /**
     * Hiển thị biên lai Hóa đơn (Receipt Layout) và hỗ trợ chia sẻ thông qua ReceiptHelper.
     */
    private void HienThiHoaDon() {
        isReceiptShowing = true;
        View receiptView = LayoutInflater.from(this)
                .inflate(R.layout.receipt_layout, null);

        // Ánh xạ các View con trong hóa đơn
        TextView txt_receipt_TableName = receiptView.findViewById(R.id.txt_receipt_TableName);
        TextView txt_receipt_Date      = receiptView.findViewById(R.id.txt_receipt_Date);
        TextView txt_receipt_Total     = receiptView.findViewById(R.id.txt_receipt_Total);
        LinearLayout layout_receipt_ItemList = receiptView.findViewById(R.id.layout_receipt_ItemList);
        Button btn_receipt_Share       = receiptView.findViewById(R.id.btn_receipt_Share);
        Button btn_receipt_Close       = receiptView.findViewById(R.id.btn_receipt_Close);

        // Gán thông tin hóa đơn
        txt_receipt_TableName.setText(
                getString(R.string.receipt_table) + tenban);
        txt_receipt_Date.setText(
                getString(R.string.receipt_date) + ngaydat);
        txt_receipt_Total.setText(
                String.format("%,d", tongtien) + " " +
                        getString(R.string.currency_vnd));

        // Nạp động danh sách món ăn vào Layout hóa đơn
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ThanhToanDTO item : thanhToanDTOList) {
            View rowView = inflater.inflate(
                    R.layout.custom_layout_receipt_item, layout_receipt_ItemList, false);

            ((TextView) rowView.findViewById(R.id.txt_receiptItem_Name))
                    .setText(item.getTenMon());
            ((TextView) rowView.findViewById(R.id.txt_receiptItem_Quantity))
                    .setText("x" + item.getSoLuong());
            long subtotal = (long) item.getSoLuong() * item.getGiaTien();
            ((TextView) rowView.findViewById(R.id.txt_receiptItem_Subtotal))
                    .setText(String.format("%,d", subtotal) + "đ");

            layout_receipt_ItemList.addView(rowView);
        }

        // Tạo và mở hộp thoại AlertDialog hiển thị biên lai
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(receiptView)
                .setCancelable(false)
                .create();

        // Nút chia sẻ hóa đơn (Chụp màn hình layout và chia sẻ)
        btn_receipt_Share.setOnClickListener(v -> {
            // Tạm thời ẩn các nút bấm để bức ảnh chụp biên lai sạch đẹp hơn
            btn_receipt_Share.setVisibility(View.GONE);
            btn_receipt_Close.setVisibility(View.GONE);

            View contentOnly = receiptView.findViewById(R.id.layout_receipt_Content);
            contentOnly.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            getResources().getDisplayMetrics().widthPixels,
                            View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED));
            contentOnly.layout(0, 0,
                    contentOnly.getMeasuredWidth(),
                    contentOnly.getMeasuredHeight());

            // Chụp view thành ảnh Bitmap và gọi Intent chia sẻ hệ thống
            Bitmap bitmap = ReceiptHelper.captureView(contentOnly);
            ReceiptHelper.shareBitmap(this, bitmap);

            // Hiện lại các nút sau khi chụp xong
            btn_receipt_Share.setVisibility(View.VISIBLE);
            btn_receipt_Close.setVisibility(View.VISIBLE);
        });

        // Nút đóng hộp thoại -> hoàn thành và đóng Activity thanh toán
        btn_receipt_Close.setOnClickListener(v -> {
            isReceiptShowing = false;
            dialog.dismiss();
            Toast.makeText(this,
                    getString(R.string.payment_success_msg),
                    Toast.LENGTH_SHORT).show();
            finish();
        });

        dialog.show();
    }
}