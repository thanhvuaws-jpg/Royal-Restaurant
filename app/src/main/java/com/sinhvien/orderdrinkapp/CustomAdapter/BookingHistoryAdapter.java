package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.R;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BookingHistoryAdapter — danh sách lịch hẹn của khách hàng.
 *
 * BẢN CŨ chỉ hiển thị: tên bàn, chuỗi trạng thái, `"Giờ hẹn: " + THOIGIANHEN`
 * in nguyên định dạng CSDL (`2026-06-07 04:45:00`), và tổng tiền món đặt
 * trước dạng số thô (`30000 đ`). Không có nút nào, không thao tác được gì.
 *
 * BẢN NÀY thêm:
 *   - Giờ hẹn định dạng tiếng Việt kèm thứ trong tuần
 *   - Đếm ngược tới giờ hẹn cho phiếu sắp tới
 *   - Số khách, số món, tiền có dấu phân cách nghìn
 *   - Ghi chú của khách
 *   - Dải màu trạng thái dọc bên trái + huy hiệu trạng thái
 *   - Nút hủy lịch hẹn (chỉ hiện khi phiếu còn hủy được)
 */
public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.ViewHolder> {

    /**
     * Số phút TỐI THIỂU trước giờ hẹn mà khách còn tự hủy được.
     *
     * Chọn 60 để trùng với khung giữ bàn ở `get_table_booking_status.php`
     * (QĐ-047): từ mốc 60 phút trước giờ hẹn, nhà hàng đã bắt đầu giữ bàn
     * và không xếp khách vãng lai vào nữa. Hủy sau mốc đó thì bàn đã bị
     * treo mất rồi. Hai con số phải bằng nhau, nếu lệch sẽ có quãng thời
     * gian mà khách hủy được trong khi bàn đang bị giữ.
     */
    public static final int PHUT_TOI_THIEU_DE_HUY = 60;

    /** Nhà hàng gọi lại cho khách khi họ bấm hủy — Fragment lo phần gọi API. */
    public interface OnHuyPhieu {
        void huy(BookingResponse phieu);
    }

    private final Context context;
    private final List<BookingResponse> bookingList;
    private final OnHuyPhieu boLangNgheHuy;

    private final SimpleDateFormat dinhDangCsdl =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final DecimalFormat dinhDangTien = com.sinhvien.orderdrinkapp.Utils.TienTe.dinhDang();

    public BookingHistoryAdapter(Context context, List<BookingResponse> bookingList) {
        this(context, bookingList, null);
    }

    public BookingHistoryAdapter(Context context, List<BookingResponse> bookingList,
                                 OnHuyPhieu boLangNgheHuy) {
        this.context = context;
        this.bookingList = bookingList;
        this.boLangNgheHuy = boLangNgheHuy;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_active_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingResponse booking = bookingList.get(position);
        String status = booking.getTinhtrang();

        holder.txt_booking_table.setText(
                booking.getTenBan() != null ? booking.getTenBan() : "Bàn #" + booking.getMaBan());

        /* ───────────────────── Trạng thái: nhãn + màu ───────────────────── */

        String nhan;
        int mau;
        if ("pending".equalsIgnoreCase(status)) {
            nhan = "Chờ xác nhận";  mau = Color.parseColor("#FFAB40");
        } else if ("confirmed".equalsIgnoreCase(status)) {
            nhan = "Đã xác nhận";   mau = Color.parseColor("#1E88E5");
        } else if ("checked_in".equalsIgnoreCase(status)) {
            nhan = "Đã nhận bàn";   mau = Color.parseColor("#43A047");
        } else if ("completed".equalsIgnoreCase(status)) {
            nhan = "Hoàn thành";    mau = Color.parseColor("#7E57C2");
        } else if ("overdue".equalsIgnoreCase(status)) {
            nhan = "Quá giờ hẹn";   mau = Color.parseColor("#E53935");
        } else if ("cancelled".equalsIgnoreCase(status)) {
            nhan = "Đã hủy";        mau = Color.parseColor("#9E9E9E");
        } else {
            nhan = status != null ? status : "—";
            mau  = Color.parseColor("#9E9E9E");
        }

        holder.txt_booking_status.setText(nhan);
        holder.dai_mau_trang_thai.setBackgroundColor(mau);

        // Huy hiệu bo tròn: tạo mới rồi mutate() mỗi lần bind. Nếu dùng lại
        // một Drawable dùng chung thì mọi thẻ sẽ đổi theo màu của thẻ cuối
        // — RecyclerView tái dùng view nên lỗi này rất dễ mắc.
        GradientDrawable vien = new GradientDrawable();
        vien.setShape(GradientDrawable.RECTANGLE);
        vien.setCornerRadius(context.getResources().getDisplayMetrics().density * 20);
        vien.setColor(mau);
        holder.txt_booking_status.setBackground(vien);

        /* ─────────────────────── Giờ hẹn + đếm ngược ────────────────────── */

        Date gioHen = doiGioHen(booking.getThoigianhen());

        if (gioHen != null) {
            holder.txt_booking_time.setText(dinhDangGioHen(gioHen));

            String demNguoc = demNguoc(gioHen, status);
            holder.txt_booking_countdown.setText(demNguoc);
            holder.txt_booking_countdown.setVisibility(demNguoc != null ? View.VISIBLE : View.GONE);
        } else {
            // Không phân giải được thì hiện nguyên chuỗi, còn hơn để trống.
            holder.txt_booking_time.setText(booking.getThoigianhen());
            holder.txt_booking_countdown.setVisibility(View.GONE);
        }

        /* ────────────────────── Số khách · món đặt trước ────────────────── */

        String phanKhach = booking.getSoKhach() > 0
                ? context.getString(R.string.customer_guests, booking.getSoKhach())
                : null;

        String tien = booking.getTongTien();
        boolean coMon = tien != null && !tien.isEmpty() && !"0".equals(tien);
        String phanMon;
        if (coMon) {
            long soTien;
            try { soTien = Long.parseLong(tien); } catch (NumberFormatException e) { soTien = 0; }
            phanMon = context.getString(R.string.customer_preorder,
                    Math.max(booking.getSoMon(), 1), dinhDangTien.format(soTien));
        } else {
            phanMon = context.getString(R.string.customer_no_preorder);
        }

        holder.txt_booking_dishes.setText(phanKhach != null
                ? context.getString(R.string.customer_guests_dishes, phanKhach, phanMon)
                : phanMon);

        /* ──────────────────────────── Ghi chú ───────────────────────────── */

        String ghiChu = booking.getGhiChu();
        if (ghiChu != null && !ghiChu.trim().isEmpty()) {
            holder.txt_booking_note.setText("“" + ghiChu.trim() + "”");
            holder.txt_booking_note.setVisibility(View.VISIBLE);
        } else {
            holder.txt_booking_note.setVisibility(View.GONE);
        }

        /* ───────────────────────── Nút hủy lịch hẹn ─────────────────────── */

        boolean choPhepHuy = boLangNgheHuy != null && coTheHuy(status, gioHen);
        holder.layout_nut_phieu.setVisibility(choPhepHuy ? View.VISIBLE : View.GONE);

        if (choPhepHuy) {
            holder.btn_huy_phieu.setOnClickListener(v -> boLangNgheHuy.huy(booking));
        } else {
            // Gỡ listener cũ: view được tái dùng cho phiếu khác, để nguyên
            // thì bấm nhầm sẽ hủy đúng phiếu của lần bind trước.
            holder.btn_huy_phieu.setOnClickListener(null);
        }
    }

    /**
     * Phiếu có còn hủy được không.
     *
     * Hai điều kiện, và cả hai đều được máy chủ kiểm tra lại — đây chỉ là
     * lớp che nút để người dùng không bấm rồi nhận lỗi:
     *   1. Trạng thái phải là `pending` hoặc `confirmed`. Đã nhận bàn thì
     *      không còn là chuyện đặt trước nữa (xem bảng chuyển trạng thái
     *      trong `api/booking_state.php`).
     *   2. Còn ít nhất PHUT_TOI_THIEU_DE_HUY phút trước giờ hẹn.
     */
    public static boolean coTheHuy(String trangThai, Date gioHen) {
        if (!("pending".equalsIgnoreCase(trangThai) || "confirmed".equalsIgnoreCase(trangThai))) {
            return false;
        }
        if (gioHen == null) return false;

        long conLaiPhut = (gioHen.getTime() - System.currentTimeMillis()) / 60000L;
        return conLaiPhut >= PHUT_TOI_THIEU_DE_HUY;
    }

    /** Phân giải chuỗi thời gian của máy chủ. Trả null nếu không đọc được. */
    public Date doiGioHen(String chuoi) {
        if (chuoi == null || chuoi.isEmpty()) return null;
        try {
            return dinhDangCsdl.parse(chuoi);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * "19:30 · Thứ Bảy, 26/05/2026"
     *
     * public static để hộp thoại xác nhận hủy trong CustomerBookingFragment
     * dùng lại — nếu không, hộp thoại sẽ in nguyên chuỗi CSDL
     * `2026-09-15 07:50:24` trong khi thẻ ngay bên dưới hiển thị định dạng
     * đẹp. Hai cách viết cùng một mốc thời gian trên cùng một màn hình làm
     * người dùng tưởng là hai thời điểm khác nhau.
     */
    public static String dinhDangGioHen(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);

        String[] thu = {"Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư",
                        "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};

        return String.format(Locale.getDefault(), "%02d:%02d · %s, %02d/%02d/%d",
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                thu[c.get(Calendar.DAY_OF_WEEK) - 1],
                c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
    }

    /**
     * "Còn 2 ngày nữa" / "Còn 3 giờ nữa" / "Sắp tới giờ hẹn".
     *
     * Chỉ có nghĩa với phiếu CHƯA diễn ra; phiếu đã hoàn thành hay đã hủy
     * thì đếm ngược là thông tin nhiễu, trả null để ẩn dòng đó đi.
     */
    private String demNguoc(Date gioHen, String trangThai) {
        if (!("pending".equalsIgnoreCase(trangThai) || "confirmed".equalsIgnoreCase(trangThai))) {
            return null;
        }

        long conLai = gioHen.getTime() - System.currentTimeMillis();
        if (conLai <= 0) return null;

        long phut = conLai / 60000L;
        if (phut >= 1440) return context.getString(R.string.customer_countdown_days, phut / 1440);
        if (phut >= 60)   return context.getString(R.string.customer_countdown_hours, phut / 60);
        if (phut >= 1)    return context.getString(R.string.customer_countdown_minutes, phut);
        return context.getString(R.string.customer_countdown_now);
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    /** ViewHolder nắm giữ cấu trúc hiển thị 1 lịch hẹn. */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt_booking_table, txt_booking_status, txt_booking_time,
                 txt_booking_countdown, txt_booking_dishes, txt_booking_note;
        View dai_mau_trang_thai;
        LinearLayout layout_nut_phieu;
        MaterialButton btn_huy_phieu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txt_booking_table     = itemView.findViewById(R.id.txt_booking_table);
            txt_booking_status    = itemView.findViewById(R.id.txt_booking_status);
            txt_booking_time      = itemView.findViewById(R.id.txt_booking_time);
            txt_booking_countdown = itemView.findViewById(R.id.txt_booking_countdown);
            txt_booking_dishes    = itemView.findViewById(R.id.txt_booking_dishes);
            txt_booking_note      = itemView.findViewById(R.id.txt_booking_note);
            dai_mau_trang_thai    = itemView.findViewById(R.id.dai_mau_trang_thai);
            layout_nut_phieu      = itemView.findViewById(R.id.layout_nut_phieu);
            btn_huy_phieu         = itemView.findViewById(R.id.btn_huy_phieu);
        }
    }
}
