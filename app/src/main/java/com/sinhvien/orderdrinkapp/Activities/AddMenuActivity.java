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
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class AddMenuActivity extends AppCompatActivity implements View.OnClickListener{

    Button btn_add_DishCreate;
    LinearLayout layout_add_DishStatus;
    ImageView img_add_DishImage, img_add_DishBack;
    TextView txt_add_DishTitle;
    TextInputLayout txtl_add_DishName, txtl_add_DishPrice, txtl_add_DishType;
    RadioGroup rg_add_DishStatus;
    RadioButton rd_add_DishAvailable, rd_add_DishUnavailable;
    String tenloai, sTenMon,sGiaTien,sTinhTrang;
    Bitmap bitmapold;
    int maloai;
    int mamon = 0;

    ActivityResultLauncher<Intent> resultLauncherOpenIMG = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                        Uri uri = result.getData().getData();
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addmenu_layout);

        //region Lấy đối tượng view
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
        //endregion

        Intent intent = getIntent();
        maloai = intent.getIntExtra("maloai",-1);
        tenloai = intent.getStringExtra("tenloai");
        txtl_add_DishType.getEditText().setText(tenloai);

        BitmapDrawable olddrawable = (BitmapDrawable)img_add_DishImage.getDrawable();
        bitmapold = olddrawable.getBitmap();

        //region Hiển thị trang sửa nếu được chọn từ context menu sửa
        // Lấy thông tin từ Cloud nếu là sửa
        mamon = getIntent().getIntExtra("mamon",0);
        if(mamon != 0){
            txt_add_DishTitle.setText("Sửa thực đơn");
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.getDishById(mamon).enqueue(new Callback<MonResponse>() {
                @Override
                public void onResponse(Call<MonResponse> call, Response<MonResponse> response) {
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        MonResponse res = response.body();
                        maloai = res.getMaLoai(); // Cập nhật mã loại từ server
                        
                        if(txtl_add_DishName.getEditText() != null) txtl_add_DishName.getEditText().setText(res.getTenMon());
                        if(txtl_add_DishPrice.getEditText() != null) {
                            try {
                                long price = Long.parseLong(res.getGiaTien());
                                String formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.GERMANY).format(price);
                                txtl_add_DishPrice.getEditText().setText(formatted);
                            } catch (Exception e) {
                                txtl_add_DishPrice.getEditText().setText(res.getGiaTien());
                            }
                        }

                        // Lấy tên loại món từ server để hiển thị
                        apiService.getCategoryById(maloai).enqueue(new Callback<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>() {
                            @Override
                            public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> call, Response<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    txtl_add_DishType.getEditText().setText(response.body().getTenLoai());
                                }
                            }
                            @Override
                            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> call, Throwable t) {}
                        });

                        if (res.getHinhAnh() != null && !res.getHinhAnh().isEmpty()) {
                            String imageUrl = ApiClient.BASE_URL + res.getHinhAnh();
                            Glide.with(AddMenuActivity.this)
                                    .load(imageUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(img_add_DishImage);
                        }

                        layout_add_DishStatus.setVisibility(View.VISIBLE);
                        if("true".equals(res.getTinhTrang())){
                            rd_add_DishAvailable.setChecked(true);
                        } else {
                            rd_add_DishUnavailable.setChecked(true);
                        }
                        btn_add_DishCreate.setText("Sửa món");
                    }
                }
                @Override
                public void onFailure(Call<MonResponse> call, Throwable t) {}
            });
        }

        //endregion

        //region Tự động format giá tiền khi nhập
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
                        String cleanString = s.toString().replaceAll("[.,]", "");
                        if (!cleanString.isEmpty()) {
                            double parsed = Double.parseDouble(cleanString);
                            String formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.GERMANY).format(parsed);
                            current = formatted;
                            txtl_add_DishPrice.getEditText().setText(formatted);
                            txtl_add_DishPrice.getEditText().setSelection(formatted.length());
                        } else {
                            current = "";
                        }
                        txtl_add_DishPrice.getEditText().addTextChangedListener(this);
                    }
                }
            });
        }
        //endregion

        img_add_DishImage.setOnClickListener(this);
        btn_add_DishCreate.setOnClickListener(this);
        img_add_DishBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        boolean ktra;
        String chucnang;
        switch (id){
            case R.id.img_add_DishImage:
                Intent iGetIMG = new Intent();
                iGetIMG.setType("image/*");
                iGetIMG.setAction(Intent.ACTION_GET_CONTENT);
                resultLauncherOpenIMG.launch(Intent.createChooser(iGetIMG,getResources().getString(R.string.choseimg)));
                break;

            case R.id.img_add_DishBack:
                finish();
                overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
                break;

            case R.id.btn_add_DishCreate:
                if(!validateImage() | !validateName() | !validatePrice()){
                    return;
                }

                if(txtl_add_DishName.getEditText() != null) sTenMon = txtl_add_DishName.getEditText().getText().toString();
                if(txtl_add_DishPrice.getEditText() != null) {
                    sGiaTien = txtl_add_DishPrice.getEditText().getText().toString().replaceAll("[.,]", "");
                }

                sTinhTrang = "true";
                if (rg_add_DishStatus.getCheckedRadioButtonId() == R.id.rd_add_DishUnavailable) {
                    sTinhTrang = "false";
                }

                String actionMon = (mamon != 0) ? "edit" : "add";
                String imageBase64Mon = imageToBase64(img_add_DishImage);

                androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(AddMenuActivity.this, "Đang xử lý...");
                progressDialog.show();

                ApiService apiServiceMon = ApiClient.getClient().create(ApiService.class);
                apiServiceMon.manageDish(actionMon, mamon, sTenMon, sGiaTien, maloai, sTinhTrang, imageBase64Mon).enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            OrderResponse res = response.body();
                            if ("success".equals(res.getStatus())) {
                                Intent intent = new Intent();
                                intent.putExtra("ktra", true);
                                intent.putExtra("chucnang", (mamon != 0) ? "suamon" : "themmon");
                                setResult(RESULT_OK, intent);
                                finish();
                            } else {
                                Toast.makeText(AddMenuActivity.this, "Lỗi: " + res.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(AddMenuActivity.this, "Lỗi Server không phản hồi đúng định dạng", Toast.LENGTH_SHORT).show();
                        }
                    }
 
                    @Override
                    public void onFailure(Call<OrderResponse> call, Throwable t) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(AddMenuActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
        }
    }

    private String imageToBase64(ImageView imageView){
        try {
            android.graphics.drawable.Drawable drawable = imageView.getDrawable();
            if (drawable == null) return "";
            
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable)drawable).getBitmap();
            } else {
                // Trường hợp ảnh từ Glide hoặc Resource, tạo bitmap mới từ drawable
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

    //region Validate field
    private boolean validateImage(){
        // Nếu là chế độ sửa (mamon != 0), cho phép không chọn ảnh mới
        if (mamon != 0) {
            return true;
        }

        BitmapDrawable drawable = (BitmapDrawable)img_add_DishImage.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        if(bitmap == bitmapold){
            Toast.makeText(getApplicationContext(),"Xin chọn hình ảnh",Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }

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

    private boolean validatePrice(){
        String val = "";
        if(txtl_add_DishPrice.getEditText() != null) val = txtl_add_DishPrice.getEditText().getText().toString().trim();
        // Xóa dấu chấm để validate số thuần túy
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
    //endregion

}