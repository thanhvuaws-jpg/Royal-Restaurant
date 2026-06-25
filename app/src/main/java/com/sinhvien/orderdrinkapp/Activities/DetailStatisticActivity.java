package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

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

/**
 * DetailStatisticActivity - Màn hình hiển thị Chi tiết Hóa đơn Thống kê (Dành cho Quản trị viên).
 * Chức năng chính:
 * - Hiển thị các thông tin cơ bản của một hóa đơn cũ đã thanh toán thành công bao gồm: Mã đơn, Ngày đặt, Bàn ăn, Nhân viên thanh toán, Tổng tiền hóa đơn.
 * - Gọi HTTP API getOrderDetails để tải chi tiết các món ăn, số lượng và đơn giá của các món ăn thuộc hóa đơn đó.
 * - Sử dụng RecyclerView gắn AdapterDisplayPayment để hiển thị trực quan danh sách món ăn đã thanh toán.
 */
public class DetailStatisticActivity extends AppCompatActivity {

    // Khai báo các View thành phần giao diện
    ImageView img_detail_BackBtn;
    TextView txt_detail_OrderId, txt_detail_OrderDate, txt_detail_TableName, txt_detail_StaffName, txt_detail_TotalAmount;
    RecyclerView rvDetailStatistic;
    
    // Các biến nhận dữ liệu truyền sang
    int madon, manv, maban;
    String ngaydat, tongtien, tenNv, tenBan;
    List<ThanhToanDTO> thanhToanDTOList;
    AdapterDisplayPayment adapterDisplayPayment;
    private Call<List<OrderDetailResponse>> orderDetailsCall; // Lưu giữ cuộc gọi API để hủy nếu Activity bị đóng
    private android.os.Parcelable savedLayoutState; // Lưu trạng thái cuộn của danh sách món ăn

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy cuộc gọi API để tránh rò rỉ bộ nhớ khi Activity bị phá hủy trước khi nhận được dữ liệu
        if (orderDetailsCall != null) {
            orderDetailsCall.cancel();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailstatistic_layout);

        // Nhận dữ liệu hóa đơn truyền từ màn hình thống kê doanh thu trước đó
        Intent intent = getIntent();
        madon = intent.getIntExtra("madon", 0);
        manv = intent.getIntExtra("manv", 0);
        maban = intent.getIntExtra("maban", 0);
        ngaydat = intent.getStringExtra("ngaydat");
        tongtien = intent.getStringExtra("tongtien");
        tenNv = intent.getStringExtra("tennv");
        tenBan = intent.getStringExtra("tenban");

        // Ánh xạ các View XML
        img_detail_BackBtn = (ImageView)findViewById(R.id.img_detail_BackBtn);
        txt_detail_OrderId = (TextView)findViewById(R.id.txt_detail_OrderId);
        txt_detail_OrderDate = (TextView)findViewById(R.id.txt_detail_OrderDate);
        txt_detail_TableName = (TextView)findViewById(R.id.txt_detail_TableName);
        txt_detail_StaffName = (TextView)findViewById(R.id.txt_detail_StaffName);
        txt_detail_TotalAmount = (TextView)findViewById(R.id.txt_detail_TotalAmount);
        rvDetailStatistic = findViewById(R.id.rvDetailStatistic);
        rvDetailStatistic.setLayoutManager(new LinearLayoutManager(this));

        // Khôi phục vị trí cuộn khi Activity cấu hình lại (Ví dụ: xoay màn hình)
        if (savedInstanceState != null) {
            savedLayoutState = savedInstanceState.getParcelable("list_state");
        }

        // Kiểm tra mã đơn hợp lệ để hiển thị thông tin
        if (madon != 0){
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

            // Gọi hàm tải chi tiết danh sách món ăn từ Server Cloud
            HienThiDSCTDD();
        }

        // Đóng màn hình quay lại trang trước
        img_detail_BackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // Hiệu ứng trượt ngang
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu trữ vị trí danh sách món ăn
        if (rvDetailStatistic != null && rvDetailStatistic.getLayoutManager() != null) {
            outState.putParcelable("list_state", rvDetailStatistic.getLayoutManager().onSaveInstanceState());
        }
    }

    /**
     * Tải danh sách chi tiết các món ăn của hóa đơn (API getOrderDetails).
     */
    private void HienThiDSCTDD(){
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        orderDetailsCall = apiService.getOrderDetails(madon);
        orderDetailsCall.enqueue(new Callback<List<OrderDetailResponse>>() {
            @Override
            public void onResponse(Call<List<OrderDetailResponse>> call, Response<List<OrderDetailResponse>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    thanhToanDTOList = new java.util.ArrayList<>();
                    // Duyệt danh sách các dòng chi tiết hóa đơn trả về và thiết lập vào DTO thanh toán
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

                    // Phục hồi lại vị trí cuộn đã lưu
                    if (savedLayoutState != null && rvDetailStatistic.getLayoutManager() != null) {
                        rvDetailStatistic.getLayoutManager().onRestoreInstanceState(savedLayoutState);
                        savedLayoutState = null;
                    }
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