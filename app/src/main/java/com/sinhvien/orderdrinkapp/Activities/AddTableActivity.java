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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Toast;

public class AddTableActivity extends AppCompatActivity {

    TextInputLayout TXTL_addtable_TableName;
    Button BTN_addtable_CreateTable;
    ImageView IMG_addtable_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addtable_layout);

        //region Lấy đối tượng trong view
        TXTL_addtable_TableName = (TextInputLayout)findViewById(R.id.txtl_addtable_TableName);
        BTN_addtable_CreateTable = (Button)findViewById(R.id.btn_addtable_CreateTable);
        IMG_addtable_back = (ImageView)findViewById(R.id.img_addtable_back);

        final int maban = getIntent().getIntExtra("maban", 0);
        if (maban != 0) {
            BTN_addtable_CreateTable.setText("Cập nhật bàn");
        }

        BTN_addtable_CreateTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!validateName()){
                    return;
                }
                String sTenBanAn = TXTL_addtable_TableName.getEditText().getText().toString();
                String action = (maban != 0) ? "edit" : "add";

                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                apiService.manageTable(action, maban, sTenBanAn).enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful()) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<OrderResponse> call, Throwable t) {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(AddTableActivity.this, "Lỗi Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        IMG_addtable_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
            }
        });
    }

    //validate dữ liệu
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
}