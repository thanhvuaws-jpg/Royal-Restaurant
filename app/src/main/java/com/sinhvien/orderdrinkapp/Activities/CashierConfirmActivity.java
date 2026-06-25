package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderDetailResponse;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayPayment;
import com.sinhvien.orderdrinkapp.DTO.ThanhToanDTO;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CashierConfirmActivity - Màn hình Xác nhận Thanh toán dành cho Thu ngân (Cashier).
 * Chức năng chính:
 * - Hiển thị chi tiết hóa đơn cần thanh toán của bàn bao gồm: Tên bàn, Nhân viên lập hóa đơn, Ngày đặt, Tổng tiền, và danh sách các món ăn đã dùng.
 * - Cho phép Thu ngân lựa chọn phương thức thanh toán thực tế (Tiền mặt hoặc Chuyển khoản).
 * - Kết nối HTTP API (confirmPayment) gửi yêu cầu xác nhận kết thúc đơn hàng lên VPS.
 * - Phát tín hiệu qua Socket.io (booking_status_updated, refresh_orders) thông báo cập nhật trạng thái bàn real-time cho các thiết bị khác trong hệ thống.
 */
public class CashierConfirmActivity extends AppCompatActivity {

    private static final String TAG = "CashierConfirmActivity";

    // Khai báo các thành phần UI hiển thị
    ImageView img_cashier_BackBtn;
    TextView txt_cashier_TableName, txt_cashier_StaffName, txt_cashier_OrderDate, txt_cashier_TotalAmount, txt_cashier_ProposedMethod;
    RecyclerView rv_cashier_DishList;
    Button btn_cashier_Cash, btn_cashier_Bank;

    // Các biến lưu trữ thông tin đơn hàng nhận được từ màn hình trước
    int madon, manv, maban;
    String ngaydat, tongtien, tenNv, tenBan, phuongthuc;
    List<ThanhToanDTO> thanhToanDTOList;
    AdapterDisplayPayment adapterDisplayPayment;
    private android.os.Parcelable savedLayoutState; // Lưu trạng thái cuộn của RecyclerView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cashier_confirm_layout);

        // Ánh xạ các View giao diện
        img_cashier_BackBtn = findViewById(R.id.img_cashier_BackBtn);
        txt_cashier_TableName = findViewById(R.id.txt_cashier_TableName);
        txt_cashier_StaffName = findViewById(R.id.txt_cashier_StaffName);
        txt_cashier_OrderDate = findViewById(R.id.txt_cashier_OrderDate);
        txt_cashier_TotalAmount = findViewById(R.id.txt_cashier_TotalAmount);
        txt_cashier_ProposedMethod = findViewById(R.id.txt_cashier_ProposedMethod);
        rv_cashier_DishList = findViewById(R.id.rv_cashier_DishList);
        rv_cashier_DishList.setLayoutManager(new LinearLayoutManager(this));
        btn_cashier_Cash = findViewById(R.id.btn_cashier_Cash);
        btn_cashier_Bank = findViewById(R.id.btn_cashier_Bank);

        // Khôi phục lại trạng thái RecyclerView khi xoay màn hình
        if (savedInstanceState != null) {
            savedLayoutState = savedInstanceState.getParcelable("list_state");
        }

        // Đọc dữ liệu từ Intent gửi tới
        Intent intent = getIntent();
        madon = intent.getIntExtra("madon", 0);
        manv = intent.getIntExtra("manv", 0);
        maban = intent.getIntExtra("maban", 0);
        ngaydat = intent.getStringExtra("ngaydat");
        tongtien = intent.getStringExtra("tongtien");
        tenNv = intent.getStringExtra("tennv");
        tenBan = intent.getStringExtra("tenban");
        phuongthuc = intent.getStringExtra("phuongthuc");

        if (madon != 0) {
            txt_cashier_OrderDate.setText(ngaydat);
            txt_cashier_TableName.setText(tenBan != null ? tenBan : "Bàn " + maban);
            txt_cashier_StaffName.setText("Nhân viên: " + (tenNv != null ? tenNv : "ID " + manv));
            txt_cashier_ProposedMethod.setText(phuongthuc != null && !phuongthuc.isEmpty() ? phuongthuc : "Không rõ");
            
            try {
                // Định dạng tiền tệ hiển thị ngăn cách phần nghìn
                long total = (long) Double.parseDouble(tongtien);
                txt_cashier_TotalAmount.setText(String.format("%,d", total) + " VNĐ");
            } catch (Exception e) {
                txt_cashier_TotalAmount.setText(tongtien + " VNĐ");
            }

            // Gọi hàm tải danh sách chi tiết các món ăn trong hóa đơn
            loadOrderDetails();
        }

        // Quay lại màn hình trước
        img_cashier_BackBtn.setOnClickListener(v -> finish());

        // Xử lý sự kiện xác nhận Thanh toán tiền mặt
        btn_cashier_Cash.setOnClickListener(v -> {
            if (ViewUtils.isFastDoubleClick()) return; // Khóa spam click chuột
            confirmPayment("Tiền mặt");
        });
        
        // Xử lý sự kiện xác nhận Thanh toán chuyển khoản ngân hàng
        btn_cashier_Bank.setOnClickListener(v -> {
            if (ViewUtils.isFastDoubleClick()) return; // Khóa spam click chuột
            confirmPayment("Chuyển khoản");
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu giữ trạng thái cuộn của danh sách món ăn
        if (rv_cashier_DishList != null && rv_cashier_DishList.getLayoutManager() != null) {
            outState.putParcelable("list_state", rv_cashier_DishList.getLayoutManager().onSaveInstanceState());
        }
    }

    /**
     * Tải chi tiết các món ăn thuộc đơn đặt hàng thông qua REST API getOrderDetails.
     */
    private void loadOrderDetails() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getOrderDetails(madon).enqueue(new Callback<List<OrderDetailResponse>>() {
            @Override
            public void onResponse(Call<List<OrderDetailResponse>> call, Response<List<OrderDetailResponse>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    thanhToanDTOList = new ArrayList<>();
                    // Duyệt danh sách nhận từ Server và gán vào list hiển thị
                    for (OrderDetailResponse res : response.body()) {
                        ThanhToanDTO dto = new ThanhToanDTO();
                        dto.setTenMon(res.getTenMon());
                        dto.setSoLuong(res.getSoLuong());
                        dto.setGiaTien((int)res.getGiaTien());
                        dto.setHinhAnhPath(res.getHinhAnh());
                        thanhToanDTOList.add(dto);
                    }
                    adapterDisplayPayment = new AdapterDisplayPayment(CashierConfirmActivity.this, thanhToanDTOList);
                    rv_cashier_DishList.setAdapter(adapterDisplayPayment);

                    // Phục hồi lại vị trí cuộn cũ nếu có
                    if (savedLayoutState != null && rv_cashier_DishList.getLayoutManager() != null) {
                        rv_cashier_DishList.getLayoutManager().onRestoreInstanceState(savedLayoutState);
                        savedLayoutState = null;
                    }
                }
            }

            @Override
            public void onFailure(Call<List<OrderDetailResponse>> call, Throwable t) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(CashierConfirmActivity.this, "Lỗi tải chi tiết: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Gửi yêu cầu xác thực thanh toán thành công lên VPS qua API confirmPayment với phương thức thanh toán tương ứng.
     */
    private void confirmPayment(String method) {
        androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(this, "Đang xử lý xác nhận...");
        progressDialog.show();

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.confirmPayment(madon, method).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    Log.d(TAG, "Xác nhận thanh toán thành công: madon=" + madon + ", method=" + method);
                    Toast.makeText(CashierConfirmActivity.this, "Xác nhận thành công!", Toast.LENGTH_SHORT).show();
                    
                    // Phát tín hiệu Socket real-time đồng bộ cập nhật giao diện trạng thái bàn ở các máy phục vụ
                    io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                    if (socket != null && socket.connected()) {
                        socket.emit("booking_status_updated");
                        socket.emit("refresh_orders");
                    }

                    finish(); // Quay lại trang danh sách chờ thanh toán
                } else {
                    Toast.makeText(CashierConfirmActivity.this, "Lỗi xác nhận", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                Log.e(TAG, "Lỗi xác nhận thanh toán: " + t.getMessage());
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(CashierConfirmActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

