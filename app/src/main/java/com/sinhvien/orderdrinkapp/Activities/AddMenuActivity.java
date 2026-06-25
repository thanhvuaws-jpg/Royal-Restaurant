package com.sinhvien.orderdrinkapp.Activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.MonResponse;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * AddMenuActivity - Màn hình Thêm / Chỉnh sửa món ăn trong thực đơn (Menu).
 * Chức năng chính:
 * - Thu thập thông tin món ăn: Tên món, Giá tiền (tự động định dạng số 1.000đ khi nhập), Danh mục phân loại.
 * - Chọn ảnh minh họa từ Gallery hoặc chụp trực tiếp qua Camera.
 * - Cho phép bật/tắt tình trạng còn hàng (Tinhtrang: Còn món/Hết món).
 * - Kết nối REST API lên VPS và phát tín hiệu thay đổi qua Socket.IO tới toàn bộ nhân viên khác để cập nhật thực đơn tức thời (real-time).
 */
public class AddMenuActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "AddMenuActivity";

    // Khai báo các thành phần giao diện
    Button btn_add_DishCreate;
    LinearLayout layout_add_DishStatus;
    ImageView img_add_DishImage, img_add_DishBack;
    TextView txt_add_DishTitle;
    TextInputLayout txtl_add_DishName, txtl_add_DishPrice, txtl_add_DishType;
    RadioGroup rg_add_DishStatus;
    RadioButton rd_add_DishAvailable, rd_add_DishUnavailable;
    
    // Lưu thông tin phục vụ xử lý
    String tenloai, sTenMon, sGiaTien, sTinhTrang;
    Bitmap bitmapold; // Ảnh mặc định để kiểm tra người dùng đã đổi ảnh chưa
    int maloai; // Mã danh mục món ăn
    int mamon = 0; // ID món ăn (nếu = 0 là Thêm mới, > 0 là sửa món)
    private String selectedImageUriStr; // Lưu URI ảnh cục bộ
    private String cloudImageUrl; // URL ảnh tải từ Cloud

    /**
     * Bộ chọn hình ảnh từ Thư viện (Gallery) hệ thống.
     */
    ActivityResultLauncher<Intent> resultLauncherOpenIMG = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                        Uri uri = result.getData().getData();
                        selectedImageUriStr = uri.toString();
                        try{
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            img_add_DishImage.setImageBitmap(bitmap);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            });

    /**
     * Bộ chụp ảnh bằng Máy ảnh (Camera) của thiết bị.
     */
    ActivityResultLauncher<Intent> resultLauncherCamera = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.get("data") != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            img_add_DishImage.setImageBitmap(imageBitmap);
                            selectedImageUriStr = null;
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addmenu_layout);

        // Ánh xạ thành phần UI
        img_add_DishImage = findViewById(R.id.img_add_DishImage);
        img_add_DishBack = findViewById(R.id.img_add_DishBack);
        txtl_add_DishName = findViewById(R.id.txtl_add_DishName);
        txtl_add_DishPrice = findViewById(R.id.txtl_add_DishPrice);
        txtl_add_DishType = findViewById(R.id.txtl_add_DishType);
        btn_add_DishCreate = findViewById(R.id.btn_add_DishCreate);
        txt_add_DishTitle = findViewById(R.id.txt_add_DishTitle);
        layout_add_DishStatus = findViewById(R.id.layout_add_DishStatus);
        rg_add_DishStatus = findViewById(R.id.rg_add_DishStatus);
        rd_add_DishAvailable = findViewById(R.id.rd_add_DishAvailable);
        rd_add_DishUnavailable = findViewById(R.id.rd_add_DishUnavailable);

        // Nhận dữ liệu phân loại món từ màn hình danh sách gọi đến
        Intent intent = getIntent();
        maloai = intent.getIntExtra("maloai",-1);
        tenloai = intent.getStringExtra("tenloai");
        txtl_add_DishType.getEditText().setText(tenloai);

        BitmapDrawable olddrawable = (BitmapDrawable)img_add_DishImage.getDrawable();
        bitmapold = olddrawable.getBitmap();

        // Kiểm tra xem có nhận ID món ăn để chuyển chế độ sang Sửa hay không
        mamon = getIntent().getIntExtra("mamon",0);
        if(mamon != 0){
            txt_add_DishTitle.setText("Sửa thực đơn");
            btn_add_DishCreate.setText("Sửa món");
            
            // Tải thông tin món cần sửa từ Cloud về nếu không có dữ liệu khôi phục tạm thời
            if (savedInstanceState == null) {
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                apiService.getDishById(mamon).enqueue(new Callback<MonResponse>() {
                    @Override
                    public void onResponse(Call<MonResponse> call, Response<MonResponse> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            MonResponse res = response.body();
                            maloai = res.getMaLoai();
                            Log.d(TAG, "Tải thông tin món thành công: mamon=" + mamon + ", tenmon=" + res.getTenMon());
                            
                            if(txtl_add_DishName.getEditText() != null) txtl_add_DishName.getEditText().setText(res.getTenMon());
                            if(txtl_add_DishPrice.getEditText() != null) {
                                try {
                                    // Định dạng lại giá tiền dạng nghìn đồng phân cách dấu chấm "." (ví dụ: 50.000)
                                    long price = Long.parseLong(res.getGiaTien());
                                    String formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.GERMANY).format(price);
                                    txtl_add_DishPrice.getEditText().setText(formatted);
                                } catch (Exception e) {
                                    txtl_add_DishPrice.getEditText().setText(res.getGiaTien());
                                }
                            }

                            // Tải chi tiết tên danh mục
                            apiService.getCategoryById(maloai).enqueue(new Callback<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>() {
                                @Override
                                public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> call, Response<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        txtl_add_DishType.getEditText().setText(response.body().getTenLoai());
                                    }
                                }
                                @Override
                                public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> call, Throwable t) {
                                    Log.e(TAG, "Lỗi tải tên loại cho món: " + t.getMessage());
                                }
                            });

                            // Load ảnh món ăn bằng Glide
                            if (res.getHinhAnh() != null && !res.getHinhAnh().isEmpty()) {
                                String imageUrl = com.sinhvien.orderdrinkapp.Utils.ViewUtils.getImageUrl(res.getHinhAnh());
                                cloudImageUrl = imageUrl;
                                Glide.with(AddMenuActivity.this)
                                        .load(imageUrl)
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .into(img_add_DishImage);
                            }

                            // Cập nhật tình trạng hàng
                            layout_add_DishStatus.setVisibility(View.VISIBLE);
                            if("true".equals(res.getTinhTrang())){
                                rd_add_DishAvailable.setChecked(true);
                            } else {
                                rd_add_DishUnavailable.setChecked(true);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<MonResponse> call, Throwable t) {}
                });
            }
        }

        // Khôi phục trạng thái nhập liệu tạm thời khi xoay màn hình thiết bị
        if (savedInstanceState != null) {
            selectedImageUriStr = savedInstanceState.getString("selected_image_uri");
            cloudImageUrl = savedInstanceState.getString("cloud_image_url");
            maloai = savedInstanceState.getInt("maloai", maloai);
            int statusId = savedInstanceState.getInt("status_id", -1);
            if (statusId != -1) {
                rg_add_DishStatus.check(statusId);
            }
            if (selectedImageUriStr != null) {
                try {
                    Uri uri = Uri.parse(selectedImageUriStr);
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    img_add_DishImage.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (cloudImageUrl != null) {
                Glide.with(AddMenuActivity.this)
                        .load(cloudImageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(img_add_DishImage);
            }
        }

        // Tự động định dạng dấu phân cách phần nghìn theo chuẩn tiếng Đức (Ví dụ: 1.000.000) khi người dùng gõ
        if (txtl_add_DishPrice.getEditText() != null) {
            txtl_add_DishPrice.getEditText().addTextChangedListener(new android.text.TextWatcher() {
                private String current = "";
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    if (!s.toString().equals(current)) {
                        txtl_add_DishPrice.getEditText().removeTextChangedListener(this);
                        String cleanString = s.toString().replaceAll("[.,]", ""); // Bỏ dấu chấm cũ để phân tích
                        if (!cleanString.isEmpty()) {
                            double parsed = Double.parseDouble(cleanString);
                            String formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.GERMANY).format(parsed);
                            current = formatted;
                            txtl_add_DishPrice.getEditText().setText(formatted);
                            txtl_add_DishPrice.getEditText().setSelection(formatted.length()); // Đưa con trỏ xuống cuối dòng
                        } else {
                            current = "";
                        }
                        txtl_add_DishPrice.getEditText().addTextChangedListener(this);
                    }
                }
            });
        }

        // Thiết lập sự kiện Click
        img_add_DishImage.setOnClickListener(this);
        btn_add_DishCreate.setOnClickListener(this);
        img_add_DishBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.img_add_DishImage) {
            hienThiDialogChonAnh();
        } else if (id == R.id.img_add_DishBack) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } else if (id == R.id.btn_add_DishCreate) {
            if (ViewUtils.isFastDoubleClick()) return; // Khóa nhấn nhiều lần liên tục
            
            // Xác thực thông tin biểu mẫu
            if (!validateImage() | !validateName() | !validatePrice()) {
                return;
            }

            if (txtl_add_DishName.getEditText() != null)
                sTenMon = txtl_add_DishName.getEditText().getText().toString();
            if (txtl_add_DishPrice.getEditText() != null) {
                // Loại bỏ hoàn toàn dấu phân cách khi lưu vào cơ sở dữ liệu (chỉ để lại các số)
                sGiaTien = txtl_add_DishPrice.getEditText().getText().toString().replaceAll("[.,]", "");
            }

            sTinhTrang = "true"; // Mặc định là còn hàng
            if (rg_add_DishStatus.getCheckedRadioButtonId() == R.id.rd_add_DishUnavailable) {
                sTinhTrang = "false"; // Hết hàng
            }

            String actionMon = (mamon != 0) ? "edit" : "add";
            String imageBase64Mon = imageToBase64(img_add_DishImage); // Encode Base64 cho ảnh

            // Loading dialog xử lý
            androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(AddMenuActivity.this, "Đang xử lý...");
            progressDialog.show();

            // Gửi API yêu cầu cập nhật hoặc thêm mới món ăn
            ApiService apiServiceMon = ApiClient.getClient().create(ApiService.class);
            apiServiceMon.manageDish(actionMon, mamon, sTenMon, sGiaTien, maloai, sTinhTrang, imageBase64Mon).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        OrderResponse res = response.body();
                        if ("success".equals(res.getStatus())) {
                            Log.d(TAG, "Quản lý món thành công: action=" + actionMon + ", mamon=" + mamon + ", tenmon=" + sTenMon);

                            // Gửi tín hiệu WebSocket để cập nhật danh sách món tức thì (real-time) cho các máy phục vụ/thu ngân khác
                            io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                            if (socket != null && socket.connected()) {
                                socket.emit("menu_changed");
                            }

                            Intent intent = new Intent();
                            intent.putExtra("ktra", true);
                            intent.putExtra("chucnang", (mamon != 0) ? "suamon" : "themmon");
                            setResult(RESULT_OK, intent);
                            finish();
                        } else {
                            Log.w(TAG, "Lỗi quản lý món: " + res.getMessage());
                            Toast.makeText(AddMenuActivity.this, "Lỗi: " + res.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.w(TAG, "Server không phản hồi đúng định dạng khi quản lý món");
                        Toast.makeText(AddMenuActivity.this, "Lỗi Server không phản hồi đúng định dạng", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<OrderResponse> call, Throwable t) {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    Log.e(TAG, "Lỗi kết nối API quản lý món: " + t.getMessage());
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(AddMenuActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**
     * Nén hình ảnh về kích thước tối đa 500 pixel và mã hóa sang chuỗi Base64.
     */
    private String imageToBase64(ImageView imageView){
        try {
            android.graphics.drawable.Drawable drawable = imageView.getDrawable();
            if (drawable == null) return "";
            
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable)drawable).getBitmap();
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
            
            bitmap = getResizedBitmap(bitmap, 500);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            byte[] byteArray = stream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Cân đối kích thước ảnh Bitmap.
     */
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    /**
     * Xác thực hình ảnh món ăn đã được cung cấp (Khi thêm mới bắt buộc phải chọn).
     */
    private boolean validateImage(){
        if (mamon != 0) {
            return true; // Nếu là sửa món thì cho phép bỏ qua không cần chọn lại ảnh mới
        }

        BitmapDrawable drawable = (BitmapDrawable)img_add_DishImage.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        if(bitmap == bitmapold){
            Toast.makeText(getApplicationContext(), "Xin chọn hình ảnh", Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }

    /**
     * Xác thực ô Tên món ăn.
     */
    private boolean validateName(){
        String val = "";
        if(txtl_add_DishName.getEditText() != null) val = txtl_add_DishName.getEditText().getText().toString().trim();
        if(val.isEmpty()){
            txtl_add_DishName.setError(getResources().getString(R.string.not_empty));
            return false;
        }else {
            txtl_add_DishName.setError(null);
            txtl_add_DishName.setErrorEnabled(false);
            return true;
        }
    }

    /**
     * Xác thực ô Giá món ăn.
     */
    private boolean validatePrice(){
        String val = "";
        if(txtl_add_DishPrice.getEditText() != null) val = txtl_add_DishPrice.getEditText().getText().toString().trim();
        String cleanVal = val.replaceAll("[.,]", "");
        if(val.isEmpty()){
            txtl_add_DishPrice.setError(getResources().getString(R.string.not_empty));
            return false;
        }else if(!cleanVal.matches(("\\d+(?:\\.\\d+)?"))){
            txtl_add_DishPrice.setError("Giá tiền không hợp lệ");
            return false;
        }else {
            txtl_add_DishPrice.setError(null);
            txtl_add_DishPrice.setErrorEnabled(false);
            return true;
        }
    }

    /**
     * Hiển thị Bottom Dialog chọn nguồn ảnh.
     */
    private void hienThiDialogChonAnh() {
        String[] options = {"Chọn từ Thư viện", "Chụp ảnh mới"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn hình ảnh")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent iGetIMG = new Intent();
                        iGetIMG.setType("image/*");
                        iGetIMG.setAction(Intent.ACTION_GET_CONTENT);
                        resultLauncherOpenIMG.launch(Intent.createChooser(iGetIMG, getResources().getString(R.string.choseimg)));
                    } else {
                        Intent iCamera = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        resultLauncherCamera.launch(iCamera);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selected_image_uri", selectedImageUriStr);
        outState.putString("cloud_image_url", cloudImageUrl);
        outState.putInt("maloai", maloai);
        outState.putInt("status_id", rg_add_DishStatus.getCheckedRadioButtonId());
    }
}