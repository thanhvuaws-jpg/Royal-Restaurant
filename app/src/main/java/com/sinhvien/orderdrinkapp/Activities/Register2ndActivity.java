package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.StaffResponse;

import java.util.Calendar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Register2ndActivity extends AppCompatActivity {

    RadioGroup RG_signup2nd_Gender;
    DatePicker DT_signup2nd_DOB;
    Button BTN_signup2nd_Complete;
    ImageView IMG_signup2nd_BackBtn;
    String hoTen,tenDN,eMail,sDT,matKhau,gioiTinh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register2nd_layout);

        //lấy đối tượng view
        RG_signup2nd_Gender = findViewById(R.id.rg_signup2nd_Gender);
        DT_signup2nd_DOB = findViewById(R.id.dt_signup2nd_DOB);
        BTN_signup2nd_Complete = findViewById(R.id.btn_signup2nd_Complete);
        IMG_signup2nd_BackBtn = findViewById(R.id.img_signup2nd_BackBtn);

        //lấy dữ liệu từ bundle của register1
        Bundle bundle = getIntent().getBundleExtra(RegisterActivity.BUNDLE);
        if(bundle != null) {
             hoTen = bundle.getString("hoten");
             tenDN = bundle.getString("tendn");
             eMail = bundle.getString("email");
             sDT = bundle.getString("sdt");
             matKhau = bundle.getString("matkhau");
        }

        BTN_signup2nd_Complete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!validateAge() | !validateGender()){
                    return;
                }

                //lấy các thông tin còn lại
                switch (RG_signup2nd_Gender.getCheckedRadioButtonId()){
                    case R.id.rd_signup2nd_Male:
                        gioiTinh = "Nam"; break;
                    case R.id.rd_signup2nd_Female:
                        gioiTinh = "Nữ"; break;
                    case R.id.rd_signup2nd_Other:
                        gioiTinh = "Khác"; break;
                }
                String ngaySinh = DT_signup2nd_DOB.getYear() + "-" + (DT_signup2nd_DOB.getMonth() + 1)
                        + "-" + DT_signup2nd_DOB.getDayOfMonth();

                android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(Register2ndActivity.this);
                progressDialog.setMessage("Đang xử lý...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                // Gửi dữ liệu đăng ký lên Cloud (maquyen = 4 đại diện cho Khách hàng thành viên)
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<StaffResponse> apiStaffResponseCall = apiService.addStaff(hoTen, tenDN, matKhau, eMail, sDT, gioiTinh, ngaySinh, 4);
                apiStaffResponseCall.enqueue(new Callback<StaffResponse>() {
                    @Override
                    public void onResponse(Call<StaffResponse> call, Response<StaffResponse> response) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                            Toast.makeText(Register2ndActivity.this, "Đăng ký thành công lên Cloud!", Toast.LENGTH_SHORT).show();
                            callLoginFromRegister();
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "Lỗi đăng ký";
                            Toast.makeText(Register2ndActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<StaffResponse> call, Throwable t) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(Register2ndActivity.this, "Lỗi kết nối Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        IMG_signup2nd_BackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
    }

    //Hàm chuyển màn hình khi hoàn thành
    public void callLoginFromRegister(){
        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    //region Validate field
    private boolean validateGender(){
        if(RG_signup2nd_Gender.getCheckedRadioButtonId() == -1){
            Toast.makeText(this,"Hãy chọn giới tính",Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }

    private boolean validateAge(){
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int userAge = DT_signup2nd_DOB.getYear();
        int isAgeValid = currentYear - userAge;

        if(isAgeValid < 10){
            Toast.makeText(this,"Bạn không đủ tuổi đăng ký!",Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }
    //endregion
}