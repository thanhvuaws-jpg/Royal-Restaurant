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

/**
 * AddStaffActivity - Màn hình Thêm mới / Cập nhật thông tin nhân viên (chỉ dành cho Quản trị viên - Admin).
 * Chức năng chính:
 * - Thu thập thông tin cá nhân của nhân viên: Họ tên, Tên đăng nhập, Email, Số điện thoại, Mật khẩu, Giới tính, Ngày sinh, Quyền hạn.
 * - Cho phép kiểm định đầu vào (Hợp lệ Email, Mật khẩu tối thiểu 6 ký tự không dấu cách, Số điện thoại đủ 10 số).
 * - Lưu trữ hoặc sửa thông tin nhân viên thông qua kết nối API RESTful (manageStaff) gửi dữ liệu lên Server VPS.
 * - Trả kết quả cập nhật về màn hình trước đó để tải lại danh sách nhân viên mới nhất.
 */
public class AddStaffActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "AddStaffActivity";

    // Regex kiểm tra mật khẩu hợp lệ: Không chứa khoảng trắng và có độ dài tối thiểu 6 ký tự
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=\\S+$)" +            // Không chứa ký tự khoảng trắng
                    ".{6,}" +                // Độ dài tối thiểu là 6 ký tự
                    "$");

    // Khai báo các thành phần UI
    ImageView img_add_StaffBack;
    TextView txt_add_StaffTitle;
    TextInputLayout txtl_add_StaffFullName, txtl_add_StaffUserName, txtl_add_StaffEmail, txtl_add_StaffPhone, txtl_add_StaffPassword;
    RadioGroup rg_add_StaffGender, rg_add_StaffRole;
    RadioButton rd_add_StaffMale, rd_add_StaffFemale, rd_add_StaffOther, rd_add_StaffAdmin, rd_add_StaffStandard, rd_add_StaffCashier;
    DatePicker dt_add_StaffDOB;
    Button btn_add_StaffCreate;
    
    // Các biến lưu trữ tạm thời thông tin biểu mẫu
    String hoTen, tenDN, eMail, sDT, matKhau, gioiTinh, ngaySinh;
    int manv = 0; // ID nhân viên cần chỉnh sửa (nếu = 0 là chế độ Thêm mới)
    int quyen = 0; // Quyền hạn (1: Admin, 2: Phục vụ, 3: Thu ngân)
    long ktra = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addstaff_layout);

        // Ánh xạ View từ file layout XML
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

        // Nhận ID nhân viên truyền sang để thực hiện chức năng chỉnh sửa thông tin (nếu có)
        manv = getIntent().getIntExtra("manv", 0);
        if(manv != 0){
            txt_add_StaffTitle.setText(getResources().getString(R.string.edit_staff_title));
            btn_add_StaffCreate.setText(getResources().getString(R.string.edit_staff_btn));
            
            // Tải thông tin chi tiết nhân viên từ server về thiết bị
            if (savedInstanceState == null) {
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

                            // Thiết lập Radio Button giới tính
                            if("Nam".equals(res.getGioiTinh())) rd_add_StaffMale.setChecked(true);
                            else if("Nữ".equals(res.getGioiTinh())) rd_add_StaffFemale.setChecked(true);
                            else rd_add_StaffOther.setChecked(true);

                            // Thiết lập Radio Button chức vụ/quyền
                            if(res.getMaQuyen() == 1) rd_add_StaffAdmin.setChecked(true);
                            else if (res.getMaQuyen() == 3) rd_add_StaffCashier.setChecked(true);
                            else rd_add_StaffStandard.setChecked(true);

                            // Cập nhật lại DatePicker theo ngày sinh nhận được (Chuẩn: YYYY-MM-DD)
                            String date = res.getNgaySinh();
                            if(date != null && date.contains("-")){
                                String[] items = date.split("-");
                                dt_add_StaffDOB.updateDate(Integer.parseInt(items[0]), Integer.parseInt(items[1])-1, Integer.parseInt(items[2]));
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<StaffResponse> call, Throwable t) {
                        Log.e(TAG, "Lỗi tải thông tin nhân viên: " + t.getMessage());
                    }
                });
            }
        }

        // Khôi phục thông tin tạm thời nếu Activity bị hủy (ví dụ: do xoay màn hình)
        if (savedInstanceState != null) {
            int genderId = savedInstanceState.getInt("gender_id", -1);
            if (genderId != -1) {
                rg_add_StaffGender.check(genderId);
            }
            int roleId = savedInstanceState.getInt("role_id", -1);
            if (roleId != -1) {
                rg_add_StaffRole.check(roleId);
            }
            int year = savedInstanceState.getInt("dob_year", -1);
            int month = savedInstanceState.getInt("dob_month", -1);
            int day = savedInstanceState.getInt("dob_day", -1);
            if (year != -1 && month != -1 && day != -1) {
                dt_add_StaffDOB.updateDate(year, month, day);
            }
        }

        // Đăng ký sự kiện click chuột
        btn_add_StaffCreate.setOnClickListener(this);
        img_add_StaffBack.setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu tạm trạng thái các ô chọn khi ứng dụng thay đổi cấu hình xoay màn hình
        outState.putInt("gender_id", rg_add_StaffGender.getCheckedRadioButtonId());
        outState.putInt("role_id", rg_add_StaffRole.getCheckedRadioButtonId());
        outState.putInt("dob_year", dt_add_StaffDOB.getYear());
        outState.putInt("dob_month", dt_add_StaffDOB.getMonth());
        outState.putInt("dob_day", dt_add_StaffDOB.getDayOfMonth());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_add_StaffCreate) {
            if (ViewUtils.isFastDoubleClick()) return; // Khóa spam click chuột liên tục
            
            // Thực hiện kiểm duyệt tính hợp lệ của toàn bộ biểu mẫu
            if (!validateAge() | !validateEmail() | !validateFullName() | !validateGender() | !validatePassWord() |
                    !validatePermission() | !validatePhone() | !validateUserName()) {
                return;
            }
            
            // Lấy nội dung chuỗi văn bản từ các trường nhập liệu
            hoTen = txtl_add_StaffFullName.getEditText().getText().toString();
            tenDN = txtl_add_StaffUserName.getEditText().getText().toString();
            eMail = txtl_add_StaffEmail.getEditText().getText().toString();
            sDT = txtl_add_StaffPhone.getEditText().getText().toString();
            matKhau = txtl_add_StaffPassword.getEditText().getText().toString();

            // Nhận diện lựa chọn giới tính
            int genderId = rg_add_StaffGender.getCheckedRadioButtonId();
            if (genderId == R.id.rd_add_StaffMale) {
                gioiTinh = "Nam";
            } else if (genderId == R.id.rd_add_StaffFemale) {
                gioiTinh = "Nữ";
            } else if (genderId == R.id.rd_add_StaffOther) {
                gioiTinh = "Khác";
            }

            // Nhận diện vai trò / quyền hạn
            int roleId = rg_add_StaffRole.getCheckedRadioButtonId();
            if (roleId == R.id.rd_add_StaffAdmin) {
                quyen = 1;
            } else if (roleId == R.id.rd_add_StaffStandard) {
                quyen = 2;
            } else if (roleId == R.id.rd_add_StaffCashier) {
                quyen = 3;
            }

            // Đồng bộ định dạng ngày chuẩn MySQL: yyyy-MM-dd để lưu trữ
            ngaySinh = dt_add_StaffDOB.getYear() + "-" + (dt_add_StaffDOB.getMonth() + 1)
                    + "-" + dt_add_StaffDOB.getDayOfMonth();

            // Thiết lập giá trị cho đối tượng trung chuyển dữ liệu DTO
            NhanVienDTO nhanVienDTO = new NhanVienDTO();
            nhanVienDTO.setHOTENNV(hoTen);
            nhanVienDTO.setTENDN(tenDN);
            nhanVienDTO.setEMAIL(eMail);
            nhanVienDTO.setSDT(sDT);
            nhanVienDTO.setMATKHAU(matKhau);
            nhanVienDTO.setGIOITINH(gioiTinh);
            nhanVienDTO.setNGAYSINH(ngaySinh);
            nhanVienDTO.setMAQUYEN(quyen);

            // Xác định là hành động thêm mới ("add") hay chỉnh sửa ("edit")
            String actionStaff = (manv != 0) ? "edit" : "add";

            // Hiển thị loading dialog
            androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(AddStaffActivity.this, "Đang xử lý...");
            progressDialog.show();

            // Gọi API lưu trữ dữ liệu nhân viên
            ApiService apiServiceStaff = ApiClient.getClient().create(ApiService.class);
            apiServiceStaff.manageStaff(actionStaff, manv, hoTen, tenDN, matKhau, eMail, sDT, gioiTinh, ngaySinh, quyen).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Quản lý nhân viên thành công: action=" + actionStaff + ", manv=" + manv + ", hoten=" + hoTen);
                        Intent intent = new Intent();
                        intent.putExtra("ketquaktra", (long) 1);
                        intent.putExtra("chucnang", (manv != 0) ? "sua" : "themnv");
                        setResult(RESULT_OK, intent); // Gửi thông báo hoàn thành về cho màn hình hiển thị danh sách nhân viên
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
        } else if (id == R.id.img_add_StaffBack) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    /**
     * Xác thực ô Họ và tên (không được để trống).
     */
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

    /**
     * Xác thực ô Tên đăng nhập (không chứa dấu cách, giới hạn 50 ký tự).
     */
    private boolean validateUserName(){
        String val = txtl_add_StaffUserName.getEditText().getText().toString().trim();
        String checkspaces = "\\A\\w{1,50}\\z";

        if(val.isEmpty()){
            txtl_add_StaffUserName.setError(getResources().getString(R.string.not_empty));
            return false;
        }else if(val.length() > 50){
            txtl_add_StaffUserName.setError("Phải nhỏ hơn 50 ký tự");
            return false;
        }else if(!val.matches(checkspaces)){
            txtl_add_StaffUserName.setError("Không được cách chữ!");
            return false;
        } else {
            txtl_add_StaffUserName.setError(null);
            txtl_add_StaffUserName.setErrorEnabled(false);
            return true;
        }
    }

    /**
     * Xác thực cấu trúc địa chỉ Email.
     */
    private boolean validateEmail(){
        String val = txtl_add_StaffEmail.getEditText().getText().toString().trim();
        String checkspaces = "[a-zA-Z0-9._-]+@[a-z]+.+[a-z]+";

        if(val.isEmpty()){
            txtl_add_StaffEmail.setError(getResources().getString(R.string.not_empty));
            return false;
        }else if(!val.matches(checkspaces)){
            txtl_add_StaffEmail.setError("Email không hợp lệ!");
            return false;
        } else {
            txtl_add_StaffEmail.setError(null);
            txtl_add_StaffEmail.setErrorEnabled(false);
            return true;
        }
    }

    /**
     * Xác thực ô Số điện thoại (Yêu cầu chính xác 10 ký số).
     */
    private boolean validatePhone(){
        String val = txtl_add_StaffPhone.getEditText().getText().toString().trim();
        if(val.isEmpty()){
            txtl_add_StaffPhone.setError(getResources().getString(R.string.not_empty));
            return false;
        }else if(val.length() != 10){
            txtl_add_StaffPhone.setError("Số điện thoại không hợp lệ!");
            return false;
        } else {
            txtl_add_StaffPhone.setError(null);
            txtl_add_StaffPhone.setErrorEnabled(false);
            return true;
        }
    }

    /**
     * Xác thực độ an toàn của mật khẩu theo chuẩn RegExp đã khai báo.
     */
    private boolean validatePassWord(){
        String val = txtl_add_StaffPassword.getEditText().getText().toString().trim();
        if(val.isEmpty()){
            txtl_add_StaffPassword.setError(getResources().getString(R.string.not_empty));
            return false;
        }else if(!PASSWORD_PATTERN.matcher(val).matches()){
            txtl_add_StaffPassword.setError("Mật khẩu ít nhất 6 ký tự!");
            return false;
        } else {
            txtl_add_StaffPassword.setError(null);
            txtl_add_StaffPassword.setErrorEnabled(false);
            return true;
        }
    }

    /**
     * Kiểm duyệt lựa chọn giới tính.
     */
    private boolean validateGender(){
        if(rg_add_StaffGender.getCheckedRadioButtonId() == -1){
            Toast.makeText(this, "Hãy chọn giới tính", Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }

    /**
     * Kiểm duyệt lựa chọn phân quyền nhân viên.
     */
    private boolean validatePermission(){
        if(rg_add_StaffRole.getCheckedRadioButtonId() == -1){
            Toast.makeText(this, "Hãy chọn quyền", Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }

    /**
     * Kiểm duyệt tuổi của nhân viên (yêu cầu từ 10 tuổi trở lên).
     */
    private boolean validateAge(){
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int userAge = dt_add_StaffDOB.getYear();
        int isAgeValid = currentYear - userAge;

        if(isAgeValid < 10){
            Toast.makeText(this, "Bạn không đủ tuổi đăng ký!", Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }
}