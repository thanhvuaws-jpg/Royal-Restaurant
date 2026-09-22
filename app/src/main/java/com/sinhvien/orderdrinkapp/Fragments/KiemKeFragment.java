package com.sinhvien.orderdrinkapp.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.KiemKeActionResponse;
import com.sinhvien.orderdrinkapp.Api.KhoResponse;
import com.sinhvien.orderdrinkapp.Api.NguyenLieuResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterKiemKe;
import com.sinhvien.orderdrinkapp.R;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * KiemKeFragment - Màn hình kiểm kê kho thực tế trên Android.
 * - Ô để trống nghĩa là không đếm, khác số 0 (Requirement 5.5).
 * - Hiển thị danh sách chênh lệch chi tiết sau khi ghi nhận (Requirement 5.1, 5.2, 5.3).
 */
public class KiemKeFragment extends Fragment {

    private ImageButton btnBack;
    private EditText edtGhiChu;
    private RecyclerView rcvKiemKe;
    private ProgressBar progressBar;
    private Button btnGuiKiemKe;

    private AdapterKiemKe adapter;
    private final DecimalFormat df = new DecimalFormat("#,##0.##");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_kiem_ke, container, false);

        btnBack = v.findViewById(R.id.btn_back_kiem_ke);
        edtGhiChu = v.findViewById(R.id.edt_ghi_chu_kiem_ke);
        rcvKiemKe = v.findViewById(R.id.rcv_kiem_ke);
        progressBar = v.findViewById(R.id.progress_loading_kiem_ke);
        btnGuiKiemKe = v.findViewById(R.id.btn_gui_kiem_ke);

        rcvKiemKe.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvKiemKe.setHasFixedSize(true);
        adapter = new AdapterKiemKe(getContext());
        rcvKiemKe.setAdapter(adapter);

        btnBack.setOnClickListener(view -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnGuiKiemKe.setOnClickListener(view -> submitKiemKe());

        loadNguyenLieuList();

        return v;
    }

    private void loadNguyenLieuList() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getApiService();
        api.getKhoDanhSach(1, 100, null, null, null).enqueue(new Callback<KhoResponse>() {
            @Override
            public void onResponse(Call<KhoResponse> call, Response<KhoResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<NguyenLieuResponse> list = response.body().getDanhSach();
                    adapter.setData(list);
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Không thể tải danh sách nguyên liệu", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<KhoResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void submitKiemKe() {
        Map<Integer, Double> thucTeMap = adapter.getThucTeMap();
        if (thucTeMap == null || thucTeMap.isEmpty()) {
            Toast.makeText(getContext(), "Chưa có nguyên liệu nào được đếm (hãy nhập số lượng vào ô tương ứng hoặc nhập 0 nếu đã hết hàng)", Toast.LENGTH_LONG).show();
            return;
        }

        List<Map<String, Object>> listDong = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : thucTeMap.entrySet()) {
            Map<String, Object> d = new HashMap<>();
            d.put("manl", entry.getKey());
            d.put("ton_thucte", entry.getValue());
            listDong.add(d);
        }

        String dongJson = new Gson().toJson(listDong);
        String ghiChu = edtGhiChu.getText().toString().trim();

        btnGuiKiemKe.setEnabled(false);
        btnGuiKiemKe.setText("Đang ghi nhận...");

        ApiService api = ApiClient.getApiService();
        api.kiemKeKho(dongJson, ghiChu).enqueue(new Callback<KiemKeActionResponse>() {
            @Override
            public void onResponse(Call<KiemKeActionResponse> call, Response<KiemKeActionResponse> response) {
                btnGuiKiemKe.setEnabled(true);
                btnGuiKiemKe.setText("LƯU KẾT QUẢ KIỂM KÊ");

                if (response.isSuccessful() && response.body() != null) {
                    KiemKeActionResponse res = response.body();
                    showKetQuaDialog(res);
                } else {
                    String errorMsg = "Không thể ghi nhận kiểm kê";
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
                    if (getContext() != null) {
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<KiemKeActionResponse> call, Throwable t) {
                btnGuiKiemKe.setEnabled(true);
                btnGuiKiemKe.setText("LƯU KẾT QUẢ KIỂM KÊ");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showKetQuaDialog(KiemKeActionResponse res) {
        StringBuilder sb = new StringBuilder();
        sb.append(res.getMessage()).append("\n\n");

        List<KiemKeActionResponse.DongKiemKeLech> chiTiet = res.getChiTiet();
        if (chiTiet != null && !chiTiet.isEmpty()) {
            sb.append("Chi tiết chênh lệch:\n");
            for (KiemKeActionResponse.DongKiemKeLech row : chiTiet) {
                String dau = row.getChenhLech() > 0 ? "+" : "";
                sb.append("• ").append(row.getTenNL()).append(":\n")
                  .append("  Sổ: ").append(df.format(row.getTonSo())).append(" ").append(row.getDonVi())
                  .append("  ->  Thực tế: ").append(df.format(row.getTonThucTe())).append(" ").append(row.getDonVi())
                  .append("  (Lệch: ").append(dau).append(df.format(row.getChenhLech())).append(" ").append(row.getDonVi()).append(")\n\n");
            }
        } else {
            sb.append("Tất cả các nguyên liệu được đếm đều khớp hoàn toàn với số liệu sổ kho!");
        }

        if (getContext() != null) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Kết quả kiểm kê kho")
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("Đóng", (dialog, which) -> {
                        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                            getParentFragmentManager().popBackStack();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }
}
