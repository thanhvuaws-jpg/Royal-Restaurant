package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayPayment;
import com.sinhvien.orderdrinkapp.DTO.ThanhToanDTO;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderDetailResponse;
import com.sinhvien.orderdrinkapp.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class DetailStatisticActivity extends AppCompatActivity {

    ImageView img_detail_BackBtn;
    TextView txt_detail_OrderId, txt_detail_OrderDate, txt_detail_TableName, txt_detail_StaffName, txt_detail_TotalAmount;
    RecyclerView rvDetailStatistic;
    int madon, manv, maban;
    String ngaydat, tongtien, tenNv, tenBan;
    List<ThanhToanDTO> thanhToanDTOList;
    AdapterDisplayPayment adapterDisplayPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailstatistic_layout);

        //Lấy thông tin từ display statistic
        Intent intent = getIntent();
        madon = intent.getIntExtra("madon",0);
        manv = intent.getIntExtra("manv",0);
        maban = intent.getIntExtra("maban",0);
        ngaydat = intent.getStringExtra("ngaydat");
        tongtien = intent.getStringExtra("tongtien");
        tenNv = intent.getStringExtra("tennv");
        tenBan = intent.getStringExtra("tenban");

        //region Thuộc tính bên view
        img_detail_BackBtn = (ImageView)findViewById(R.id.img_detail_BackBtn);
        txt_detail_OrderId = (TextView)findViewById(R.id.txt_detail_OrderId);
        txt_detail_OrderDate = (TextView)findViewById(R.id.txt_detail_OrderDate);
        txt_detail_TableName = (TextView)findViewById(R.id.txt_detail_TableName);
        txt_detail_StaffName = (TextView)findViewById(R.id.txt_detail_StaffName);
        txt_detail_TotalAmount = (TextView)findViewById(R.id.txt_detail_TotalAmount);
        rvDetailStatistic = findViewById(R.id.rvDetailStatistic);
        rvDetailStatistic.setLayoutManager(new LinearLayoutManager(this));
        //endregion

        //chỉ hiển thị nếu lấy đc mã đơn đc chọn
        if (madon !=0){
            txt_detail_OrderId.setText(getResources().getString(R.string.order_id_prefix) + "#" + madon);
            txt_detail_OrderDate.setText(ngaydat);
            txt_detail_TotalAmount.setText(tongtien + " " + getResources().getString(R.string.currency_vnd));

            if (tenNv != null) {
                txt_detail_StaffName.setText(tenNv);
            } else {
                txt_detail_StaffName.setText("ID: " + manv);
            }
            if (tenBan != null) {
                txt_detail_TableName.setText(tenBan);
            } else {
                txt_detail_TableName.setText("Bàn " + maban);
            }

            HienThiDSCTDD();
        }

        img_detail_BackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
            }
        });
    }

    private void HienThiDSCTDD(){
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getOrderDetails(madon).enqueue(new Callback<List<OrderDetailResponse>>() {
            @Override
            public void onResponse(Call<List<OrderDetailResponse>> call, Response<List<OrderDetailResponse>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    thanhToanDTOList = new java.util.ArrayList<>();
                    for (OrderDetailResponse res : response.body()) {
                        ThanhToanDTO dto = new ThanhToanDTO();
                        dto.setTenMon(res.getTenMon());
                        dto.setSoLuong(res.getSoLuong());
                        dto.setGiaTien((int)res.getGiaTien());
                        dto.setHinhAnhPath(res.getHinhAnh());
                        thanhToanDTOList.add(dto);
                    }
                    adapterDisplayPayment = new AdapterDisplayPayment(DetailStatisticActivity.this, thanhToanDTOList);
                    rvDetailStatistic.setAdapter(adapterDisplayPayment);
                }
            }

            @Override
            public void onFailure(Call<List<OrderDetailResponse>> call, Throwable t) {
                if (!isFinishing() && !isDestroyed()) {
                    android.widget.Toast.makeText(DetailStatisticActivity.this, "Lỗi tải chi tiết: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}