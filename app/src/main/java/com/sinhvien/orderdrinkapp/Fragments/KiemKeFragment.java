package com.sinhvien.orderdrinkapp.Fragments;

import androidx.appcompat.app.AlertDialog;
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
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
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
import com.sinhvien.orderdrinkapp.Utils.HopThoai;
import com.sinhvien.orderdrinkapp.Utils.ThongBao;

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

        // Yêu cầu 11: chỉ Quản lý gửi kiểm kê (kiểm kê ghi lại tồn kho).
        // Nút mở màn này đã ẩn với người khác; ở đây chỉ khóa nút gửi, vẫn
        // cho xem vì quyền 2 và 3 được đọc tồn.
        final boolean laQuanLy = SessionManager.isAdmin(requireContext());

        rcvKiemKe.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvKiemKe.setHasFixedSize(true);
        adapter = new AdapterKiemKe(getContext());
        rcvKiemKe.setAdapter(adapter);

        btnBack.setOnClickListener(view -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        if (laQuanLy) {
            btnGuiKiemKe.setOnClickListener(view -> submitKiemKe());
        } else {
            btnGuiKiemKe.setEnabled(false);
            btnGuiKiemKe.setText("CHỈ QUẢN LÝ ĐƯỢC KIỂM KÊ");
        }

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
                        ThongBao.hien(getContext(), "Không thể tải danh sách nguyên liệu", Toast.LENGTH_SHORT);
                    }
                }
            }

            @Override
            public void onFailure(Call<KhoResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                if (getContext() != null) {
                    ThongBao.hien(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT);
                }
            }
        });
    }

    /**
     * Bấm "LƯU KẾT QUẢ KIỂM KÊ": KHÔNG ghi ngay.
     *
     * Bước 1 hỏi máy chủ chênh lệch (xem_truoc, không ghi gì). Khớp sổ thì báo
     * khớp; có lệch thì hiện từng dòng lệch, đánh dấu lệch lớn, bắt nhập lý do
     * rồi mới điều chỉnh tồn (bước 2). Trước đây bấm là tồn bị ghi đè ngay —
     * nhập thử số lệch thì tồn "tự nhiên cộng vào" (báo ngày 22/10/2026).
     */
    private void submitKiemKe() {
        String oSai = adapter.tenOSaiDau();
        if (oSai != null) {
            ThongBao.hien(getContext(), "Số đếm của " + oSai + " không phải là số. Sửa lại hoặc để trống ô đó.", Toast.LENGTH_LONG);
            return;
        }
        Map<Integer, Double> thucTeMap = adapter.getThucTeMap();
        if (thucTeMap == null || thucTeMap.isEmpty()) {
            ThongBao.hien(getContext(), "Chưa có nguyên liệu nào được đếm (hãy nhập số lượng vào ô tương ứng hoặc nhập 0 nếu đã hết hàng)", Toast.LENGTH_LONG);
            return;
        }

        List<Map<String, Object>> listDong = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : thucTeMap.entrySet()) {
            Map<String, Object> d = new HashMap<>();
            d.put("manl", entry.getKey());
            d.put("ton_thucte", entry.getValue());
            listDong.add(d);
        }
        final String dongJson = new Gson().toJson(listDong);

        dangGui(true, "Đang so với sổ...");
        ApiClient.getApiService().kiemKeXemTruoc(dongJson).enqueue(new Callback<KiemKeActionResponse>() {
            @Override
            public void onResponse(Call<KiemKeActionResponse> call, Response<KiemKeActionResponse> response) {
                dangGui(false, null);
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    ThongBao.hien(getContext(), docLoi(response, "Không so được với sổ kho"), Toast.LENGTH_LONG);
                    return;
                }
                KiemKeActionResponse xem = response.body();
                if (xem.getSoLech() == 0) {
                    new HopThoai.Builder(requireContext())
                            .setLoai(HopThoai.Loai.XONG)
                            .setTitle("Khớp sổ")
                            .setMessage(xem.getSoDem() + " nguyên liệu đã đếm đều khớp sổ — không cần điều chỉnh tồn.")
                            .setPositiveButton("Đóng", (d, w) -> quayLai())
                            .show();
                    return;
                }
                hienXacNhan(xem, dongJson);
            }

            @Override
            public void onFailure(Call<KiemKeActionResponse> call, Throwable t) {
                dangGui(false, null);
                if (getContext() != null) {
                    ThongBao.hien(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT);
                }
            }
        });
    }

    /** Bước 2: liệt kê dòng lệch, bắt lý do, rồi mới điều chỉnh tồn. */
    private void hienXacNhan(KiemKeActionResponse xem, String dongJson) {
        StringBuilder sb = new StringBuilder("Tồn chưa bị sửa. Kiểm tra lại:\n\n");
        java.util.LinkedHashSet<String> goiY = new java.util.LinkedHashSet<>();
        for (KiemKeActionResponse.DongKiemKeLech row : xem.getChiTiet()) {
            String dau = row.getChenhLech() > 0 ? "+" : "−";
            sb.append("• ").append(row.getTenNL()).append(": sổ ").append(df.format(row.getTonSo()))
              .append(" → đếm ").append(df.format(row.getTonThucTe()))
              .append(" (").append(dau).append(df.format(Math.abs(row.getChenhLech()))).append(" ").append(row.getDonVi()).append(")");
            if (row.isLechLon()) sb.append("\n   ⚠ Lệch lớn — kiểm tra lại số đếm");
            sb.append("\n");
            if (!row.getGoiY().isEmpty()) goiY.add(row.getGoiY());
        }
        for (String g : goiY) sb.append("\n").append(g);

        com.google.android.material.textfield.TextInputLayout khungLyDo =
                new com.google.android.material.textfield.TextInputLayout(requireContext());
        khungLyDo.setHint("Lý do lệch (bắt buộc)");
        com.google.android.material.textfield.TextInputEditText oLyDo =
                new com.google.android.material.textfield.TextInputEditText(khungLyDo.getContext());
        oLyDo.setText(edtGhiChu.getText().toString().trim());
        khungLyDo.addView(oLyDo);

        new HopThoai.Builder(requireContext())
                .setLoai(xem.isCoLechLon() ? HopThoai.Loai.CANH : HopThoai.Loai.TIN)
                .setTitle(xem.getSoLech() + " nguyên liệu lệch sổ")
                .setMessage(sb.toString().trim())
                .setView(khungLyDo)
                .setNegativeButton("Đếm lại", null)
                .setPositiveButton("Điều chỉnh tồn", (d, w) -> {
                    String lyDo = oLyDo.getText() != null ? oLyDo.getText().toString().trim() : "";
                    if (lyDo.length() < 3) {
                        ThongBao.hien(getContext(), "Nhập lý do lệch trước khi điều chỉnh tồn.", Toast.LENGTH_LONG);
                        edtGhiChu.setText(lyDo);
                        hienXacNhan(xem, dongJson);
                        return;
                    }
                    edtGhiChu.setText(lyDo);
                    ghiDieuChinh(dongJson, lyDo);
                })
                .setCancelable(false)
                .show();
    }

    private void ghiDieuChinh(String dongJson, String lyDo) {
        dangGui(true, "Đang ghi nhận...");
        ApiClient.getApiService().kiemKeKho(dongJson, lyDo, "1").enqueue(new Callback<KiemKeActionResponse>() {
            @Override
            public void onResponse(Call<KiemKeActionResponse> call, Response<KiemKeActionResponse> response) {
                dangGui(false, null);
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    showKetQuaDialog(response.body());
                } else {
                    ThongBao.hien(getContext(), docLoi(response, "Không thể ghi nhận kiểm kê"), Toast.LENGTH_LONG);
                }
            }

            @Override
            public void onFailure(Call<KiemKeActionResponse> call, Throwable t) {
                dangGui(false, null);
                if (getContext() != null) {
                    ThongBao.hien(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT);
                }
            }
        });
    }

    private void dangGui(boolean dang, String chu) {
        btnGuiKiemKe.setEnabled(!dang);
        btnGuiKiemKe.setText(dang ? chu : "LƯU KẾT QUẢ KIỂM KÊ");
    }

    private static String docLoi(Response<?> response, String macDinh) {
        try {
            if (response.errorBody() != null) {
                JSONObject errObj = new JSONObject(response.errorBody().string());
                if (errObj.has("message")) return errObj.getString("message");
            }
        } catch (Exception e) {
            return macDinh + " (" + response.code() + ")";
        }
        return macDinh;
    }

    private void quayLai() {
        if (isAdded() && getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }

    private void showKetQuaDialog(KiemKeActionResponse res) {
        StringBuilder sb = new StringBuilder();
        sb.append(res.getMessage()).append("\n\n");

        List<KiemKeActionResponse.DongKiemKeLech> chiTiet = res.getChiTiet();
        if (chiTiet != null && !chiTiet.isEmpty()) {
            sb.append("Đã điều chỉnh tồn:\n");
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
            new HopThoai.Builder(getContext())
                    .setLoai(HopThoai.Loai.XONG)
                    .setTitle("Kết quả kiểm kê kho")
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("Đóng", (dialog, which) -> quayLai())
                    .setCancelable(false)
                    .show();
        }
    }
}
