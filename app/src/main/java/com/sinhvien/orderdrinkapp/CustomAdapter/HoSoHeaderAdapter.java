package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sinhvien.orderdrinkapp.R;

import java.text.DecimalFormat;

/**
 * HoSoHeaderAdapter — phần đầu của màn hình "Lịch sử & chi tiêu".
 *
 * Chỉ có ĐÚNG MỘT mục: thẻ hồ sơ, ba ô thống kê, tiêu đề danh sách và dãy
 * chip lọc.
 *
 *
 * VÌ SAO LÀ MỘT ADAPTER RIÊNG CHỨ KHÔNG PHẢI VIEW CỐ ĐỊNH
 * ========================================================
 * Ghép với `BookingHistoryAdapter` bằng `ConcatAdapter`, nên phần đầu là
 * một mục của cùng một RecyclerView. Ba cái lợi:
 *
 *   1. Nó CUỘN theo danh sách. Thẻ hồ sơ cao gần 300dp; để cố định thì
 *      trên màn hình 6 inch chỉ còn chỗ cho 2 thẻ lịch hẹn.
 *   2. Vẫn giữ nguyên khả năng tái sử dụng view của RecyclerView. Cách cũ
 *      là bọc tất cả trong NestedScrollView, và chính điều đó đã vô hiệu
 *      hóa cơ chế tái sử dụng (xem QĐ-056).
 *   3. Không cần adapter đa kiểu-view. `BookingHistoryAdapter` được dùng
 *      chung với màn hình lịch hẹn; nhét thêm một kiểu view "header" vào
 *      đó sẽ làm phức tạp cả hai màn hình vì lợi ích của một.
 *
 * Dữ liệu vào qua `capNhat()`; adapter tự vẽ lại đúng một mục.
 */
public class HoSoHeaderAdapter extends RecyclerView.Adapter<HoSoHeaderAdapter.ViewHolder> {

    /** Ngưỡng chi tiêu để lên hạng, đơn vị đồng. */
    private static final long NGUONG_BAC       = 500_000L;
    private static final long NGUONG_VANG      = 1_000_000L;
    private static final long NGUONG_KIMCUONG  = 2_500_000L;

    /** Fragment nhận lại LinearLayout chứa chip để tự dựng dãy lọc và sự kiện sửa hồ sơ. */
    public interface OnHeaderSan {
        void dungChipLoc(LinearLayout khungChip);
        void onChinhSuaHoSo();
    }

    private final Context context;
    private final OnHeaderSan boLangNghe;
    private final DecimalFormat dinhDangTien = com.sinhvien.orderdrinkapp.Utils.TienTe.dinhDang();

    private String hoTen = "", sdt = "", email = "", hinhAnh = "";
    private long chiTieu = 0;
    private int soLanDat = 0, soHoanThanh = 0, soDaHuy = 0, tongSoPhieu = 0;

    public HoSoHeaderAdapter(Context context, OnHeaderSan boLangNghe) {
        this.context = context;
        this.boLangNghe = boLangNghe;
    }

    /** Đổ dữ liệu hồ sơ vào phần đầu (kèm ảnh đại diện). */
    public void capNhat(String hoTen, String sdt, String email, String hinhAnh, long chiTieu,
                        int soLanDat, int soHoanThanh, int soDaHuy) {
        this.hoTen       = hoTen != null ? hoTen : "";
        this.sdt         = sdt != null ? sdt : "";
        this.email       = email != null ? email : "";
        this.hinhAnh     = hinhAnh != null ? hinhAnh : "";
        this.chiTieu     = chiTieu;
        this.soLanDat    = soLanDat;
        this.soHoanThanh = soHoanThanh;
        this.soDaHuy     = soDaHuy;
        notifyItemChanged(0);
    }

    /** Quá tải tương thích ngược không có ảnh đại diện. */
    public void capNhat(String hoTen, String sdt, String email, long chiTieu,
                        int soLanDat, int soHoanThanh, int soDaHuy) {
        capNhat(hoTen, sdt, email, this.hinhAnh, chiTieu, soLanDat, soHoanThanh, soDaHuy);
    }

    /** Cập nhật riêng số phiếu khớp bộ lọc (đổi mỗi lần bấm chip). */
    public void capNhatTongSoPhieu(int tong) {
        this.tongSoPhieu = tong;
        notifyItemChanged(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_ho_so_header, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        String tenHien = hoTen.isEmpty() ? context.getString(R.string.customer_default_name) : hoTen;
        h.txt_profile_name.setText(tenHien);
        // Tên chủ thẻ in hoa như trên thẻ thật.
        h.txt_ten_chu_the.setText(tenHien.toUpperCase(new java.util.Locale("vi", "VN")));

        // Hiển thị ảnh Avatar nếu có; nếu không fallback về chữ cái đầu tiên của tên
        if (h.img_profile_avatar != null) {
            if (hinhAnh != null && !hinhAnh.trim().isEmpty()) {
                h.img_profile_avatar.setVisibility(View.VISIBLE);
                h.txt_chu_cai_dau.setVisibility(View.GONE);
                Glide.with(context)
                        .load(hinhAnh)
                        .circleCrop()
                        .placeholder(R.drawable.bg_tron_sang)
                        .error(R.drawable.bg_tron_sang)
                        .into(h.img_profile_avatar);
            } else {
                h.img_profile_avatar.setVisibility(View.GONE);
                h.txt_chu_cai_dau.setVisibility(View.VISIBLE);
                h.txt_chu_cai_dau.setText(chuCaiDaiDien(hoTen));
            }
        } else {
            h.txt_chu_cai_dau.setText(chuCaiDaiDien(hoTen));
        }

        // Bấm vào khung Avatar để mở dialog chỉnh sửa hồ sơ & ảnh
        if (h.layout_avatar_container != null) {
            h.layout_avatar_container.setOnClickListener(v -> {
                if (boLangNghe != null) {
                    boLangNghe.onChinhSuaHoSo();
                }
            });
        }

        h.txt_profile_phone.setText("SĐT: " + (sdt.isEmpty() ? "-" : sdt));

        if (email.isEmpty()) {
            h.txt_profile_email.setVisibility(View.GONE);
        } else {
            h.txt_profile_email.setText(email);
            h.txt_profile_email.setVisibility(View.VISIBLE);
        }

        h.txt_profile_spending.setText(dinhDangTien.format(chiTieu) + " đ");

        h.txt_so_lan_dat.setText(String.valueOf(soLanDat));
        h.txt_so_hoan_thanh.setText(String.valueOf(soHoanThanh));
        h.txt_so_da_huy.setText(String.valueOf(soDaHuy));

        h.txt_tong_so_phieu.setText(tongSoPhieu > 0
                ? context.getString(R.string.customer_booking_count, tongSoPhieu) : "");

        veHangThanhVien(h);

        // Fragment tự dựng chip vào khung này. Gọi ở mỗi lần bind vì
        // RecyclerView có thể tạo lại view của mục header.
        if (boLangNghe != null) boLangNghe.dungChipLoc(h.layout_chip_loc);
    }

    /**
     * Chữ cái đại diện thay cho ảnh đại diện — không cần tải ảnh, không cần
     * chỗ lưu, và luôn có gì đó để hiển thị.
     *
     * Lấy chữ đầu của TỪ CUỐI CÙNG, không phải từ đầu tiên: tên người Việt
     * xếp họ trước tên, và người ta tự xưng bằng TÊN. "Ngô Thị Phượng" phải
     * ra "P" chứ không phải "N".
     */
    private String chuCaiDaiDien(String hoTen) {
        if (hoTen == null) return "?";
        String sach = hoTen.trim();
        if (sach.isEmpty()) return "?";

        String[] tu = sach.split("\\s+");
        String cuoi = tu[tu.length - 1];
        return cuoi.isEmpty() ? "?" : cuoi.substring(0, 1).toUpperCase();
    }

    /** Huy hiệu hạng kim loại 3D + Thẻ VIP CR80 + thanh tiến độ tới hạng kế tiếp. */
    private void veHangThanhVien(ViewHolder h) {
        String tenHang;
        String mauHang;
        long nguongKeTiep;
        String tenHangKeTiep;
        int cardRes;
        int badgeRes;

        if (chiTieu >= NGUONG_KIMCUONG) {
            tenHang = context.getString(R.string.customer_rank_diamond);
            mauHang = "#00BCD4";
            cardRes = R.drawable.the_kimcuong;
            badgeRes = R.drawable.rank_kimcuong;
            nguongKeTiep = 0;
            tenHangKeTiep = null;
        } else if (chiTieu >= NGUONG_VANG) {
            tenHang = context.getString(R.string.customer_rank_gold);
            mauHang = "#FFD700";
            cardRes = R.drawable.the_vang;
            badgeRes = R.drawable.rank_vang;
            nguongKeTiep = NGUONG_KIMCUONG;
            tenHangKeTiep = context.getString(R.string.customer_rank_diamond);
        } else if (chiTieu >= NGUONG_BAC) {
            tenHang = context.getString(R.string.customer_rank_silver);
            mauHang = "#C0C0C0";
            cardRes = R.drawable.the_bac;
            badgeRes = R.drawable.rank_bac;
            nguongKeTiep = NGUONG_VANG;
            tenHangKeTiep = context.getString(R.string.customer_rank_gold);
        } else {
            tenHang = context.getString(R.string.customer_rank_bronze);
            mauHang = "#CD7F32";
            cardRes = R.drawable.the_dong;
            badgeRes = R.drawable.rank_dong;
            nguongKeTiep = NGUONG_BAC;
            tenHangKeTiep = context.getString(R.string.customer_rank_silver);
        }

        h.txt_profile_badge.setText(tenHang);

        if (h.img_card_background != null) {
            h.img_card_background.setImageResource(cardRes);
        }
        if (h.img_rank_badge != null) {
            h.img_rank_badge.setImageResource(badgeRes);
        }

        // mutate() bắt buộc: không có nó thì Drawable được chia sẻ giữa mọi
        // view dùng cùng tệp XML, và đổi màu ở đây sẽ đổi luôn chỗ khác.
        GradientDrawable nen = (GradientDrawable) h.txt_profile_badge.getBackground();
        if (nen != null) {
            nen = (GradientDrawable) nen.mutate();
            nen.setColor(Color.parseColor(mauHang));
            h.txt_profile_badge.setBackground(nen);
        }

        if (nguongKeTiep == 0) {
            h.progress_hang.setProgress(100);
            h.txt_tien_do_hang.setText(R.string.customer_rank_top);
        } else {
            long moc = (nguongKeTiep == NGUONG_KIMCUONG) ? NGUONG_VANG : ((nguongKeTiep == NGUONG_VANG) ? NGUONG_BAC : 0);
            long da   = chiTieu - moc;
            long can  = nguongKeTiep - moc;
            int  phan = (int) Math.max(0, Math.min(100, can > 0 ? (da * 100 / can) : 0));

            h.progress_hang.setProgress(phan);
            h.txt_tien_do_hang.setText(context.getString(R.string.customer_rank_progress,
                    dinhDangTien.format(nguongKeTiep - chiTieu), tenHangKeTiep));
        }
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img_card_background, img_rank_badge, img_profile_avatar, img_edit_badge;
        FrameLayout layout_avatar_container;
        TextView txt_chu_cai_dau, txt_profile_name, txt_profile_phone, txt_profile_email,
                 txt_profile_spending, txt_profile_badge, txt_tien_do_hang, txt_ten_chu_the,
                 txt_so_lan_dat, txt_so_hoan_thanh, txt_so_da_huy, txt_tong_so_phieu;
        ProgressBar progress_hang;
        LinearLayout layout_chip_loc;

        public ViewHolder(@NonNull View v) {
            super(v);
            img_card_background     = v.findViewById(R.id.img_card_background);
            img_rank_badge          = v.findViewById(R.id.img_rank_badge);
            layout_avatar_container = v.findViewById(R.id.layout_avatar_container);
            img_profile_avatar      = v.findViewById(R.id.img_profile_avatar);
            img_edit_badge          = v.findViewById(R.id.img_edit_badge);
            txt_chu_cai_dau         = v.findViewById(R.id.txt_chu_cai_dau);
            txt_profile_name        = v.findViewById(R.id.txt_profile_name);
            txt_profile_phone       = v.findViewById(R.id.txt_profile_phone);
            txt_profile_email       = v.findViewById(R.id.txt_profile_email);
            txt_profile_spending    = v.findViewById(R.id.txt_profile_spending);
            txt_profile_badge       = v.findViewById(R.id.txt_profile_badge);
            txt_ten_chu_the         = v.findViewById(R.id.txt_ten_chu_the);
            txt_tien_do_hang        = v.findViewById(R.id.txt_tien_do_hang);
            txt_so_lan_dat          = v.findViewById(R.id.txt_so_lan_dat);
            txt_so_hoan_thanh       = v.findViewById(R.id.txt_so_hoan_thanh);
            txt_so_da_huy           = v.findViewById(R.id.txt_so_da_huy);
            txt_tong_so_phieu       = v.findViewById(R.id.txt_tong_so_phieu);
            progress_hang           = v.findViewById(R.id.progress_hang);
            layout_chip_loc         = v.findViewById(R.id.layout_chip_loc);
        }
    }
}
