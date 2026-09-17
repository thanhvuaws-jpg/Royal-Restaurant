package com.sinhvien.orderdrinkapp.Fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.DiemDanhResponse;
import com.sinhvien.orderdrinkapp.Api.DoiVoucherResponse;
import com.sinhvien.orderdrinkapp.Api.LoyaltyResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.MaCuaToiAdapter;
import com.sinhvien.orderdrinkapp.CustomAdapter.ONgayAdapter;
import com.sinhvien.orderdrinkapp.CustomAdapter.VoucherDoiAdapter;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DiemDanhFragment — màn hình "Điểm danh & Quà tặng".
 *
 * Dựng theo bản thiết kế người dùng cung cấp: bảng 7 ô điểm danh, ô số
 * điểm tích lũy, thanh tiến trình 7 ngày, và hai nút hành động.
 *
 *
 * MỘT KHU VỰC, HAI DANH SÁCH
 * ---------------------------
 * Phần dưới màn hình đổi qua lại giữa "Mã của tôi" và "Kho phiếu giảm giá"
 * tùy nút nào vừa được bấm, thay vì hiện cả hai chồng nhau.
 *
 * Lý do: đây là hai việc khác nhau về mục đích. Khách vào để LẤY MÃ ĐỌC
 * CHO NHÂN VIÊN (thường xuyên), hoặc để TIÊU ĐIỂM (thỉnh thoảng). Hiện cả
 * hai thì việc thường xuyên bị đẩy xuống dưới việc hiếm khi làm.
 *
 *
 * MỘT LỜI GỌI CHO CẢ MÀN HÌNH
 * ----------------------------
 * `loyalty_home.php` trả về mọi thứ cùng lúc, kể cả việc cấp quà tri ân
 * tuần nếu tuần đó khách chưa nhận. Nên chỉ cần gọi lại đúng một endpoint
 * sau mỗi hành động là toàn bộ màn hình đồng bộ.
 */
public class DiemDanhFragment extends Fragment {

    /** Bảng điểm danh xếp 3 cột; ô mốc ngày 7 trải hết hàng cuối. */
    private static final int SO_COT = 3;

    private RecyclerView rv_o_ngay, rv_danh_sach;
    private TextView txt_so_diem, txt_goi_y_doi, txt_chuoi, txt_hang,
                     txt_tieu_de_ds, txt_dem_ds, txt_rong_ds;
    private ProgressBar progress_chuoi;
    private MaterialButton btn_diem_danh, btn_doi_phieu;

    private ONgayAdapter oNgayAdapter;
    private VoucherDoiAdapter voucherAdapter;
    private MaCuaToiAdapter maAdapter;

    /** false = đang xem "Mã của tôi"; true = đang xem "Kho phiếu". */
    private boolean dangXemKhoPhieu = false;

    private LoyaltyResponse duLieu;
    private boolean dangTai = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diem_danh, container, false);

        rv_o_ngay      = view.findViewById(R.id.rv_o_ngay);
        rv_danh_sach   = view.findViewById(R.id.rv_danh_sach);
        txt_so_diem    = view.findViewById(R.id.txt_so_diem);
        txt_goi_y_doi  = view.findViewById(R.id.txt_goi_y_doi);
        txt_chuoi      = view.findViewById(R.id.txt_chuoi);
        txt_hang       = view.findViewById(R.id.txt_hang);
        txt_tieu_de_ds = view.findViewById(R.id.txt_tieu_de_ds);
        txt_dem_ds     = view.findViewById(R.id.txt_dem_ds);
        txt_rong_ds    = view.findViewById(R.id.txt_rong_ds);
        progress_chuoi = view.findViewById(R.id.progress_chuoi);
        btn_diem_danh  = view.findViewById(R.id.btn_diem_danh);
        btn_doi_phieu  = view.findViewById(R.id.btn_doi_phieu);

        if (savedInstanceState != null) {
            dangXemKhoPhieu = savedInstanceState.getBoolean("kho_phieu", false);
        }

        /* ─────────────── Bảng 7 ô ─────────────── */

        oNgayAdapter = new ONgayAdapter(getContext());
        GridLayoutManager glm = new GridLayoutManager(getContext(), SO_COT);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return oNgayAdapter.soCot(position, SO_COT);
            }
        });
        rv_o_ngay.setLayoutManager(glm);
        rv_o_ngay.setAdapter(oNgayAdapter);
        rv_o_ngay.addItemDecoration(new KhoangCachO(
                (int) (6 * getResources().getDisplayMetrics().density)));

        /* ─────────────── Danh sách dưới ─────────────── */

        voucherAdapter = new VoucherDoiAdapter(getContext(), this::xacNhanDoi);
        maAdapter      = new MaCuaToiAdapter(getContext());
        rv_danh_sach.setLayoutManager(new LinearLayoutManager(getContext()));
        rv_danh_sach.setNestedScrollingEnabled(false);

        btn_diem_danh.setOnClickListener(v -> goiDiemDanh());
        btn_doi_phieu.setOnClickListener(v -> {
            dangXemKhoPhieu = !dangXemKhoPhieu;
            veDanhSach();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        taiDuLieu();
    }

    /**
     * Nạp lại khi màn hình được hiện trở lại.
     *
     * VÌ SAO KHÔNG ĐỦ NẾU CHỈ CÓ onResume()
     * --------------------------------------
     * `CustomerHomeActivity.navigateTo()` chuyển tab bằng `hide()`/`show()`
     * chứ không thay Fragment. Cách đó giữ được trạng thái từng màn hình,
     * nhưng Fragment bị ẩn KHÔNG bị dừng — nên khi hiện lại, `onResume()`
     * KHÔNG chạy. Kết quả: dữ liệu đứng yên từ lần đầu mở tab.
     *
     * Đã đo được: đặt điểm của khách thành 400 trong CSDL, đổi tab đi rồi
     * quay lại vẫn thấy 10; tắt hẳn app mở lại mới thấy 400.
     *
     * `onHiddenChanged` là callback dành riêng cho hide/show. Giữ cả hai:
     * onResume lo lần mở đầu tiên và lúc quay lại từ app khác,
     * onHiddenChanged lo việc đổi tab.
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) taiDuLieu();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("kho_phieu", dangXemKhoPhieu);
    }

    /* ═══════════════════════════ Tải dữ liệu ════════════════════════════ */

    private void taiDuLieu() {
        if (dangTai) return;
        dangTai = true;

        int makh = SessionManager.getMaNV(getContext());
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.layDuLieuDiemDanh(makh).enqueue(new Callback<LoyaltyResponse>() {
            @Override
            public void onResponse(Call<LoyaltyResponse> call, Response<LoyaltyResponse> response) {
                dangTai = false;
                if (!isAdded()) return;

                if (!response.isSuccessful() || response.body() == null
                        || !"success".equals(response.body().getStatus())) {
                    Toast.makeText(getContext(), "Không tải được dữ liệu điểm danh",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                duLieu = response.body();
                ve();

                // Quà tri ân tuần vừa được máy chủ cấp trong chính lời gọi này.
                if (duLieu.getQuaTuanMoi() != null) {
                    hienQuaTuan(duLieu.getQuaTuanMoi());
                }
            }

            @Override
            public void onFailure(Call<LoyaltyResponse> call, Throwable t) {
                dangTai = false;
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* ═════════════════════════════ Vẽ giao diện ═════════════════════════ */

    private void ve() {
        if (duLieu == null) return;

        oNgayAdapter.capNhat(duLieu.getONgay());

        txt_so_diem.setText(String.valueOf(duLieu.getDiem()));

        // Gợi ý cụ thể "đổi được mấy phiếu" hữu ích hơn con số điểm trần:
        // nó nói cho khách biết điểm của họ ĐỔI ĐƯỢC GÌ.
        int soDoiDuoc = 0;
        if (duLieu.getVoucher() != null) {
            for (LoyaltyResponse.Voucher v : duLieu.getVoucher()) {
                if (v.isDuDiem() && v.isDuHang()) soDoiDuoc++;
            }
        }
        txt_goi_y_doi.setText(soDoiDuoc > 0
                ? getString(R.string.dd_doi_duoc, soDoiDuoc)
                : getString(R.string.dd_chua_du_doi));

        progress_chuoi.setProgress(duLieu.getChuoi() % 7 == 0 && duLieu.getChuoi() > 0
                ? 7 : duLieu.getChuoi() % 7);
        txt_chuoi.setText(getString(R.string.dd_chuoi_hien_tai,
                duLieu.getChuoi() % 7 == 0 && duLieu.getChuoi() > 0 ? 7 : duLieu.getChuoi() % 7));
        txt_hang.setText(duLieu.getHangTen());

        /* Nút điểm danh */
        if (duLieu.isDaDiemDanh()) {
            btn_diem_danh.setEnabled(false);
            btn_diem_danh.setText(R.string.dd_nut_da_diem_danh);
            btn_diem_danh.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C7BDB6")));
        } else {
            btn_diem_danh.setEnabled(true);
            btn_diem_danh.setText(R.string.dd_nut_diem_danh);
            btn_diem_danh.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#795548")));
        }

        veDanhSach();
    }

    private void veDanhSach() {
        if (duLieu == null) return;

        if (dangXemKhoPhieu) {
            txt_tieu_de_ds.setText(R.string.dd_kho_phieu);
            btn_doi_phieu.setText(R.string.dd_nut_xem_ma);
            rv_danh_sach.setAdapter(voucherAdapter);
            voucherAdapter.capNhat(duLieu.getVoucher(), duLieu.getDiem());

            int n = duLieu.getVoucher() != null ? duLieu.getVoucher().size() : 0;
            txt_dem_ds.setText(n > 0 ? n + " phiếu" : "");
            txt_rong_ds.setText(R.string.dd_rong_voucher);
            txt_rong_ds.setVisibility(n == 0 ? View.VISIBLE : View.GONE);

        } else {
            txt_tieu_de_ds.setText(R.string.dd_ma_cua_toi);
            btn_doi_phieu.setText(R.string.dd_nut_doi_phieu);
            rv_danh_sach.setAdapter(maAdapter);
            maAdapter.capNhat(duLieu.getMaCuaToi());

            int n = duLieu.getMaCuaToi() != null ? duLieu.getMaCuaToi().size() : 0;
            txt_dem_ds.setText(n > 0 ? n + " mã" : "");
            txt_rong_ds.setText(R.string.dd_rong_ma);
            txt_rong_ds.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        }
    }

    /* ═══════════════════════════ Hành động ══════════════════════════════ */

    private void goiDiemDanh() {
        btn_diem_danh.setEnabled(false);   // chặn bấm đúp ngay ở giao diện

        int makh = SessionManager.getMaNV(getContext());
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.diemDanh(makh).enqueue(new Callback<DiemDanhResponse>() {
            @Override
            public void onResponse(Call<DiemDanhResponse> call, Response<DiemDanhResponse> response) {
                if (!isAdded()) return;

                boolean ok = response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus());

                if (ok) {
                    DiemDanhResponse r = response.body();
                    if (r.isLaMoc()) {
                        // Mốc trọn tuần đáng được nhấn mạnh hơn một Toast.
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Trọn 7 ngày!")
                                .setMessage(r.getMessage())
                                .setPositiveButton("Tuyệt!", null)
                                .show();
                    } else {
                        Toast.makeText(getContext(), r.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), docLoi(response), Toast.LENGTH_LONG).show();
                }

                taiDuLieu();   // nạp lại để bảng ô và số điểm khớp máy chủ
            }

            @Override
            public void onFailure(Call<DiemDanhResponse> call, Throwable t) {
                if (!isAdded()) return;
                btn_diem_danh.setEnabled(true);
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void xacNhanDoi(LoyaltyResponse.Voucher v) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dd_nut_doi_phieu)
                .setMessage(getString(R.string.dd_xac_nhan_doi, v.getDiemDoi(), v.getTen()))
                .setPositiveButton(R.string.dd_xac_nhan_doi_ok, (d, w) -> goiDoiVoucher(v))
                .setNegativeButton("Để sau", null)
                .show();
    }

    private void goiDoiVoucher(LoyaltyResponse.Voucher v) {
        int makh = SessionManager.getMaNV(getContext());
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.doiVoucher(makh, v.getMaVoucher()).enqueue(new Callback<DoiVoucherResponse>() {
            @Override
            public void onResponse(Call<DoiVoucherResponse> call, Response<DoiVoucherResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {

                    DoiVoucherResponse r = response.body();
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Đổi thành công")
                            .setMessage(r.getTen() + "\n\nMã của bạn:\n" + r.getMaCode()
                                    + "\n\nĐọc mã này cho nhân viên khi thanh toán.")
                            .setPositiveButton("Xem mã của tôi", (d, w) -> {
                                dangXemKhoPhieu = false;
                                taiDuLieu();
                            })
                            .setNegativeButton("Đóng", null)
                            .show();

                    // Nạp lại ngay cả khi người dùng bấm "Đóng": số điểm đã
                    // đổi khác rồi, để nguyên là hiển thị sai.
                    taiDuLieu();

                } else {
                    Toast.makeText(getContext(), docLoi(response), Toast.LENGTH_LONG).show();
                    taiDuLieu();
                }
            }

            @Override
            public void onFailure(Call<DoiVoucherResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hienQuaTuan(LoyaltyResponse.QuaTuan qua) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dd_qua_tuan_moi)
                .setMessage(qua.getTen() + "\n" + (qua.getMoTa() != null ? qua.getMoTa() : "")
                        + "\n\nMã của bạn:\n" + qua.getMaCode())
                .setPositiveButton("Nhận quà", null)
                .show();
    }

    /**
     * Đọc thông báo lỗi máy chủ gửi kèm.
     *
     * Máy chủ trả 409 cho những trường hợp như "hôm nay đã điểm danh" hay
     * "chưa đủ điểm", và câu giải thích nằm ở errorBody() chứ không ở
     * body() — cùng lý do đã nêu ở QĐ-046.
     */
    private String docLoi(Response<?> response) {
        if (response.errorBody() != null) {
            try {
                org.json.JSONObject o = new org.json.JSONObject(response.errorBody().string());
                String m = o.optString("message", "");
                if (!m.isEmpty()) return m;
            } catch (Exception ignored) { }
        }
        return "Thao tác không thành công";
    }

    /** Khoảng cách đều giữa các ô trong lưới điểm danh. */
    private static class KhoangCachO extends RecyclerView.ItemDecoration {
        private final int cach;

        KhoangCachO(int cach) { this.cach = cach; }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            outRect.set(cach / 2, cach / 2, cach / 2, cach / 2);
        }
    }
}
