package com.sinhvien.orderdrinkapp.Fragments;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.BookingPageResponse;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.Api.CustomerProfileResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.BookingHistoryAdapter;
import com.sinhvien.orderdrinkapp.CustomAdapter.HoSoHeaderAdapter;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CustomerProfileFragment — màn hình "Lịch sử & chi tiêu".
 *
 *
 * BỐN VẤN ĐỀ CỦA BẢN CŨ
 * =====================
 *
 * 1. Giống hệt QĐ-056: `RecyclerView` nằm trong `NestedScrollView` với
 *    `wrap_content` + `nestedScrollingEnabled="false"`, nên nó dựng sẵn
 *    TOÀN BỘ thẻ và giữ trong bộ nhớ, không tái sử dụng view nào.
 *
 * 2. `onSaveInstanceState()` nhét cả danh sách vào Bundle dưới dạng JSON —
 *    nguy cơ `TransactionTooLargeException` khi xoay màn hình.
 *
 * 3. `get_customer_profile.php` trả về TOÀN BỘ phiếu đặt bàn, không có
 *    giới hạn nào. Còn tệ hơn `get_bookings.php` vốn ít ra có `LIMIT 50`.
 *
 * 4. Con số chi tiêu SAI — xem chú thích trong `get_customer_profile.php`.
 *    Nó cộng đơn theo `DONDAT.MANV` (nhân viên tạo đơn) thay vì theo khách
 *    hàng thật sự, cho ra 720.000đ trong khi số đúng là 3.330.000đ.
 *
 *
 * CÁCH DỰNG LẠI
 * =============
 * Một `RecyclerView` duy nhất chiếm trọn màn hình, dùng `ConcatAdapter`
 * ghép hai adapter:
 *
 *     [HoSoHeaderAdapter]  — 1 mục: thẻ hồ sơ, ô thống kê, chip lọc
 *     [BookingHistoryAdapter] — danh sách lịch hẹn, tải dần khi cuộn
 *
 * Nhờ vậy phần đầu vẫn cuộn theo như trước, mà RecyclerView giữ nguyên
 * cơ chế tái sử dụng view.
 *
 * Hai nguồn dữ liệu, gọi song song vì không phụ thuộc nhau:
 *   - `get_customer_profile.php` : hồ sơ + thống kê (một lần mỗi lần mở)
 *   - `get_bookings.php`         : danh sách, 10 phiếu mỗi lượt
 */
public class CustomerProfileFragment extends Fragment
        implements HoSoHeaderAdapter.OnHeaderSan {

    private static final int PHIEU_MOI_TRANG = 10;
    private static final int NGUONG_TAI_THEM = 3;

    private static final int[] NHAN_LOC = {
            R.string.customer_filter_all,
            R.string.customer_filter_upcoming,
            R.string.customer_filter_seated,
            R.string.customer_filter_done,
            R.string.customer_filter_cancelled
    };
    private static final String[] GIA_TRI_LOC = {
            "", "pending,confirmed", "checked_in", "completed", "cancelled,overdue"
    };

    private RecyclerView rv_history_bookings;
    private SwipeRefreshLayout swipe_ho_so;
    private ProgressBar progress_ho_so;

    private HoSoHeaderAdapter headerAdapter;
    private BookingHistoryAdapter adapter;
    private final List<BookingResponse> bookingList = new ArrayList<>();

    private int viTriLoc     = 0;
    private int trangHienTai = 1;
    private int tongSoTrang  = 1;
    private boolean dangTai  = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_profile, container, false);

        rv_history_bookings = view.findViewById(R.id.rv_history_bookings);
        swipe_ho_so         = view.findViewById(R.id.swipe_ho_so);
        progress_ho_so      = view.findViewById(R.id.progress_ho_so);

        // Chỉ khôi phục bộ lọc, không khôi phục dữ liệu (xem vấn đề 2 ở trên).
        if (savedInstanceState != null) {
            viTriLoc = savedInstanceState.getInt("vi_tri_loc", 0);
        }

        headerAdapter = new HoSoHeaderAdapter(getContext(), this);
        adapter = new BookingHistoryAdapter(getContext(), bookingList);

        rv_history_bookings.setLayoutManager(new LinearLayoutManager(getContext()));
        rv_history_bookings.setAdapter(new ConcatAdapter(headerAdapter, adapter));

        ganCuonVoHan();

        swipe_ho_so.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        swipe_ho_so.setOnRefreshListener(this::taiLaiTatCa);

        // Tên hiển thị ngay từ phiên đã lưu, không phải chờ mạng.
        headerAdapter.capNhat(SessionManager.getFullName(getContext()), "", "", 0, 0, 0, 0);

        return view;
    }

    /* ═══════════════════ Dãy chip lọc (nằm trong header) ════════════════ */

    /**
     * Được `HoSoHeaderAdapter` gọi lại mỗi lần mục header được vẽ.
     *
     * Chip nằm trong header nên có thể bị dựng lại bất cứ lúc nào — vì vậy
     * luôn xóa sạch rồi dựng lại, và đọc `viTriLoc` từ Fragment để chip
     * đang chọn không bị mất sau khi cuộn đi rồi cuộn về.
     */
    @Override
    public void dungChipLoc(LinearLayout khungChip) {
        khungChip.removeAllViews();
        for (int i = 0; i < NHAN_LOC.length; i++) {
            khungChip.addView(taoChip(khungChip, i, getString(NHAN_LOC[i])));
        }
    }

    private TextView taoChip(LinearLayout khungChip, int viTri, String nhan) {
        TextView chip = new TextView(getContext());
        chip.setText(nhan);
        chip.setTextSize(13);
        chip.setSingleLine(true);
        chip.setBackgroundResource(R.drawable.bg_chip_danh_muc);
        chip.setTextColor(ContextCompat.getColorStateList(requireContext(),
                R.color.text_chip_danh_muc));
        chip.setSelected(viTri == viTriLoc);

        float d = getResources().getDisplayMetrics().density;
        chip.setPadding((int) (14 * d), (int) (8 * d), (int) (14 * d), (int) (8 * d));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = (int) (8 * d);
        chip.setLayoutParams(lp);

        chip.setOnClickListener(v -> {
            if (viTriLoc == viTri) return;
            viTriLoc = viTri;
            for (int i = 0; i < khungChip.getChildCount(); i++) {
                khungChip.getChildAt(i).setSelected(i == viTriLoc);
            }
            trangHienTai = 1;
            taiDanhSach(true);
        });
        return chip;
    }

    /* ══════════════════════════ Cuộn vô hạn ═════════════════════════════ */

    private void ganCuonVoHan() {
        rv_history_bookings.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || dangTai || trangHienTai >= tongSoTrang) return;

                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;

                // +1 vì mục header cũng đếm vào vị trí của ConcatAdapter.
                int tongMuc = bookingList.size() + 1;
                if (lm.findLastVisibleItemPosition() >= tongMuc - 1 - NGUONG_TAI_THEM) {
                    trangHienTai++;
                    taiDanhSach(false);
                }
            }
        });
    }

    /* ═════════════════════════════ Tải dữ liệu ══════════════════════════ */

    private void taiLaiTatCa() {
        trangHienTai = 1;
        taiHoSo();
        taiDanhSach(true);
    }

    /** Hồ sơ + thống kê. Gọi riêng vì không đổi khi người dùng lọc. */
    private void taiHoSo() {
        int makh = SessionManager.getMaNV(getContext());
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getCustomerProfile(makh).enqueue(new Callback<CustomerProfileResponse>() {
            @Override
            public void onResponse(Call<CustomerProfileResponse> call,
                                   Response<CustomerProfileResponse> response) {
                if (!isAdded()) return;
                swipe_ho_so.setRefreshing(false);

                if (!response.isSuccessful() || response.body() == null
                        || !"success".equals(response.body().getStatus())) {
                    return;
                }

                CustomerProfileResponse p = response.body();
                long chiTieu = 0;
                try {
                    if (p.getSpending() != null && !p.getSpending().isEmpty()) {
                        chiTieu = Long.parseLong(p.getSpending());
                    }
                } catch (NumberFormatException ignored) { }

                // Ưu tiên tên từ máy chủ; nếu thiếu thì dùng tên trong phiên.
                String ten = (p.getHoTen() != null && !p.getHoTen().isEmpty())
                        ? p.getHoTen() : SessionManager.getFullName(getContext());

                headerAdapter.capNhat(ten, p.getSdt(), p.getEmail(), chiTieu,
                        p.getSoLanDat(), p.getSoHoanThanh(), p.getSoDaHuy());
            }

            @Override
            public void onFailure(Call<CustomerProfileResponse> call, Throwable t) {
                if (!isAdded()) return;
                swipe_ho_so.setRefreshing(false);
            }
        });
    }

    /** Một trang lịch hẹn theo bộ lọc đang chọn. */
    private void taiDanhSach(boolean tuDau) {
        if (dangTai) return;
        dangTai = true;

        if (tuDau && !swipe_ho_so.isRefreshing() && bookingList.isEmpty()) {
            progress_ho_so.setVisibility(View.VISIBLE);
        }

        int makh = SessionManager.getMaNV(getContext());
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getBookingsPhanTrang(makh, GIA_TRI_LOC[viTriLoc],
                        trangHienTai, PHIEU_MOI_TRANG, 1)
                .enqueue(new Callback<BookingPageResponse>() {
            @Override
            public void onResponse(Call<BookingPageResponse> call,
                                   Response<BookingPageResponse> response) {
                if (!isAdded()) return;
                ketThucTai();

                if (!response.isSuccessful() || response.body() == null
                        || !"success".equals(response.body().getStatus())) {
                    return;
                }

                BookingPageResponse body = response.body();
                tongSoTrang = body.getTotalPages();
                headerAdapter.capNhatTongSoPhieu(body.getTotal());

                int viTriCu = bookingList.size();
                if (tuDau) {
                    bookingList.clear();
                    viTriCu = 0;
                }
                if (body.getData() != null) bookingList.addAll(body.getData());

                if (tuDau) {
                    adapter.notifyDataSetChanged();
                } else {
                    adapter.notifyItemRangeInserted(viTriCu, bookingList.size() - viTriCu);
                }
            }

            @Override
            public void onFailure(Call<BookingPageResponse> call, Throwable t) {
                if (!isAdded()) return;
                ketThucTai();
                if (!tuDau && trangHienTai > 1) trangHienTai--;
                Toast.makeText(getContext(), "Không tải được lịch sử. Kéo xuống để thử lại.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ketThucTai() {
        dangTai = false;
        progress_ho_so.setVisibility(View.GONE);
        swipe_ho_so.setRefreshing(false);
    }

    /* ══════════════════════════ Vòng đời ════════════════════════════════ */

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Chỉ bộ lọc. Bản cũ lưu cả danh sách dưới dạng JSON — Bundle đi qua
        // Binder với trần khoảng 1 MB, đủ dài là sập ứng dụng khi xoay màn hình.
        outState.putInt("vi_tri_loc", viTriLoc);
    }

    @Override
    public void onResume() {
        super.onResume();
        taiLaiTatCa();
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
        if (!hidden) taiLaiTatCa();
    }

}
