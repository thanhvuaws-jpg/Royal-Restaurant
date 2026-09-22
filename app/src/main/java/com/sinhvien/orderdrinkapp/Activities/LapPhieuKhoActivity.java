package com.sinhvien.orderdrinkapp.Activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.gson.Gson;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.KhoResponse;
import com.sinhvien.orderdrinkapp.Api.NguyenLieuResponse;
import com.sinhvien.orderdrinkapp.Api.PhieuKhoActionResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.AnhHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LapPhieuKhoActivity - Lập phiếu xuất kho hoặc phiếu hủy hàng trên Android.
 * - Cho phép thêm/bớt nhiều dòng hàng linh hoạt.
 * - Phiếu hủy hàng BẮT BUỘC có ảnh bằng chứng được nén <= 1MB (AnhHelper).
 * - Đọc thông báo lỗi nghiệp vụ từ errorBody().
 */
public class LapPhieuKhoActivity extends AppCompatActivity {

    private String loai = "xuat"; // "xuat" hoặc "huy"

    private Spinner spnLyDo;
    private TextView lblNguoiNhan;
    private EditText edtNguoiNhan;
    private EditText edtGhiChu;

    private LinearLayout layoutAnhBangChung;
    private Button btnChupAnh;
    private Button btnChonAnh;
    private ImageView imgPreview;

    private Button btnThemDong;
    private LinearLayout containerDongHang;
    private Button btnXacNhan;

    private String hinhAnhBase64 = "";
    private final List<NguyenLieuResponse> danhSachNguyenLieu = new ArrayList<>();
    private final List<View> danhSachViewDong = new ArrayList<>();

    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lap_phieu_kho);

        loai = getIntent().getStringExtra("loai");
        if (loai == null || (!loai.equals("xuat") && !loai.equals("huy"))) {
            loai = "xuat";
        }

        initLaunchers();
        initViews();
        loadDanhSachNguyenLieu();
    }

    private void initLaunchers() {
        // Chụp ảnh từ Camera
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) {
                hinhAnhBase64 = AnhHelper.nenVaChuyenBase64(bitmap);
                imgPreview.setImageBitmap(bitmap);
                imgPreview.setVisibility(View.VISIBLE);
            }
        });

        // Chọn ảnh từ Thư viện
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                Bitmap bitmap = AnhHelper.docBitmapTuUri(this, uri);
                if (bitmap != null) {
                    hinhAnhBase64 = AnhHelper.nenVaChuyenBase64(bitmap);
                    imgPreview.setImageBitmap(bitmap);
                    imgPreview.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_lap_phieu);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(loai.equals("huy") ? "Lập phiếu hủy hàng" : "Lập phiếu xuất kho");
        }

        spnLyDo = findViewById(R.id.spn_ly_do);
        lblNguoiNhan = findViewById(R.id.lbl_nguoi_nhan);
        edtNguoiNhan = findViewById(R.id.edt_nguoi_nhan);
        edtGhiChu = findViewById(R.id.edt_ghi_chu);

        layoutAnhBangChung = findViewById(R.id.layout_anh_bang_chung);
        btnChupAnh = findViewById(R.id.btn_chup_anh);
        btnChonAnh = findViewById(R.id.btn_chon_anh);
        imgPreview = findViewById(R.id.img_preview_bang_chung);

        btnThemDong = findViewById(R.id.btn_them_dong);
        containerDongHang = findViewById(R.id.container_dong_hang);
        btnXacNhan = findViewById(R.id.btn_xac_nhan_lap_phieu);

        setupLyDoSpinner();

        if (loai.equals("huy")) {
            lblNguoiNhan.setVisibility(View.GONE);
            edtNguoiNhan.setVisibility(View.GONE);
            layoutAnhBangChung.setVisibility(View.VISIBLE);
        } else {
            lblNguoiNhan.setVisibility(View.VISIBLE);
            edtNguoiNhan.setVisibility(View.VISIBLE);
            layoutAnhBangChung.setVisibility(View.GONE);
        }

        btnChupAnh.setOnClickListener(v -> cameraLauncher.launch(null));
        btnChonAnh.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnThemDong.setOnClickListener(v -> themDongHang(null, 1.0));
        btnXacNhan.setOnClickListener(v -> submitPhieu());
    }

    private void setupLyDoSpinner() {
        String[] lyDoArray;
        if (loai.equals("huy")) {
            lyDoArray = new String[]{"Hỏng hóc / ôi thiu", "Hết hạn sử dụng", "Rơi vỡ / đổ tràn", "Khác"};
        } else {
            lyDoArray = new String[]{"Giao cho bếp", "Sử dụng nội bộ", "Khác"};
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, lyDoArray);
        spnLyDo.setAdapter(adapter);
    }

    private String getSelectedLyDoKey() {
        int pos = spnLyDo.getSelectedItemPosition();
        if (loai.equals("huy")) {
            switch (pos) {
                case 0: return "hong";
                case 1: return "het_han";
                case 2: return "roi_vo";
                default: return "khac";
            }
        } else {
            switch (pos) {
                case 0: return "bep";
                case 1: return "noi_bo";
                default: return "khac";
            }
        }
    }

    private void loadDanhSachNguyenLieu() {
        ApiService api = ApiClient.getApiService();
        api.getKhoDanhSach(1, 100, null, null, null).enqueue(new Callback<KhoResponse>() {
            @Override
            public void onResponse(Call<KhoResponse> call, Response<KhoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NguyenLieuResponse> list = response.body().getDanhSach();
                    if (list != null) {
                        danhSachNguyenLieu.clear();
                        danhSachNguyenLieu.addAll(list);

                        // Mặc định thêm sẵn 1 dòng hàng đầu tiên
                        if (danhSachViewDong.isEmpty()) {
                            themDongHang(null, 1.0);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<KhoResponse> call, Throwable t) {
                Toast.makeText(LapPhieuKhoActivity.this, "Không thể tải danh sách nguyên liệu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void themDongHang(Integer defaultManl, double defaultSoluong) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_dong_lap_phieu, containerDongHang, false);
        Spinner spnNL = rowView.findViewById(R.id.spn_chon_nl);
        EditText edtSL = rowView.findViewById(R.id.edt_so_luong);
        TextView txtDonVi = rowView.findViewById(R.id.txt_don_vi);
        ImageButton btnXoa = rowView.findViewById(R.id.btn_xoa_dong);

        List<String> tenNLList = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < danhSachNguyenLieu.size(); i++) {
            NguyenLieuResponse nl = danhSachNguyenLieu.get(i);
            tenNLList.add(nl.getTenNL() + " (" + nl.getDonVi() + ")");
            if (defaultManl != null && nl.getMaNL() == defaultManl) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> nlAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tenNLList);
        spnNL.setAdapter(nlAdapter);
        if (!tenNLList.isEmpty()) {
            spnNL.setSelection(selectedIndex);
            txtDonVi.setText(danhSachNguyenLieu.get(selectedIndex).getDonVi());
        }

        spnNL.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < danhSachNguyenLieu.size()) {
                    txtDonVi.setText(danhSachNguyenLieu.get(position).getDonVi());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        edtSL.setText(defaultSoluong > 0 ? String.valueOf(defaultSoluong) : "1");

        btnXoa.setOnClickListener(v -> {
            containerDongHang.removeView(rowView);
            danhSachViewDong.remove(rowView);
        });

        containerDongHang.addView(rowView);
        danhSachViewDong.add(rowView);
    }

    private void submitPhieu() {
        if (danhSachViewDong.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất một dòng nguyên liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bắt buộc ảnh nếu là phiếu hủy
        if (loai.equals("huy") && (hinhAnhBase64 == null || hinhAnhBase64.trim().isEmpty())) {
            Toast.makeText(this, "Phiếu hủy hàng bắt buộc phải có ảnh bằng chứng!", Toast.LENGTH_LONG).show();
            return;
        }

        List<Map<String, Object>> listDong = new ArrayList<>();
        for (View rowView : danhSachViewDong) {
            Spinner spnNL = rowView.findViewById(R.id.spn_chon_nl);
            EditText edtSL = rowView.findViewById(R.id.edt_so_luong);

            int pos = spnNL.getSelectedItemPosition();
            if (pos < 0 || pos >= danhSachNguyenLieu.size()) continue;

            NguyenLieuResponse nl = danhSachNguyenLieu.get(pos);
            String slStr = edtSL.getText().toString().trim();
            double sl = 0.0;
            try {
                sl = Double.parseDouble(slStr);
            } catch (Exception ignored) {}

            if (sl <= 0) {
                Toast.makeText(this, "Số lượng của " + nl.getTenNL() + " phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> d = new HashMap<>();
            d.put("manl", nl.getMaNL());
            d.put("soluong", sl);
            listDong.add(d);
        }

        if (listDong.isEmpty()) {
            Toast.makeText(this, "Chưa có dòng hàng hợp lệ nào", Toast.LENGTH_SHORT).show();
            return;
        }

        String dongJson = new Gson().toJson(listDong);
        String lyDoKey = getSelectedLyDoKey();
        String nguoiNhan = edtNguoiNhan.getText().toString().trim();
        String ghiChu = edtGhiChu.getText().toString().trim();

        btnXacNhan.setEnabled(false);
        btnXacNhan.setText("Đang xử lý...");

        ApiService api = ApiClient.getApiService();
        api.lapPhieuKho(loai, dongJson, lyDoKey, nguoiNhan, null, null, ghiChu, hinhAnhBase64)
                .enqueue(new Callback<PhieuKhoActionResponse>() {
                    @Override
                    public void onResponse(Call<PhieuKhoActionResponse> call, Response<PhieuKhoActionResponse> response) {
                        btnXacNhan.setEnabled(true);
                        btnXacNhan.setText("XÁC NHẬN LẬP PHIẾU");

                        if (response.isSuccessful() && response.body() != null) {
                            String soPhieu = "";
                            if (response.body().getPhieu() != null) {
                                soPhieu = response.body().getPhieu().getSoPhieu();
                            }
                            String msg = "Đã lập phiếu thành công " + (soPhieu != null ? soPhieu : "");
                            Toast.makeText(LapPhieuKhoActivity.this, msg, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            // Đọc thông báo lỗi nghiệp vụ từ errorBody()
                            String errorMsg = "Không thể lập phiếu kho";
                            try {
                                if (response.errorBody() != null) {
                                    String errJson = response.errorBody().string();
                                    JSONObject errObj = new JSONObject(errJson);
                                    if (errObj.has("message")) {
                                        errorMsg = errObj.getString("message");
                                    }
                                }
                            } catch (Exception e) {
                                errorMsg += " (" + response.code() + ")";
                            }
                            Toast.makeText(LapPhieuKhoActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<PhieuKhoActionResponse> call, Throwable t) {
                        btnXacNhan.setEnabled(true);
                        btnXacNhan.setText("XÁC NHẬN LẬP PHIẾU");
                        Toast.makeText(LapPhieuKhoActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
