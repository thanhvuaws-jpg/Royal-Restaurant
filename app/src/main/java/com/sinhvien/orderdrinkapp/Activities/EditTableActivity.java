package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.material.textfield.TextInputLayout;
import com.sinhvien.orderdrinkapp.R;

public class EditTableActivity extends AppCompatActivity {

    TextInputLayout txtl_edit_TableName;
    Button btn_edit_TableUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edittable_layout);

        //thuộc tính view
        txtl_edit_TableName = (TextInputLayout)findViewById(R.id.txtl_edit_TableName);
        btn_edit_TableUpdate = (Button)findViewById(R.id.btn_edit_TableUpdate);

        int maban = getIntent().getIntExtra("maban",0); //lấy maban từ bàn đc chọn

        btn_edit_TableUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tenban = txtl_edit_TableName.getEditText().getText().toString().trim();

                if(!tenban.isEmpty()){
                    // boolean ktra = banAnDAO.CapNhatTenBan(maban,tenban); // Disabled for Cloud
                    Intent intent = new Intent();
                    intent.putExtra("ketquasua", true); // Dummy success
                    setResult(RESULT_OK,intent);
                    finish();
                } else {
                    txtl_edit_TableName.setError(getResources().getString(R.string.not_empty));
                }
            }
        });
    }
}