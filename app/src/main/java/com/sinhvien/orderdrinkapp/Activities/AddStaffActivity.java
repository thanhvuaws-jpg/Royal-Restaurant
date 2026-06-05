package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.Api.StaffResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;

import java.util.Calendar;
import java.util.regex.Pattern;

public class AddStaffActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "AddStaffActivity";

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    //"(?=.*[@#$%^&+=])" +     // at least 1 special character
                    "(?=\\S+$)" +            // no white spaces
                    ".{6,}" +                // at least 4 characters
                    "$");

    ImageView img_add_StaffBack;
    TextView txt_add_StaffTitle;
    TextInputLayout txtl_add_StaffFullName, txtl_add_StaffUserName, txtl_add_StaffEmail, txtl_add_StaffPhone, txtl_add_StaffPassword;
    RadioGroup rg_add_StaffGender, rg_add_StaffRole;
    RadioButton rd_add_StaffMale, rd_add_StaffFemale, rd_add_StaffOther, rd_add_StaffAdmin, rd_add_StaffStandard, rd_add_StaffCashier;
    DatePicker dt_add_StaffDOB;
    Button btn_add_StaffCreate;
    String hoTen,tenDN,eMail,sDT,matKhau,gioiTinh,ngaySinh;
    int manv = 0,quyen = 0;
    long ktra = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addstaff_layout);

        //region Lấy đối tượng trong view
        txt_add_StaffTitle = (TextView)findViewById(R.id.txt_add_StaffTitle);
        img_add_StaffBack = (ImageView)findViewById(R.id.img_add_StaffBack);
        txtl_add_StaffFullName = (TextInputLayout)findViewById(R.id.txtl_add_StaffFullName);
        txtl_add_StaffUserName = (TextInputLayout)findViewById(R.id.txtl_add_StaffUserName);
        txtl_add_StaffEmail = (TextInputLayout)findViewById(R.id.txtl_add_StaffEmail);
        txtl_add_StaffPhone = (TextInputLayout)findViewById(R.id.txtl_add_StaffPhone);
        txtl_add_StaffPassword = (TextInputLayout)findViewById(R.id.txtl_add_StaffPassword);
        rg_add_StaffGender = (RadioGroup)findViewById(R.id.rg_add_StaffGender);
        rg_add_StaffRole = (RadioGroup)findViewById(R.id.rg_add_StaffRole);
        rd_add_StaffMale = (RadioButton)findViewById(R.id.rd_add_StaffMale);
        rd_add_StaffFemale = (RadioButton)findViewById(R.id.rd_add_StaffFemale);
        rd_add_StaffOther = (RadioButton)findViewById(R.id.rd_add_StaffOther);
        rd_add_StaffAdmin = (RadioButton)findViewById(R.id.rd_add_StaffAdmin);
        rd_add_StaffStandard = (RadioButton)findViewById(R.id.rd_add_StaffStandard);
        rd_add_StaffCashier = (RadioButton)findViewById(R.id.rd_add_StaffCashier);
        dt_add_StaffDOB = (DatePicker)findViewById(R.id.dt_add_StaffDOB);
        btn_add_StaffCreate = (Button)findViewById(R.id.btn_add_StaffCreate);
        //endregion

        //region Hiển thị trang sửa nếu được chọn từ context menu sửa
        // Lấy thông tin từ Cloud nếu là sửa
        manv = getIntent().getIntExtra("manv",0);
        if(manv != 0){
            txt_add_StaffTitle.setText(getResources().getString(R.string.edit_staff_title));
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.getStaffById(manv).enqueue(new Callback<StaffResponse>() {
                @Override
                public void onResponse(Call<StaffResponse> call, Response<StaffResponse> response) {
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        StaffResponse res = response.body();
                        Log.d(TAG, "Tải thông tin nhân viên thành công: manv=" + manv + ", hoten=" + res.getHoTenNV());
                        txtl_add_StaffFullName.getEditText().setText(res.getHoTenNV());
                        txtl_add_StaffUserName.getEditText().setText(res.getTenDN());
                        txtl_add_StaffEmail.getEditText().setText(res.getEmail());
                        txtl_add_StaffPhone.getEditText().setText(res.getSdt());
                        txtl_add_StaffPassword.getEditText().setText(res.getMatKhau());

                        if("Nam".equals(res.getGioiTinh())) rd_add_StaffMale.setChecked(true);
                        else if("Nữ".equals(res.getGioiTinh())) rd_add_StaffFemale.setChecked(true);
                        else rd_add_StaffOther.setChecked(true);

                        if(res.getMaQuyen() == 1) rd_add_StaffAdmin.setChecked(true);
                        else if (res.getMaQuyen() == 3) rd_add_StaffCashier.setChecked(true);
                        else rd_add_StaffStandard.setChecked(true);

                        String date = res.getNgaySinh();
                        if(date != null && date.contains("-")){ // Cloud format yyyy-MM-dd
                            String[] items = date.split("-");
                            dt_add_StaffDOB.updateDate(Integer.parseInt(items[0]), Integer.parseInt(items[1])-1, Integer.parseInt(items[2]));
                        }
                        btn_add_StaffCreate.setText(getResources().getString(R.string.edit_staff_btn));
                    }
                }
                @Override
                public void onFailure(Call<StaffResponse> call, Throwable t) {
                    Log.e(TAG, "Lỗi tải thông tin nhân viên: " + t.getMessage());
                }
            });
        }
        //endregion

        btn_add_StaffCreate.setOnClickListener(this);
        img_add_StaffBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        String chucnang;
        switch (id){
            case R.id.btn_add_StaffCreate:
                if (ViewUtils.isFastDoubleClick()) return; // Chống double click
                if( !validateAge() | !validateEmail() | !validateFullName() | !validateGender() | !validatePassWord() |
                !validatePermission() | !validatePhone() | !validateUserName()){
                    return;
                }
                //Lấy dữ liệu từ view
                hoTen = txtl_add_StaffFullName.getEditText().getText().toString();
                tenDN = txtl_add_StaffUserName.getEditText().getText().toString();
                eMail = txtl_add_StaffEmail.getEditText().getText().toString();
                sDT = txtl_add_StaffPhone.getEditText().getText().toString();
                matKhau = txtl_add_StaffPassword.getEditText().getText().toString();

                switch (rg_add_StaffGender.getCheckedRadioButtonId()){
                    case R.id.rd_add_StaffMale: gioiTinh = "Nam"; break;
                    case R.id.rd_add_StaffFemale: gioiTinh = "Nữ"; break;
                    case R.id.rd_add_StaffOther: gioiTinh = "Khác"; break;
                }
                switch (rg_add_StaffRole.getCheckedRadioButtonId()){
                    case R.id.rd_add_StaffAdmin: quyen = 1; break;
                    case R.id.rd_add_StaffStandard: quyen = 2; break;
                    case R.id.rd_add_StaffCashier: quyen = 3; break;
                }

                // Định dạng ngày chuẩn MySQL: yyyy-MM-dd
                ngaySinh = dt_add_StaffDOB.getYear() + "-" + (dt_add_StaffDOB.getMonth() + 1)
                        + "-" + dt_add_StaffDOB.getDayOfMonth();

                //truyền dữ liệu vào obj nhanvienDTO
                NhanVienDTO nhanVienDTO = new NhanVienDTO();
                nhanVienDTO.setHOTENNV(hoTen);
                nhanVienDTO.setTENDN(tenDN);
                nhanVienDTO.setEMAIL(eMail);
                nhanVienDTO.setSDT(sDT);
                nhanVienDTO.setMATKHAU(matKhau);
                nhanVienDTO.setGIOITINH(gioiTinh);
                nhanVienDTO.setNGAYSINH(ngaySinh);
                nhanVienDTO.setMAQUYEN(quyen);

                // Cloud logic cho cả Thêm và Sửa
                String actionStaff = (manv != 0) ? "edit" : "add";

                androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(AddStaffActivity.this, "Đang xử lý...");
                progressDialog.show();

                ApiService apiServiceStaff = ApiClient.getClient().create(ApiService.class);
                apiServiceStaff.manageStaff(actionStaff, manv, hoTen, tenDN, matKhau, eMail, sDT, gioiTinh, ngaySinh, quyen).enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Quản lý nhân viên thành công: action=" + actionStaff + ", manv=" + manv + ", hoten=" + hoTen);
                            Intent intent = new Intent();
                            intent.putExtra("ketquaktra", (long)1);
                            intent.putExtra("chucnang", (manv != 0) ? "sua" : "themnv");
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }
 
                    @Override
                    public void onFailure(Call<OrderResponse> call, Throwable t) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        Log.e(TAG, "Lỗi kết nối API quản lý nhân viên: " + t.getMessage());
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(AddStaffActivity.this, "Lỗi Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;

            case R.id.img_add_StaffBack:
                finish();
                overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
                break;
        }
    }

    //region validate fields
    private boolean validateFullName(){
        String val = txtl_add_StaffFullName.getEditText().getText().toString().trim();

        if(val.isEmpty()){
            txtl_add_StaffFullName.setError(getResources().getString(R.string.not_empty));
            return false;
        }else {
            txtl_add_StaffFullName.setError(null);
            txtl_add_StaffFullName.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateUserName(){
        String val = txtl_add_StaffUserName.getEditText().getText().toString().trim();
        String checkspaces = "\\A\\w{1,50}\\z";

        if(val.isEmpty()){
            txtl_add_StaffUserName.setError(getResources().getString(R.string.not_empty));
            return false;
        }else if(val.length()>50){
            txtl_add_StaffUserName.setError("Phải nhỏ hơn 50 ký tự");
            return false;
        }else if(!val.matches(checkspaces)){
            txtl_add_StaffUserName.setError("Không được cách chữ!");
            return false;
        }
        else {
            txtl_add_StaffUserName.setError(null);
            txtl_add_StaffUserName.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateEmail(){
        String val = txtl_add_StaffEmail.getEditText().getText().toString().trim();
        String checkspaces = "[a-zA-Z0-9._-]+@[a-z]+.+[a-z]+";

        if(val.isEmpty()){
            txtl_add_StaffEmail.setError(getResources().getString(R.string.not_empty));
            return false;
        }else if(!val.matches(checkspaces)){
            txtl_add_StaffEmail.setError("Email không hợp lệ!");
            return false;
        }
        else {
            txtl_add_StaffEmail.setError(null);
            txtl_add_StaffEmail.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePhone(){
        String val = txtl_add_StaffPhone.getEditText().getText().toString().trim();


        if(val.isEmpty()){
            txtl_add_StaffPhone.setError(getResources().getString(R.string.not_empty));
            return false;
        }else if(val.length() != 10){
            txtl_add_StaffPhone.setError("Số điện thoại không hợp lệ!");
            return false;
        }
        else {
            txtl_add_StaffPhone.setError(null);
            txtl_add_StaffPhone.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePassWord(){
        String val = txtl_add_StaffPassword.getEditText().getText().toString().trim();

        if(val.isEmpty()){
            txtl_add_StaffPassword.setError(getResources().getString(R.string.not_empty));
            return false;
        }else if(!PASSWORD_PATTERN.matcher(val).matches()){
            txtl_add_StaffPassword.setError("Mật khẩu ít nhất 6 ký tự!");
            return false;
        }
        else {
            txtl_add_StaffPassword.setError(null);
            txtl_add_StaffPassword.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateGender(){
        if(rg_add_StaffGender.getCheckedRadioButtonId() == -1){
            Toast.makeText(this,"Hãy chọn giới tính",Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }

    private boolean validatePermission(){
        if(rg_add_StaffRole.getCheckedRadioButtonId() == -1){
            Toast.makeText(this,"Hãy chọn quyền",Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }

    private boolean validateAge(){
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int userAge = dt_add_StaffDOB.getYear();
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