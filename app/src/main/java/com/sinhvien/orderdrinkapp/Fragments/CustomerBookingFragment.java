package com.sinhvien.orderdrinkapp.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.sinhvien.orderdrinkapp.Activities.CustomerBookingActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.BookingPageResponse;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.BookingHistoryAdapter;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CustomerBookingFragment — màn hình "Lịch hẹn đặt bàn của bạn".
 *
 *
 * BA VẤN ĐỀ CỦA BẢN CŨ (đều liên quan tới bộ nhớ)
 * ================================================
 *
 * 1. RecyclerView KHÔNG tái sử dụng view.
 *    Bố cục cũ đặt nó trong NestedScrollView với `wrap_content` và
 *    `nestedScrollingEnabled="false"`. Hai thứ đó buộc RecyclerView cao
 *    bằng tổng mọi mục, nên nó dựng sẵn TẤT CẢ thẻ và giữ trong bộ nhớ.
 *    50 phiếu là 50 CardView cùng tồn tại. Đây là nguyên nhân nặng máy
 *    lớn nhất — lớn hơn nhiều so với kích thước dữ liệu tải về.
 *
 * 2. `onSaveInstanceState()` nhét CẢ danh sách vào Bundle dưới dạng JSON.
 *    Bundle đi qua Binder, mà Binder có trần khoảng 1 MB cho một giao
 *    dịch. Danh sách đủ dài là ném `TransactionTooLargeException` và làm
 *    sập ứng dụng khi xoay màn hình. Nay chỉ lưu bộ lọc đang chọn — vài
 *    byte — rồi tải lại từ máy chủ.
 *
 * 3. `onResume()` tải lại toàn bộ 50 phiếu mỗi lần quay về tab.
 *    Nay chỉ tải trang đầu (10 phiếu), phần còn lại tải dần khi cuộn.
 *
 *
 * VÌ SAO CUỘN VÔ HẠN CHỨ KHÔNG PHẢI NÚT CHUYỂN TRANG
 * ---------------------------------------------------
 * Màn hình chọn món (QĐ-055) dùng nút trước/sau vì ở đó người dùng đang
 * TÌM một món cụ thể và cần nhảy qua lại. Còn đây là dòng lịch sử, người
 * ta đọc từ mới nhất xuôi xuống. Cuộn tới đâu tải tới đó là đúng thói
 * quen hơn, và vẫn đạt mục tiêu: không bao giờ tải hết một lượt.
 */
public class CustomerBookingFragment extends Fragment {

    /** Số phiếu mỗi lượt tải. Đủ phủ hơn một màn hình để cuộn không giật. */
    private static final int PHIEU_MOI_TRANG = 10;

    /** Còn cách cuối danh sách bao nhiêu mục thì bắt đầu tải trang tiếp. */
    private static final int NGUONG_TAI_THEM = 3;

    /** Các bộ lọc trên dãy chip: {nhãn, chuỗi trạng thái gửi cho máy chủ}. */
    private static final int[] NHAN_LOC = {
            R.string.customer_filter_all,
            R.string.customer_filter_upcoming,
            R.string.customer_filter_seated,
            R.string.customer_filter_done,
            R.string.customer_filter_cancelled
    };
    private static final String[] GIA_TRI_LOC = {
            "",                       // Tất cả
            "pending,confirmed",      // Sắp tới
            "checked_in",             // Đã nhận bàn
            "completed",              // Hoàn thành
            "cancelled,overdue"       // Đã hủy / quá hạn
    };

    private MaterialButton btn_new_booking;
    private RecyclerView rv_active_bookings;
    private SwipeRefreshLayout swipe_lich_hen;
    private LinearLayout layout_chip_loc, layout_rong;
    private TextView txt_rong, txt_tong_so_phieu;
    private ProgressBar progress_lich_hen;

    private BookingHistoryAdapter adapter;
    private final List<BookingResponse> bookingList = new ArrayList<>();

    private int viTriLoc      = 0;
    private int trangHienTai  = 1;
    private int tongSoTrang   = 1;
    private int tongSoPhieu   = 0;
    private boolean dangTai   = false;

    private io.socket.client.Socket mSocket;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_booking, container, false);

        btn_new_booking    = view.findViewById(R.id.btn_new_booking);
        rv_active_bookings = view.findViewById(R.id.rv_active_bookings);
        swipe_lich_hen     = view.findViewById(R.id.swipe_lich_hen);
        layout_chip_loc    = view.findViewById(R.id.layout_chip_loc);
        layout_rong        = view.findViewById(R.id.layout_rong);
        txt_rong           = view.findViewById(R.id.txt_rong);
        txt_tong_so_phieu  = view.findViewById(R.id.txt_tong_so_phieu);
        progress_lich_hen  = view.findViewById(R.id.progress_lich_hen);

        // Chỉ khôi phục BỘ LỌC, không khôi phục dữ liệu.
        // Xem chú thích số 2 ở đầu lớp về TransactionTooLargeException.
        if (savedInstanceState != null) {
            viTriLoc = savedInstanceState.getInt("vi_tri_loc", 0);
        }

        adapter = new BookingHistoryAdapter(getContext(), bookingList, this::xacNhanHuyPhieu);
        rv_active_bookings.setLayoutManager(new LinearLayoutManager(getContext()));
        rv_active_bookings.setAdapter(adapter);
        rv_active_bookings.setHasFixedSize(true);

        ganCuonVoHan();
        veChipLoc();

        swipe_lich_hen.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        swipe_lich_hen.setOnRefreshListener(this::taiLaiTuDau);

        btn_new_booking.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CustomerBookingActivity.class)));

        return view;
    }

    /* ═══════════════════════════ Dãy chip lọc ═══════════════════════════ */

    private void veChipLoc() {
        layout_chip_loc.removeAllViews();
        for (int i = 0; i < NHAN_LOC.length; i++) {
            layout_chip_loc.addView(taoChip(i, getString(NHAN_LOC[i])));
        }
        capNhatChipDangChon();
    }

    private TextView taoChip(int viTri, String nhan) {
        TextView chip = new TextView(getContext());
        chip.setText(nhan);
        chip.setTextSize(13);
        chip.setSingleLine(true);
        chip.setBackgroundResource(R.drawable.bg_chip_danh_muc);
        chip.setTextColor(ContextCompat.getColorStateList(requireContext(),
                R.color.text_chip_danh_muc));

        float d = getResources().getDisplayMetrics().density;
        chip.setPadding((int) (14 * d), (int) (8 * d), (int) (14 * d), (int) (8 * d));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = (int) (8 * d);
        chip.setLayoutParams(lp);

        chip.setTag(viTri);
        chip.setOnClickListener(v -> {
            if (viTriLoc == viTri) return;
            viTriLoc = viTri;
            capNhatChipDangChon();
            taiLaiTuDau();
        });
        return chip;
    }

    private void capNhatChipDangChon() {
        for (int i = 0; i < layout_chip_loc.getChildCount(); i++) {
            View v = layout_chip_loc.getChildAt(i);
            v.setSelected(v.getTag() instanceof Integer && (Integer) v.getTag() == viTriLoc);
        }
    }

    /* ══════════════════════════ Cuộn vô hạn ═════════════════════════════ */

    private void ganCuonVoHan() {
        rv_active_bookings.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || dangTai || trangHienTai >= tongSoTrang) return;

                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;

                // Bắt đầu tải khi còn cách cuối vài mục, để dữ liệu về kịp
                // trước lúc người dùng cuộn tới nơi — không thấy khoảng trống.
                if (lm.findLastVisibleItemPosition() >= bookingList.size() - 1 - NGUONG_TAI_THEM) {
                    trangHienTai++;
                    taiPhieu(false);
                }
            }
        });
    }

    /* ═════════════════════════════ Tải dữ liệu ══════════════════════════ */

    private void taiLaiTuDau() {
        trangHienTai = 1;
        taiPhieu(true);
    }

    /**
     * @param tuDau true = thay cả danh sách (mở màn hình, đổi lọc, kéo làm
     *              mới); false = nối thêm vào cuối (cuộn tới đáy).
     */
    private void taiPhieu(boolean tuDau) {
        if (dangTai) return;
        dangTai = true;

        if (tuDau && !swipe_lich_hen.isRefreshing() && bookingList.isEmpty()) {
            progress_lich_hen.setVisibility(View.VISIBLE);
        }

        int makh = SessionManager.getMaNV(getContext());
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getBookingsPhanTrang(makh, GIA_TRI_LOC[viTriLoc],
                        trangHienTai, PHIEU_MOI_TRANG, 1)
                .enqueue(new Callback<BookingPageResponse>() {
            @Override
            public void onResponse(Call<BookingPageResponse> call, Response<BookingPageResponse> response) {
                if (!isAdded()) return;
                ketThucTai();

                if (!response.isSuccessful() || response.body() == null
                        || !"success".equals(response.body().getStatus())) {
                    if (tuDau) capNhatTrangThaiRong();
                    return;
                }

                BookingPageResponse body = response.body();
                tongSoPhieu = body.getTotal();
                tongSoTrang = body.getTotalPages();

                int viTriCu = bookingList.size();
                if (tuDau) {
                    bookingList.clear();
                    viTriCu = 0;
                }
                if (body.getData() != null) bookingList.addAll(body.getData());

                // notifyItemRangeInserted thay cho notifyDataSetChanged khi
                // nối thêm: chỉ vẽ phần mới, giữ nguyên vị trí cuộn và không
                // làm nháy những thẻ đang hiển thị.
                if (tuDau) {
                    adapter.notifyDataSetChanged();
                    rv_active_bookings.scrollToPosition(0);
                } else {
                    adapter.notifyItemRangeInserted(viTriCu, bookingList.size() - viTriCu);
                }

                capNhatTrangThaiRong();
            }

            @Override
            public void onFailure(Call<BookingPageResponse> call, Throwable t) {
                if (!isAdded()) return;
                ketThucTai();
                // Cuộn hụt một trang thì lùi lại để lần cuộn sau thử lại đúng trang đó.
                if (!tuDau && trangHienTai > 1) trangHienTai--;
                if (tuDau) capNhatTrangThaiRong();
                Toast.makeText(getContext(), "Không tải được lịch hẹn. Kéo xuống để thử lại.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ketThucTai() {
        dangTai = false;
        progress_lich_hen.setVisibility(View.GONE);
        swipe_lich_hen.setRefreshing(false);
    }

    private void capNhatTrangThaiRong() {
        boolean rong = bookingList.isEmpty();
        layout_rong.setVisibility(rong ? View.VISIBLE : View.GONE);

        // Chữ khác nhau: chưa từng đặt bàn là một chuyện, còn "mục này
        // trống nhưng mục khác có" lại là chuyện khác — nói rõ để người
        // dùng không tưởng dữ liệu bị mất.
        txt_rong.setText(viTriLoc == 0
                ? getString(R.string.customer_empty_bookings)
                : getString(R.string.customer_empty_filtered));

        txt_tong_so_phieu.setText(rong ? ""
                : getString(R.string.customer_booking_count, tongSoPhieu));
    }

    /* ═══════════════════════════ Hủy lịch hẹn ═══════════════════════════ */

    /**
     * Hỏi lại rồi gọi API hủy.
     *
     * Nút chỉ hiện với phiếu còn hủy được (xem `BookingHistoryAdapter.coTheHuy`),
     * nhưng vẫn kiểm tra lại thời gian ở đây: thẻ có thể đã nằm trên màn
     * hình vài phút và vừa vượt qua mốc một tiếng.
     */
    private void xacNhanHuyPhieu(BookingResponse phieu) {
        Date gioHen = adapter.doiGioHen(phieu.getThoigianhen());

        if (!BookingHistoryAdapter.coTheHuy(phieu.getTinhtrang(), gioHen)) {
            Toast.makeText(getContext(), R.string.customer_cancel_too_late, Toast.LENGTH_LONG).show();
            taiLaiTuDau();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.customer_cancel_title)
                .setMessage(getString(R.string.customer_cancel_message,
                        phieu.getTenBan() != null ? phieu.getTenBan() : "bàn đã đặt",
                        BookingHistoryAdapter.dinhDangGioHen(gioHen)))
                .setPositiveButton(R.string.customer_cancel_yes, (d, w) -> goiApiHuy(phieu))
                .setNegativeButton(R.string.customer_cancel_no, null)
                .show();
    }

    private void goiApiHuy(BookingResponse phieu) {
        androidx.appcompat.app.AlertDialog cho =
                com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(
                        requireContext(), "Đang hủy lịch hẹn...");
        cho.show();

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.updateBookingStatus(phieu.getMaDatBan(), "cancelled")
                .enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {
                if (cho.isShowing()) cho.dismiss();
                if (!isAdded()) return;

                boolean ok = response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus());

                if (ok) {
                    Toast.makeText(getContext(), R.string.customer_cancel_ok, Toast.LENGTH_SHORT).show();

                    // Báo cho nhân viên biết ngay, không đợi lượt quét định kỳ.
                    io.socket.client.Socket socket =
                            com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                    if (socket != null && socket.connected()) {
                        socket.emit("booking_status_updated");
                    }
                    taiLaiTuDau();
                } else {
                    // Máy chủ giải thích rõ vì sao từ chối và câu đó nằm ở
                    // errorBody() khi mã HTTP là 4xx — xem QĐ-046.
                    Toast.makeText(getContext(), docLoi(response), Toast.LENGTH_LONG).show();
                    taiLaiTuDau();
                }
            }

            @Override
            public void onFailure(Call<BookingResponse> call, Throwable t) {
                if (cho.isShowing()) cho.dismiss();
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String docLoi(Response<BookingResponse> response) {
        if (response.body() != null && response.body().getMessage() != null
                && !response.body().getMessage().isEmpty()) {
            return response.body().getMessage();
        }
        if (response.errorBody() != null) {
            try {
                org.json.JSONObject o = new org.json.JSONObject(response.errorBody().string());
                String m = o.optString("message", "");
                if (!m.isEmpty()) return m;
            } catch (Exception ignored) { }
        }
        return "Không thể hủy lịch hẹn";
    }

    /* ══════════════════════════ Vòng đời ════════════════════════════════ */

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // CHỈ lưu bộ lọc. Bản cũ lưu cả danh sách dưới dạng JSON, mà Bundle
        // đi qua Binder với trần khoảng 1 MB — danh sách dài là sập ứng dụng
        // với TransactionTooLargeException ngay khi xoay màn hình.
        outState.putInt("vi_tri_loc", viTriLoc);
    }

    @Override
    public void onResume() {
        super.onResume();
        taiLaiTuDau();
        setupSocketListener();
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
        if (!hidden) taiLaiTuDau();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mSocket != null) {
            mSocket.off("booking_status_updated");
        }
    }

    private void setupSocketListener() {
        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (mSocket == null) return;

        mSocket.on("booking_status_updated", args -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                taiLaiTuDau();
                Toast.makeText(getContext(),
                        "Lịch hẹn của bạn vừa được cập nhật", Toast.LENGTH_SHORT).show();
            });
        });
    }
}
