package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView img_payment_BackBtn;
    TextView txt_payment_TableName, txt_payment_OrderDate, txt_payment_TotalAmount;
    RecyclerView rv_payment_DishList;
    Button btn_payment_Pay;
    List<ThanhToanDTO> thanhToanDTOList;
    AdapterDisplayPayment adapterDisplayPayment;
    long tongtien = 0;
    int maban, madondat;
    String tenban, ngaydat;

    // Polling & Socket tools
    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private androidx.appcompat.app.AlertDialog waitingDialog;
    private boolean isPolling = false;

    private io.socket.client.Socket mSocket;
    private io.socket.emitter.Emitter.Listener onRefreshOrders = new io.socket.emitter.Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    checkApprovalStatus();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_layout);

        //region Bind views
        img_payment_BackBtn    = findViewById(R.id.img_payment_BackBtn);
        txt_payment_TableName  = findViewById(R.id.txt_payment_TableName);
        txt_payment_OrderDate  = findViewById(R.id.txt_payment_OrderDate);
        txt_payment_TotalAmount = findViewById(R.id.txt_payment_TotalAmount);
        rv_payment_DishList    = findViewById(R.id.rv_payment_DishList);
        rv_payment_DishList.setLayoutManager(new LinearLayoutManager(this));
        btn_payment_Pay        = findViewById(R.id.btn_payment_Pay);
        //endregion

        maban   = getIntent().getIntExtra("maban", 0);
        tenban  = getIntent().getStringExtra("tenban");
        ngaydat = getIntent().getStringExtra("ngaydat");
        madondat = getIntent().getIntExtra("madondat", 0);

        txt_payment_TableName.setText(tenban);
        txt_payment_OrderDate.setText(ngaydat);

        HienThiDSMonThanhToan();

        img_payment_BackBtn.setOnClickListener(this);
        btn_payment_Pay.setOnClickListener(this);
    }

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

    private void capNhatGiaoDien() {
        adapterDisplayPayment = new AdapterDisplayPayment(this, thanhToanDTOList);
        rv_payment_DishList.setAdapter(adapterDisplayPayment);

        txt_payment_TotalAmount.setText(
                String.format("%,d", tongtien) + " " +
                        getResources().getString(R.string.currency_vnd));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_payment_Pay) {
            hienThiDialogChonPhuongThuc();
        } else if (id == R.id.img_payment_BackBtn) {
            finish();
        }
    }

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

    private void hienThiDialogVietQR() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_vietqr, null);
        ImageView imgQR = dialogView.findViewById(R.id.img_dialogqr_QR);
        TextView txtBank = dialogView.findViewById(R.id.txt_dialogqr_Bank);
        TextView txtAccount = dialogView.findViewById(R.id.txt_dialogqr_Account);
        TextView txtAmount = dialogView.findViewById(R.id.txt_dialogqr_Amount);
        TextView txtMessage = dialogView.findViewById(R.id.txt_dialogqr_Message);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialogqr_Confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialogqr_Cancel);

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
            thucHienThanhToan("Chuyển khoản");
        });

        btnCancel.setOnClickListener(v -> {
            com.bumptech.glide.Glide.with(this).clear(imgQR);
            dialog.dismiss();
        });

        dialog.show();
    }

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
                    io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                    if (socket != null && socket.connected()) {
                        socket.emit("refresh_orders");
                    }
                    startPollingForApproval();
                } else {
                    Toast.makeText(PaymentActivity.this, "Lỗi gửi yêu cầu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(PaymentActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void startPollingForApproval() {
        waitingDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(this, "Đang chờ Thu ngân xác nhận...");
        waitingDialog.show();

        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (mSocket != null && mSocket.connected()) {
            mSocket.on("refresh_orders", onRefreshOrders);
            // Backup polling chạy mỗi 5s
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
            // Không có socket -> Polling 3s như cũ
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

    private void checkApprovalStatus() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.checkOrderStatus(madondat).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().getTinhTrang();
                    if ("true".equals(status)) { // Đã xác nhận
                        stopPolling();
                        HienThiHoaDon();
                    }
                }
            }
            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                // Ignore failure during polling
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }

    /**
     * Hiển thị dialog hóa đơn dạng AlertDialog sau khi thanh toán thành công.
     * Người dùng có thể chia sẻ hoặc đóng hóa đơn.
     */
    private void HienThiHoaDon() {
        View receiptView = LayoutInflater.from(this)
                .inflate(R.layout.receipt_layout, null);

        // Bind views trong receipt
        TextView txt_receipt_TableName = receiptView.findViewById(R.id.txt_receipt_TableName);
        TextView txt_receipt_Date      = receiptView.findViewById(R.id.txt_receipt_Date);
        TextView txt_receipt_Total     = receiptView.findViewById(R.id.txt_receipt_Total);
        LinearLayout layout_receipt_ItemList = receiptView.findViewById(R.id.layout_receipt_ItemList);
        Button btn_receipt_Share       = receiptView.findViewById(R.id.btn_receipt_Share);
        Button btn_receipt_Close       = receiptView.findViewById(R.id.btn_receipt_Close);

        // Điền thông tin
        txt_receipt_TableName.setText(
                getString(R.string.receipt_table) + tenban);
        txt_receipt_Date.setText(
                getString(R.string.receipt_date) + ngaydat);
        txt_receipt_Total.setText(
                String.format("%,d", tongtien) + " " +
                        getString(R.string.currency_vnd));

        // Thêm từng dòng món vào danh sách
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

        // Tạo AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(receiptView)
                .setCancelable(false)
                .create();

        // Nút chia sẻ
        btn_receipt_Share.setOnClickListener(v -> {
            // Render layout hóa đơn (ẩn nút trước khi chụp)
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

            Bitmap bitmap = ReceiptHelper.captureView(contentOnly);
            ReceiptHelper.shareBitmap(this, bitmap);

            // Hiện lại nút
            btn_receipt_Share.setVisibility(View.VISIBLE);
            btn_receipt_Close.setVisibility(View.VISIBLE);
        });

        // Nút đóng → về màn hình chính
        btn_receipt_Close.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this,
                    getString(R.string.payment_success_msg),
                    Toast.LENGTH_SHORT).show();
            finish();
        });

        dialog.show();
    }
}