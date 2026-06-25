package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputLayout;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AmountMenuActivity - Màn hình chọn Số lượng và Ghi chú khi gọi món cho một bàn cụ thể.
 * Chức năng chính:
 * - Khi click vào món ăn trong danh sách, màn hình này xuất hiện để chọn số lượng món cần đặt.
 * - Tự động liên hệ API Server để tìm Đơn đặt bàn hiện tại (OrderByTable). Nếu chưa có đơn hàng, hệ thống tự động khởi tạo đơn mới (createOrder).
 * - Lưu chi tiết món ăn (mã món, số lượng) vào cơ sở dữ liệu cloud qua API (addOrderDetail).
 */
public class AmountMenuActivity extends AppCompatActivity {

    private static final String TAG = "AmountMenuActivity";

    // Khai báo View
    TextInputLayout txtl_amount_Quantity, txtl_amount_Note;
    Button btn_amount_Confirm;
    ImageView img_amount_Back;
    
    int maban, mamon; // ID bàn và ID món ăn
    int madondatCloud = 0; // ID đơn đặt hàng lưu trên Cloud

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.amount_menu_layout);

        // Ánh xạ View
        txtl_amount_Quantity = findViewById(R.id.txtl_amount_Quantity);
        txtl_amount_Note = findViewById(R.id.txtl_amount_Note);
        btn_amount_Confirm = findViewById(R.id.btn_amount_Confirm);
        img_amount_Back = findViewById(R.id.img_amount_Back);

        // Lắng nghe sự kiện click nút Back
        img_amount_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Nhận dữ liệu bàn và món ăn được chọn từ Intent
        Intent intent = getIntent();
        maban = intent.getIntExtra("maban", 0);
        mamon = intent.getIntExtra("mamon", 0);

        // Khôi phục mã đơn đặt hàng nếu có cấu hình xoay màn hình
        if (savedInstanceState != null) {
            madondatCloud = savedInstanceState.getInt("madondat_cloud", 0);
        }
        if (madondatCloud == 0) {
            layMaDonHangTuCloud(); // Lấy mã đơn hàng từ Cloud tương ứng với bàn hiện tại
        }

        // Lắng nghe sự kiện click nút Xác nhận số lượng
        btn_amount_Confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ViewUtils.isFastDoubleClick()) return; // Khóa nhấn double click quá nhanh
                
                // Xác thực số lượng nhập vào
                if (!validateAmount()) {
                    return;
                }

                if (madondatCloud == 0) {
                    Toast.makeText(AmountMenuActivity.this, "Đang khởi tạo đơn hàng, vui lòng thử lại...", Toast.LENGTH_SHORT).show();
                    return;
                }

                int sluong = Integer.parseInt(txtl_amount_Quantity.getEditText().getText().toString());
                
                // Hiển thị tiến trình loading
                androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(AmountMenuActivity.this, "Đang thêm món...");
                progressDialog.show();

                // Gửi thông tin chi tiết món ăn vừa gọi lên Cloud
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                apiService.addOrderDetail(madondatCloud, mamon, sluong).enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                            Log.d(TAG, "Thêm món thành công: mamon=" + mamon + ", soluong=" + sluong + ", madon=" + madondatCloud);
                            Toast.makeText(AmountMenuActivity.this, "Đã gọi món lên Cloud!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AmountMenuActivity.this, "Lỗi lưu món ăn", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<OrderResponse> call, Throwable t) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        Log.e(TAG, "Lỗi thêm món vào đơn: " + t.getMessage());
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(AmountMenuActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    /**
     * Gửi yêu cầu lên server VPS lấy thông tin Đơn đặt hàng hiện tại đang phục vụ cho bàn ăn.
     */
    private void layMaDonHangTuCloud() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getOrderByTable(maban).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    madondatCloud = response.body().getMaDonDat(); // Nhận mã đơn hiện tại
                } else {
                    // Nếu bàn chưa được mở đơn, tạo mới một đơn hàng tức thì
                    taoDonHangMoi();
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(AmountMenuActivity.this, "Lỗi lấy mã đơn: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Khởi tạo một đơn hàng mới gắn với nhân viên phục vụ hiện tại và bàn ăn được chọn.
     */
    private void taoDonHangMoi() {
        int manv = SessionManager.getMaNV(this);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.createOrder(manv, maban).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    madondatCloud = response.body().getMaDonDat(); // Nhận ID đơn mới tạo
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(AmountMenuActivity.this, "Lỗi tạo đơn mới", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Kiểm duyệt định dạng số lượng món gọi (Không rỗng, là ký số hợp lệ).
     */
    private boolean validateAmount() {
        if (txtl_amount_Quantity.getEditText() == null) return false;
        String val = txtl_amount_Quantity.getEditText().getText().toString().trim();
        if (val.isEmpty()) {
            txtl_amount_Quantity.setError(getString(R.string.not_empty));
            return false;
        } else if (!val.matches("\\d+(?:\\.\\d+)?")) {
            txtl_amount_Quantity.setError("Số lượng không hợp lệ");
            return false;
        } else {
            txtl_amount_Quantity.setError(null);
            txtl_amount_Quantity.setErrorEnabled(false);
            return true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu trữ mã đơn đặt hàng để tránh bị mất khi Activity thay đổi vòng đời (config changes)
        outState.putInt("madondat_cloud", madondatCloud);
    }
}