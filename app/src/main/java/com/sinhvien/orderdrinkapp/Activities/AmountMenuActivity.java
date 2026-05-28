package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AmountMenuActivity extends AppCompatActivity {

    TextInputLayout txtl_amount_Quantity, txtl_amount_Note;
    Button btn_amount_Confirm;
    ImageView img_amount_Back;
    int maban, mamon, madondatCloud = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.amount_menu_layout);

        // Bind views
        txtl_amount_Quantity = findViewById(R.id.txtl_amount_Quantity);
        txtl_amount_Note = findViewById(R.id.txtl_amount_Note);
        btn_amount_Confirm = findViewById(R.id.btn_amount_Confirm);
        img_amount_Back = findViewById(R.id.img_amount_Back);

        img_amount_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Lấy thông tin từ bàn được chọn
        Intent intent = getIntent();
        maban = intent.getIntExtra("maban", 0);
        mamon = intent.getIntExtra("mamon", 0);

        // Lấy mã đơn hàng từ Cloud
        layMaDonHangTuCloud();

        btn_amount_Confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateAmount()) {
                    return;
                }

                if (madondatCloud == 0) {
                    Toast.makeText(AmountMenuActivity.this, "Đang khởi tạo đơn hàng, vui lòng thử lại...", Toast.LENGTH_SHORT).show();
                    return;
                }

                int sluong = Integer.parseInt(txtl_amount_Quantity.getEditText().getText().toString());
                
                androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(AmountMenuActivity.this, "Đang thêm món...");
                progressDialog.show();

                // Gửi món ăn lên Cloud
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                apiService.addOrderDetail(madondatCloud, mamon, sluong).enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                            Toast.makeText(AmountMenuActivity.this, "Đã gọi món lên Cloud!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AmountMenuActivity.this, "Lỗi lưu món ăn", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<OrderResponse> call, Throwable t) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(AmountMenuActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void layMaDonHangTuCloud() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getOrderByTable(maban).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    madondatCloud = response.body().getMaDonDat();
                } else {
                    // Nếu chưa có đơn, tạo đơn mới ngay
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

    private void taoDonHangMoi() {
        int manv = SessionManager.getMaNV(this);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.createOrder(manv, maban).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    madondatCloud = response.body().getMaDonDat();
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
}