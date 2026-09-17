package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.Api.TinNhan;
import com.sinhvien.orderdrinkapp.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * TinNhanAdapter — vẽ danh sách tin nhắn của phòng chat chăm sóc khách hàng.
 *
 *
 * BA KIỂU DÒNG
 * ------------
 *   KHACH   : bong bóng dạt phải, màu chủ đạo
 *   HO_TRO  : bong bóng dạt trái — dùng cho CẢ nhân viên lẫn trợ lý tự động,
 *             chỉ khác nền, biểu tượng và tên
 *   NGAY    : nhãn ngày chen giữa
 *
 *
 * VÌ SAO NHÃN NGÀY LÀ MỘT DÒNG THẬT, KHÔNG PHẢI PHẦN ĐẦU CỦA BONG BÓNG
 * ====================================================================
 * Cách rẻ hơn là nhét nhãn vào chính layout tin rồi ẩn/hiện. Nhưng
 * RecyclerView TÁI SỬ DỤNG view: một dòng từng hiện nhãn sẽ được dùng lại
 * cho một dòng khác, và nếu quên đặt lại visibility thì nhãn ngày xuất hiện
 * ở giữa cuộc trò chuyện. Lỗi đó chỉ lộ ra khi cuộn — tức là gần như không
 * bao giờ thấy lúc phát triển.
 *
 * Tách thành một kiểu dòng riêng thì không có gì để quên.
 *
 *
 * DANH SÁCH HIỂN THỊ TÁCH KHỎI DANH SÁCH DỮ LIỆU
 * ----------------------------------------------
 * dsTin giữ tin thật; dsHienThi là dsTin đã chèn nhãn ngày. Trộn nhãn thẳng
 * vào dữ liệu sẽ khiến mọi phép "tin cuối là tin nào" phải nhớ bỏ qua nhãn —
 * và sẽ có chỗ quên.
 */
public class TinNhanAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int KIEU_KHACH  = 1;
    private static final int KIEU_HO_TRO = 2;
    private static final int KIEU_NGAY   = 3;

    /** Một dòng trên màn hình: hoặc là tin, hoặc là nhãn ngày. */
    private static class Dong {
        final int     kieu;
        final TinNhan tin;
        final String  nhan;

        Dong(int kieu, TinNhan tin, String nhan) {
            this.kieu = kieu; this.tin = tin; this.nhan = nhan;
        }
    }

    private final List<TinNhan> dsTin     = new ArrayList<>();
    private final List<Dong>    dsHienThi = new ArrayList<>();

    /* ═══════════════════════ Dữ liệu ═══════════════════════ */

    /** Thay toàn bộ danh sách. Dùng khi nạp lần đầu hoặc khi tải lại. */
    public void datLai(List<TinNhan> tin) {
        dsTin.clear();
        if (tin != null) dsTin.addAll(tin);
        dungLaiDanhSach();
        notifyDataSetChanged();
    }

    /**
     * Thêm một tin vào cuối, BỎ QUA nếu đã có.
     *
     * Chống trùng bằng mã tin là bắt buộc chứ không phải phòng xa: cùng một
     * tin có thể tới hai đường — qua phản hồi của lời gọi gửi, và qua sự kiện
     * Socket.IO. Không lọc thì mỗi câu hiện hai lần.
     */
    public void them(TinNhan t) {
        if (t == null) return;
        if (t.getMaTinNhan() > 0) {
            for (TinNhan cu : dsTin) {
                if (cu.getMaTinNhan() == t.getMaTinNhan()) return;
            }
        }
        dsTin.add(t);
        dungLaiDanhSach();
        notifyDataSetChanged();
    }

    /** Gỡ một tin tạm (mã âm) khi máy chủ đã xác nhận hoặc khi gửi hụt. */
    public void goTinTam(long maTam) {
        for (int i = 0; i < dsTin.size(); i++) {
            if (dsTin.get(i).getMaTinNhan() == maTam) {
                dsTin.remove(i);
                dungLaiDanhSach();
                notifyDataSetChanged();
                return;
            }
        }
    }

    /** Mã tin thật lớn nhất đang có — mốc cho lần lấy tăng dần kế tiếp. */
    public long maCuoi() {
        long lon = 0;
        for (TinNhan t : dsTin) {
            if (t.getMaTinNhan() > lon) lon = t.getMaTinNhan();
        }
        return lon;
    }

    public boolean rong() { return dsTin.isEmpty(); }

    /** Dựng lại danh sách hiển thị: chèn nhãn ngày mỗi khi sang ngày mới. */
    private void dungLaiDanhSach() {
        dsHienThi.clear();
        String ngayTruoc = null;
        for (TinNhan t : dsTin) {
            String ngay = phanNgay(t.getNgayTao());
            if (ngay != null && !ngay.equals(ngayTruoc)) {
                dsHienThi.add(new Dong(KIEU_NGAY, null, nhanNgay(ngay)));
                ngayTruoc = ngay;
            }
            dsHienThi.add(new Dong(t.laCuaKhach() ? KIEU_KHACH : KIEU_HO_TRO, t, null));
        }
    }

    /* ═══════════════════════ RecyclerView ═══════════════════════ */

    @Override
    public int getItemViewType(int vt) { return dsHienThi.get(vt).kieu; }

    @Override
    public int getItemCount() { return dsHienThi.size(); }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup cha, int kieu) {
        LayoutInflater bom = LayoutInflater.from(cha.getContext());
        if (kieu == KIEU_NGAY) {
            return new OngNgay(bom.inflate(R.layout.item_nhan_ngay, cha, false));
        }
        if (kieu == KIEU_KHACH) {
            return new OngKhach(bom.inflate(R.layout.item_tin_khach, cha, false));
        }
        return new OngHoTro(bom.inflate(R.layout.item_tin_ho_tro, cha, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder oh, int vt) {
        Dong d = dsHienThi.get(vt);

        if (oh instanceof OngNgay) {
            ((OngNgay) oh).txtNhan.setText(d.nhan);
            return;
        }

        if (oh instanceof OngKhach) {
            OngKhach o = (OngKhach) oh;
            o.txtNoiDung.setText(d.tin.getNoiDung());
            // Tin chưa được máy chủ xác nhận thì làm mờ và ghi "Đang gửi…".
            // Khách phải thấy được sự khác nhau giữa "đã tới nơi" và "đang
            // trên đường", nhất là khi mạng chập chờn.
            if (d.tin.dangGui()) {
                o.txtNoiDung.setAlpha(0.55f);
                o.txtGio.setText("Đang gửi…");
            } else {
                o.txtNoiDung.setAlpha(1f);
                o.txtGio.setText(gio(d.tin.getNgayTao()));
            }
            return;
        }

        OngHoTro o = (OngHoTro) oh;
        o.txtNoiDung.setText(d.tin.getNoiDung());
        o.txtGio.setText(gio(d.tin.getNgayTao()));

        if (d.tin.laCuaBot()) {
            o.txtNoiDung.setBackgroundResource(R.drawable.bong_chat_bot);
            o.anh.setImageResource(R.drawable.ic_tro_ly);
            o.txtTen.setText("Trợ lý tự động");
            o.txtTen.setTextColor(Color.parseColor("#AA8811"));
        } else {
            o.txtNoiDung.setBackgroundResource(R.drawable.bong_chat_nhanvien);
            o.anh.setImageResource(R.drawable.ic_chat_ho_tro);
            String ten = d.tin.getTenHienThi();
            o.txtTen.setText(ten.isEmpty() ? "Nhân viên" : ten);
            o.txtTen.setTextColor(Color.parseColor("#5D4037"));
        }
    }

    static class OngKhach extends RecyclerView.ViewHolder {
        final TextView txtNoiDung, txtGio;
        OngKhach(@NonNull View v) {
            super(v);
            txtNoiDung = v.findViewById(R.id.txt_noi_dung);
            txtGio     = v.findViewById(R.id.txt_gio);
        }
    }

    static class OngHoTro extends RecyclerView.ViewHolder {
        final TextView  txtNoiDung, txtGio, txtTen;
        final ImageView anh;
        OngHoTro(@NonNull View v) {
            super(v);
            txtNoiDung = v.findViewById(R.id.txt_noi_dung);
            txtGio     = v.findViewById(R.id.txt_gio);
            txtTen     = v.findViewById(R.id.txt_ten_nguoi_gui);
            anh        = v.findViewById(R.id.anh_nguoi_gui);
        }
    }

    static class OngNgay extends RecyclerView.ViewHolder {
        final TextView txtNhan;
        OngNgay(@NonNull View v) {
            super(v);
            txtNhan = v.findViewById(R.id.txt_nhan_ngay);
        }
    }

    /* ═══════════════════════ Định dạng giờ ═══════════════════════ */
    //
    // Máy chủ trả chuỗi dạng YYYY-MM-DD HH:MM:SS — định dạng của PDO, và
    // telegram.js cũng cố ý phát ra đúng dạng đó. Nên cắt chuỗi là đủ, không
    // cần SimpleDateFormat.
    //
    // Cắt chuỗi còn tránh được một cái bẫy: SimpleDateFormat mặc định diễn
    // giải theo múi giờ máy, mà giờ trả về đã là giờ địa phương của quán —
    // phân tích rồi định dạng lại sẽ cộng thêm lệch múi giờ.

    /** 2026-09-12 14:05:37 -> 14:05. Trả rỗng nếu chuỗi không đúng dạng. */
    private static String gio(String s) {
        if (s == null || s.length() < 16) return "";
        return s.substring(11, 16);
    }

    /** 2026-09-12 14:05:37 -> 2026-09-12. */
    private static String phanNgay(String s) {
        if (s == null || s.length() < 10) return null;
        return s.substring(0, 10);
    }

    /** 2026-09-12 -> Hôm nay | Hôm qua | 12/09/2026. */
    private static String nhanNgay(String ngay) {
        Calendar c = Calendar.getInstance();
        String homNay = String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        if (ngay.equals(homNay)) return "Hôm nay";

        c.add(Calendar.DAY_OF_MONTH, -1);
        String homQua = String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        if (ngay.equals(homQua)) return "Hôm qua";

        return ngay.substring(8, 10) + "/" + ngay.substring(5, 7) + "/" + ngay.substring(0, 4);
    }
}
