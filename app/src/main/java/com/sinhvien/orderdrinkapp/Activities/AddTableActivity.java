package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputLayout;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.Api.TableResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Toast;
import android.util.Log;

/**
 * AddTableActivity - Màn hình Thêm / Chỉnh sửa bàn ăn trong nhà hàng.
 * Chức năng chính:
 * - Thu thập thông tin Tên bàn ăn/Số bàn ăn mới.
 * - Kiểm duyệt dữ liệu nhập vào (không được để trống).
 * - Kết nối HTTP API POST lên máy chủ VPS để cập nhật hoặc tạo bàn mới.
 * - Trả kết quả thành công RESULT_OK về cho Fragment để thực hiện tải lại giao diện danh sách bàn.
 */
public class AddTableActivity extends AppCompatActivity {

    private static final String TAG = "AddTableActivity";

    // Khai báo thành phần View
    TextInputLayout TXTL_addtable_TableName;
    Button BTN_addtable_CreateTable;
    ImageView IMG_addtable_back;
    int maban = 0; // ID của bàn ăn (nếu = 0 là chế độ Thêm mới)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addtable_layout);

        // Ánh xạ các thành phần View từ XML layout
        TXTL_addtable_TableName = (TextInputLayout)findViewById(R.id.txtl_addtable_TableName);
        BTN_addtable_CreateTable = (Button)findViewById(R.id.btn_addtable_CreateTable);
        IMG_addtable_back = (ImageView)findViewById(R.id.img_addtable_back);

        // Lấy thông tin mã bàn truyền qua Intent (Chế độ sửa bàn)
        maban = getIntent().getIntExtra("maban", 0);
        if (savedInstanceState != null) {
            maban = savedInstanceState.getInt("maban", maban);
        }
        if (maban != 0) {
            BTN_addtable_CreateTable.setText("Cập nhật bàn");
        }

        // Đăng ký sự kiện click cho nút Thêm/Cập nhật bàn
        BTN_addtable_CreateTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ViewUtils.isFastDoubleClick()) return; // Khóa click liên tục quá nhanh
                
                // Xác thực biểu mẫu nhập liệu
                if(!validateName()){
                    return;
                }
                
                String sTenBanAn = TXTL_addtable_TableName.getEditText().getText().toString();
                String action = (maban != 0) ? "edit" : "add";

                // Hiển thị vòng xoay đang tải
                androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(AddTableActivity.this, "Đang xử lý...");
                progressDialog.show();

                // Gửi dữ liệu yêu cầu quản lý bàn lên VPS Server
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                apiService.manageTable(action, maban, sTenBanAn).enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Quản lý bàn thành công: action=" + action + ", maban=" + maban + ", tenban=" + sTenBanAn);
                            setResult(RESULT_OK); // Trả kết quả thành công về cho Fragment hiển thị
                            finish();
                        }
                    }
 
                    @Override
                    public void onFailure(Call<OrderResponse> call, Throwable t) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        Log.e(TAG, "Lỗi kết nối API quản lý bàn: " + t.getMessage());
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(AddTableActivity.this, "Lỗi Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // Đăng ký sự kiện click cho nút quay lại
        IMG_addtable_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // Hiệu ứng trượt lùi
            }
        });
    }

    /**
     * Kiểm duyệt ô Tên bàn ăn (không để trống).
     */
    private boolean validateName(){
        String val = TXTL_addtable_TableName.getEditText().getText().toString().trim();
        if(val.isEmpty()){
            TXTL_addtable_TableName.setError(getResources().getString(R.string.not_empty));
            return false;
        }else {
            TXTL_addtable_TableName.setError(null);
            TXTL_addtable_TableName.setErrorEnabled(false);
            return true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu giữ ID bàn khi ứng dụng bị xoay ngang màn hình
        outState.putInt("maban", maban);
    }
}