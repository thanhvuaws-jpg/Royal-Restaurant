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
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.LoaiMonResponse;
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

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class AddCategoryActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AddCategoryActivity";

    Button BTN_addcategory_CreateCategory;
    ImageView IMG_addcategory_back, IMG_addcategory_AddImage;
    TextView TXT_addcategory_title;
    TextInputLayout TXTL_addcategory_CategoryName;
    int maloai = 0;
    Bitmap bitmapold;   //Bitmap dạng ảnh theo ma trận các pixel
    private String selectedImageUriStr;
    private String cloudImageUrl;

    //dùng result launcher do activityforresult ko dùng đc nữa
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
                            IMG_addcategory_AddImage.setImageBitmap(bitmap);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            });

    ActivityResultLauncher<Intent> resultLauncherCamera = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.get("data") != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            IMG_addcategory_AddImage.setImageBitmap(imageBitmap);
                            selectedImageUriStr = null;
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addcategory_layout);

        //region Lấy đối tượng view
        BTN_addcategory_CreateCategory = (Button)findViewById(R.id.btn_addcategory_CreateCategory);
        TXTL_addcategory_CategoryName = (TextInputLayout)findViewById(R.id.txtl_addcategory_CategoryName);
        IMG_addcategory_back = (ImageView)findViewById(R.id.img_addcategory_back);
        IMG_addcategory_AddImage = (ImageView)findViewById(R.id.img_addcategory_AddImage);
        TXT_addcategory_title = (TextView)findViewById(R.id.txt_addcategory_title);
        //endregion

        BitmapDrawable olddrawable = (BitmapDrawable)IMG_addcategory_AddImage.getDrawable();
        bitmapold = olddrawable.getBitmap();

        //region Hiển thị trang sửa nếu được chọn từ context menu sửa
        // Lấy thông tin từ Cloud nếu là sửa
        maloai = getIntent().getIntExtra("maloai",0);
        if(maloai != 0){
            TXT_addcategory_title.setText(getResources().getString(R.string.editcategory));
            BTN_addcategory_CreateCategory.setText("Sửa loại");
            if (savedInstanceState == null) {
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                apiService.getCategoryById(maloai).enqueue(new Callback<LoaiMonResponse>() {
                    @Override
                    public void onResponse(Call<LoaiMonResponse> call, Response<LoaiMonResponse> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Tải thông tin loại thành công: maloai=" + maloai);
                            TXTL_addcategory_CategoryName.getEditText().setText(response.body().getTenLoai());
                            String imageUrl = com.sinhvien.orderdrinkapp.Utils.ViewUtils.getImageUrl(response.body().getHinhAnh());
                            cloudImageUrl = imageUrl;
                            Glide.with(AddCategoryActivity.this)
                                    .load(imageUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(IMG_addcategory_AddImage);
                        }
                    }
                    @Override
                    public void onFailure(Call<LoaiMonResponse> call, Throwable t) {
                        Log.e(TAG, "Lỗi tải thông tin loại: " + t.getMessage());
                    }
                });
            }
        }

        if (savedInstanceState != null) {
            selectedImageUriStr = savedInstanceState.getString("selected_image_uri");
            cloudImageUrl = savedInstanceState.getString("cloud_image_url");
            if (selectedImageUriStr != null) {
                try {
                    Uri uri = Uri.parse(selectedImageUriStr);
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    IMG_addcategory_AddImage.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (cloudImageUrl != null) {
                Glide.with(AddCategoryActivity.this)
                        .load(cloudImageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(IMG_addcategory_AddImage);
            }
        }
        //endregion

        IMG_addcategory_back.setOnClickListener(this);
        IMG_addcategory_AddImage.setOnClickListener(this);
        BTN_addcategory_CreateCategory.setOnClickListener(this);
    }

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
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        boolean ktra;
        String chucnang;
        if (id == R.id.img_addcategory_back) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); //animation
        } else if (id == R.id.img_addcategory_AddImage) {
            hienThiDialogChonAnh();
        } else if (id == R.id.btn_addcategory_CreateCategory) {
            if (ViewUtils.isFastDoubleClick()) return; // Chống double click
            if (!validateImage() | !validateName()) {
                return;
            }

            String sTenLoai = TXTL_addcategory_CategoryName.getEditText().getText().toString();
            String action = (maloai != 0) ? "edit" : "add";
            String imageBase64 = imageToBase64(IMG_addcategory_AddImage);

            androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(AddCategoryActivity.this, "Đang xử lý...");
            progressDialog.show();

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.manageCategory(action, maloai, sTenLoai, imageBase64).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Quản lý loại thành công: action=" + action + ", maloai=" + maloai);
                        Intent intent = new Intent();
                        intent.putExtra("ktra", true);
                        intent.putExtra("chucnang", (maloai != 0) ? "sualoai" : "themloai");
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<OrderResponse> call, Throwable t) {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    Log.e(TAG, "Lỗi kết nối API quản lý loại: " + t.getMessage());
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(AddCategoryActivity.this, "Lỗi Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // Chuyển ảnh sang Base64 để gửi lên VPS
    private String imageToBase64(ImageView imageView){
        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        bitmap = getResizedBitmap(bitmap, 500); // Thu nhỏ để gửi nhanh hơn
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        byte[] byteArray = stream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Hàm thu nhỏ ảnh giữ nguyên tỷ lệ
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

    //region validate fields
    private boolean validateImage(){
        BitmapDrawable drawable = (BitmapDrawable)IMG_addcategory_AddImage.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        if(bitmap == bitmapold){
            Toast.makeText(getApplicationContext(),"Xin chọn hình ảnh",Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }

    private boolean validateName(){
        String val = TXTL_addcategory_CategoryName.getEditText().getText().toString().trim();
        if(val.isEmpty()){
            TXTL_addcategory_CategoryName.setError(getResources().getString(R.string.not_empty));
            return false;
        }else {
            TXTL_addcategory_CategoryName.setError(null);
            TXTL_addcategory_CategoryName.setErrorEnabled(false);
            return true;
        }
    }
    //endregion

}