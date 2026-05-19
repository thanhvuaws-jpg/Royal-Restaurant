package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CashierConfirmActivity extends AppCompatActivity {

    ImageView img_cashier_BackBtn;
    TextView txt_cashier_TableName, txt_cashier_StaffName, txt_cashier_OrderDate, txt_cashier_TotalAmount, txt_cashier_ProposedMethod;
    RecyclerView rv_cashier_DishList;
    Button btn_cashier_Cash, btn_cashier_Bank;

    int madon, manv, maban;
    String ngaydat, tongtien, tenNv, tenBan, phuongthuc;
    List<ThanhToanDTO> thanhToanDTOList;
    AdapterDisplayPayment adapterDisplayPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cashier_confirm_layout);

        // Bind Views
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

        // Get Data
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
                long total = (long) Double.parseDouble(tongtien);
                txt_cashier_TotalAmount.setText(String.format("%,d", total) + " VNĐ");
            } catch (Exception e) {
                txt_cashier_TotalAmount.setText(tongtien + " VNĐ");
            }

            loadOrderDetails();
        }

        img_cashier_BackBtn.setOnClickListener(v -> finish());

        btn_cashier_Cash.setOnClickListener(v -> confirmPayment("Tiền mặt"));
        btn_cashier_Bank.setOnClickListener(v -> confirmPayment("Chuyển khoản"));
    }

    private void loadOrderDetails() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getOrderDetails(madon).enqueue(new Callback<List<OrderDetailResponse>>() {
            @Override
            public void onResponse(Call<List<OrderDetailResponse>> call, Response<List<OrderDetailResponse>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    thanhToanDTOList = new ArrayList<>();
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

    private void confirmPayment(String method) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Đang xác nhận...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.confirmPayment(madon, method).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    Toast.makeText(CashierConfirmActivity.this, "Xác nhận thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Quay lại trang danh sách chờ
                } else {
                    Toast.makeText(CashierConfirmActivity.this, "Lỗi xác nhận", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(CashierConfirmActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
