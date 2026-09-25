package com.sinhvien.orderdrinkapp.Activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.sinhvien.orderdrinkapp.Api.AnhMauBanResponse;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.AnhHelper;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AddTableActivity - Màn hình Thêm / Sửa bàn ăn (chỉ quản lý).
 *
 * THIẾT KẾ LẠI 25/09/2026 (QĐ-095): trước đây chỉ nhập được tên, ảnh bàn
 * chỉ đổi được bằng SQL, và chế độ sửa không bao giờ được mở (không nơi nào
 * truyền "maban"). Giờ:
 * - Chọn ảnh bàn theo một trong ba cách: bấm một ảnh mẫu có sẵn trên máy
 *   chủ, chụp ảnh bàn thật, hoặc chọn từ thư viện. Ảnh chụp/chọn được nén
 *   dưới 1 MB (AnhHelper) rồi gửi base64 để máy chủ tải lên Cloudinary.
 * - Chế độ sửa (nhấn giữ thẻ bàn trên sơ đồ) điền sẵn tên và ảnh hiện tại.
 * - Máy chủ từ chối (tên trùng, ảnh không tải lên được…) thì hiện đúng lý
 *   do và ở lại màn hình. Trước đây mọi phản hồi đều đóng màn hình như thể
 *   đã thành công.
 */
public class AddTableActivity extends AppCompatActivity {

    private static final String TAG = "AddTableActivity";

    /** Intent extra khi mở ở chế độ sửa. */
    public static final String EXTRA_MABAN = "maban";
    public static final String EXTRA_TENBAN = "tenban";
    /** Ảnh hiện tại của bàn: đường dẫn nội bộ ("images/ban/…") hoặc URL Cloudinary. */
    public static final String EXTRA_ANH = "anh";

    // Khai báo thành phần View
    TextInputLayout TXTL_addtable_TableName;
    Button BTN_addtable_CreateTable;
    ImageView IMG_addtable_back;
    private ImageView imgAnh;
    private TextView txtChuaAnh, txtNguonAnh;
    private RecyclerView rvAnhMau;

    int maban = 0; // ID của bàn ăn (nếu = 0 là chế độ Thêm mới)

    // ── Ảnh đang chọn: đúng MỘT trong ba, hoặc không đổi gì ──
    /** Đường dẫn ảnh mẫu đã bấm chọn (gửi trong trường hinhanh). */
    private String anhMauChon;
    /** Ảnh chụp/chọn từ máy, đã nén (gửi trong trường hinhanh_base64). */
    private String anhBase64;
    /** Người dùng bấm "Bỏ ảnh" với bàn đang có ảnh. */
    private boolean boAnh;
    /** Ảnh bàn đang có khi mở màn sửa — để hiện lại và biết có gì để bỏ. */
    private String anhBanDau;

    private final List<AnhMauBanResponse.AnhMau> dsAnhMau = new ArrayList<>();
    private AnhMauAdapter anhMauAdapter;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    /** Tệp tạm cho camera ghi ảnh gốc vào (không dùng ảnh xem trước nhỏ xíu). */
    private Uri uriAnhChup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addtable_layout);

        // Ánh xạ các thành phần View từ XML layout
        TXTL_addtable_TableName = findViewById(R.id.txtl_addtable_TableName);
        // Xóa dòng lỗi ngay khi người dùng gõ lại (H22).
        com.sinhvien.orderdrinkapp.Utils.ViewUtils.xoaLoiKhiGo(TXTL_addtable_TableName);
        BTN_addtable_CreateTable = findViewById(R.id.btn_addtable_CreateTable);
        IMG_addtable_back = findViewById(R.id.img_addtable_back);
        imgAnh = findViewById(R.id.img_addtable_anh);
        txtChuaAnh = findViewById(R.id.txt_addtable_chua_anh);
        txtNguonAnh = findViewById(R.id.txt_addtable_nguon_anh);
        rvAnhMau = findViewById(R.id.rv_addtable_anh_mau);

        // Lấy thông tin bàn truyền qua Intent (chế độ sửa bàn)
        maban = getIntent().getIntExtra(EXTRA_MABAN, 0);
        anhBanDau = getIntent().getStringExtra(EXTRA_ANH);
        if (savedInstanceState != null) {
            maban = savedInstanceState.getInt("maban", maban);
            anhMauChon = savedInstanceState.getString("anhMauChon");
            boAnh = savedInstanceState.getBoolean("boAnh", false);
            // Mở camera có thể khiến hệ thống hủy rồi tạo lại màn này; mất
            // uriAnhChup thì ảnh vừa chụp xong không biết nằm ở đâu.
            uriAnhChup = savedInstanceState.getParcelable("uriAnhChup");
            // Ảnh base64 KHÔNG lưu qua xoay màn hình: tới ~1,3 MB, vượt giới
            // hạn của Bundle (TransactionTooLargeException). Người dùng chụp lại.
        }
        if (maban != 0) {
            ((TextView) findViewById(R.id.txt_addtable_title)).setText(R.string.edit_table_title);
            BTN_addtable_CreateTable.setText("Cập nhật bàn");
            if (savedInstanceState == null && TXTL_addtable_TableName.getEditText() != null) {
                TXTL_addtable_TableName.getEditText().setText(getIntent().getStringExtra(EXTRA_TENBAN));
            }
        }

        khoiTaoChonAnh();
        hienAnhDangChon();
        taiAnhMau();

        BTN_addtable_CreateTable.setOnClickListener(v -> {
            if (ViewUtils.isFastDoubleClick()) return; // Khóa click liên tục quá nhanh
            if (!validateName()) return;
            luuBan();
        });

        IMG_addtable_back.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // Hiệu ứng trượt lùi
        });
    }

    /* ═══════════════════════════ Chọn ảnh ═══════════════════════════ */

    private void khoiTaoChonAnh() {
        // TakePicture (ảnh gốc ghi vào tệp) thay vì TakePicturePreview: bản
        // xem trước chỉ vài trăm điểm ảnh, kéo lên khung 170dp là vỡ hạt.
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), daChup -> {
            if (Boolean.TRUE.equals(daChup) && uriAnhChup != null) nhanAnhTuMay(uriAnhChup, "Ảnh vừa chụp");
        });
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) nhanAnhTuMay(uri, "Ảnh chọn từ thư viện");
        });

        findViewById(R.id.btn_addtable_chup).setOnClickListener(v -> {
            try {
                File thuMuc = new File(getCacheDir(), "anh_ban");
                //noinspection ResultOfMethodCallIgnored
                thuMuc.mkdirs();
                File tep = new File(thuMuc, "chup_ban.jpg");
                uriAnhChup = androidx.core.content.FileProvider.getUriForFile(
                        this, getPackageName() + ".fileprovider", tep);
                cameraLauncher.launch(uriAnhChup);
            } catch (Exception e) {
                Log.e(TAG, "Không mở được camera", e);
                Toast.makeText(this, "Không mở được camera trên máy này", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_addtable_thu_vien).setOnClickListener(v -> galleryLauncher.launch("image/*"));
        findViewById(R.id.btn_addtable_bo_anh).setOnClickListener(v -> {
            anhMauChon = null;
            anhBase64 = null;
            boAnh = true;
            hienAnhDangChon();
        });

        anhMauAdapter = new AnhMauAdapter();
        rvAnhMau.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvAnhMau.setAdapter(anhMauAdapter);
    }

    /** Ảnh chụp hoặc chọn từ thư viện: nén dưới 1 MB rồi giữ dạng base64. */
    private void nhanAnhTuMay(Uri uri, String nguon) {
        Bitmap bitmap = AnhHelper.docBitmapTuUri(this, uri);
        if (bitmap == null) {
            Toast.makeText(this, "Không đọc được ảnh này", Toast.LENGTH_SHORT).show();
            return;
        }
        anhBase64 = AnhHelper.nenVaChuyenBase64(bitmap);
        anhMauChon = null;
        boAnh = false;
        imgAnh.setVisibility(View.VISIBLE);
        txtChuaAnh.setVisibility(View.GONE);
        Glide.with(this).clear(imgAnh);
        imgAnh.setImageBitmap(bitmap);
        txtNguonAnh.setText(nguon + " — sẽ tải lên khi lưu bàn");
        anhMauAdapter.notifyDataSetChanged();
        capNhatNutBoAnh();
    }

    /** "Bỏ ảnh" chỉ hiện khi đang có ảnh để bỏ (ảnh vừa chọn, hoặc ảnh cũ chưa bỏ). */
    private void capNhatNutBoAnh() {
        boolean coAnh = anhMauChon != null || anhBase64 != null || (anhBanDau != null && !boAnh);
        findViewById(R.id.btn_addtable_bo_anh).setVisibility(coAnh ? View.VISIBLE : View.INVISIBLE);
    }

    /** Vẽ khung xem trước theo lựa chọn hiện tại (ảnh mẫu / ảnh cũ / không ảnh). */
    private void hienAnhDangChon() {
        String duongDan;
        String nguon;
        if (anhMauChon != null) {
            duongDan = anhMauChon;
            nguon = "Ảnh mẫu";
        } else if (boAnh) {
            duongDan = null;
            nguon = anhBanDau != null ? "Sẽ bỏ ảnh hiện tại khi lưu" : "";
        } else {
            duongDan = anhBanDau;
            nguon = anhBanDau != null ? "Ảnh hiện tại của bàn" : "";
        }
        txtNguonAnh.setText(nguon);
        capNhatNutBoAnh();

        String url = ViewUtils.getImageUrl(duongDan);
        if (url.isEmpty()) {
            Glide.with(this).clear(imgAnh);
            imgAnh.setVisibility(View.GONE);
            txtChuaAnh.setVisibility(View.VISIBLE);
            return;
        }
        imgAnh.setVisibility(View.VISIBLE);
        txtChuaAnh.setVisibility(View.GONE);
        Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).centerCrop().into(imgAnh);
    }

    private void taiAnhMau() {
        ApiClient.getClient().create(ApiService.class).getAnhMauBan().enqueue(new Callback<AnhMauBanResponse>() {
            @Override
            public void onResponse(Call<AnhMauBanResponse> call, Response<AnhMauBanResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    dsAnhMau.clear();
                    dsAnhMau.addAll(response.body().getAnh());
                    anhMauAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<AnhMauBanResponse> call, Throwable t) {
                Log.w(TAG, "Không tải được ảnh mẫu: " + t.getMessage());
            }
        });
    }

    /** Lưới ngang các ảnh mẫu; ô đang chọn có viền vàng. */
    private class AnhMauAdapter extends RecyclerView.Adapter<AnhMauAdapter.O> {
        class O extends RecyclerView.ViewHolder {
            final MaterialCardView the;
            final ImageView anh;
            O(View v) {
                super(v);
                the = v.findViewById(R.id.card_anh_mau);
                anh = v.findViewById(R.id.img_anh_mau);
            }
        }

        @Override
        public O onCreateViewHolder(ViewGroup parent, int viewType) {
            return new O(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_anh_mau_ban, parent, false));
        }

        @Override
        public void onBindViewHolder(O o, int position) {
            AnhMauBanResponse.AnhMau a = dsAnhMau.get(position);
            boolean dangChon = a.getDuongDan() != null && a.getDuongDan().equals(anhMauChon);
            o.the.setStrokeWidth(dangChon ? (int) (3 * getResources().getDisplayMetrics().density) : 0);
            o.the.setContentDescription("Ảnh mẫu " + (position + 1) + (dangChon ? ", đang chọn" : ""));
            Glide.with(AddTableActivity.this).load(ViewUtils.getImageUrl(a.getUrlAnhNho()))
                    .diskCacheStrategy(DiskCacheStrategy.ALL).centerCrop().into(o.anh);
            o.the.setOnClickListener(v -> {
                anhMauChon = a.getDuongDan();
                anhBase64 = null;
                boAnh = false;
                hienAnhDangChon();
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() { return dsAnhMau.size(); }
    }

    /* ═══════════════════════════ Lưu ═══════════════════════════ */

    private void luuBan() {
        String sTenBanAn = TXTL_addtable_TableName.getEditText().getText().toString().trim();
        String action = (maban != 0) ? "edit" : "add";

        androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper
                .getLoadingDialog(this, anhBase64 != null ? "Đang tải ảnh lên..." : "Đang xử lý...");
        progressDialog.show();

        // Chỉ gửi đúng một cách đổi ảnh; không đổi gì thì cả ba để null và
        // máy chủ giữ nguyên ảnh cũ.
        String boAnhGui = (boAnh && anhBanDau != null) ? "1" : null;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.luuBan(action, maban, sTenBanAn, anhMauChon, anhBase64, boAnhGui).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    Log.d(TAG, "Lưu bàn thành công: action=" + action + ", maban=" + maban + ", tenban=" + sTenBanAn);
                    // Máy khác đang mở sơ đồ bàn nghe sự kiện này và tải lại.
                    io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                    if (socket != null && socket.connected()) socket.emit("refresh_orders");
                    Toast.makeText(AddTableActivity.this, maban != 0 ? "Đã cập nhật bàn" : "Đã thêm bàn", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Trả kết quả thành công về cho Fragment hiển thị
                    finish();
                    return;
                }
                String msg = response.body() != null && response.body().getMessage() != null
                        ? response.body().getMessage()
                        : ViewUtils.docLoiMayChu(response, "Không lưu được bàn (mã " + response.code() + ")");
                Log.w(TAG, "Lưu bàn bị từ chối: " + msg);
                new androidx.appcompat.app.AlertDialog.Builder(AddTableActivity.this)
                        .setTitle("Chưa lưu được bàn")
                        .setMessage(msg)
                        .setPositiveButton("Đã hiểu", null)
                        .show();
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                Log.e(TAG, "Lỗi kết nối API quản lý bàn: " + t.getMessage());
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(AddTableActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Kiểm duyệt ô Tên bàn ăn (không để trống). Trùng tên thì máy chủ báo.
     */
    private boolean validateName() {
        String val = TXTL_addtable_TableName.getEditText().getText().toString().trim();
        if (val.isEmpty()) {
            TXTL_addtable_TableName.setError(getResources().getString(R.string.not_empty));
            return false;
        } else {
            TXTL_addtable_TableName.setError(null);
            TXTL_addtable_TableName.setErrorEnabled(false);
            return true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu giữ ID bàn và ảnh mẫu đã chọn khi xoay màn hình
        outState.putInt("maban", maban);
        outState.putString("anhMauChon", anhMauChon);
        outState.putBoolean("boAnh", boAnh);
        outState.putParcelable("uriAnhChup", uriAnhChup);
    }
}
