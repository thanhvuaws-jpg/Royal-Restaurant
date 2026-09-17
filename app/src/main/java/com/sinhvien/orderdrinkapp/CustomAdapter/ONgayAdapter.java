package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.Api.LoyaltyResponse;
import com.sinhvien.orderdrinkapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * ONgayAdapter — bảng 7 ô điểm danh.
 *
 * Bốn trạng thái ô, đúng theo bản thiết kế người dùng gửi:
 *
 *   chua   xám nhạt, xu mờ, chữ xám       — ngày chưa tới
 *   xong   nâu đậm, dấu tích trắng        — đã điểm danh
 *   homnay kem, viền vàng dày, xu vàng    — ô đang chờ bấm
 *   moc    kem, viền vàng, hộp quà        — ngày 7
 *
 * Ô mốc (ngày 7) chiếm trọn một hàng — dùng `GridLayoutManager` với
 * `SpanSizeLookup` trả về 3 cho vị trí cuối. Cách này giữ đúng bố cục
 * "6 ô ba cột + 1 ô rộng" của bản thiết kế mà không phải ghép hai
 * RecyclerView.
 */
public class ONgayAdapter extends RecyclerView.Adapter<ONgayAdapter.ViewHolder> {

    private final Context context;
    private final List<LoyaltyResponse.ONgay> danhSach = new ArrayList<>();

    public ONgayAdapter(Context context) {
        this.context = context;
    }

    public void capNhat(List<LoyaltyResponse.ONgay> ds) {
        danhSach.clear();
        if (ds != null) danhSach.addAll(ds);
        notifyDataSetChanged();
    }

    /** Ô cuối (mốc quà) chiếm cả 3 cột. */
    public int soCot(int viTri, int tongCot) {
        return (viTri == danhSach.size() - 1) ? tongCot : 1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_o_diem_danh, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        LoyaltyResponse.ONgay o = danhSach.get(position);

        h.txt_ten_ngay.setText(context.getString(R.string.dd_ngay, o.getNgay()));
        h.txt_diem_o.setText(context.getString(R.string.dd_cong_diem, o.getDiem()));

        boolean laMoc  = o.isLaMoc();
        String trangThai = o.getTrangThai();

        // Mặc định: ẩn hộp quà, hiện đồng xu.
        h.img_qua.setVisibility(View.GONE);
        h.txt_bieu_tuong.setVisibility(View.VISIBLE);

        if ("xong".equals(trangThai)) {
            h.khung_o_ngay.setBackgroundResource(R.drawable.bg_o_xong);
            h.txt_ten_ngay.setTextColor(Color.parseColor("#E8DAD3"));
            h.txt_diem_o.setTextColor(Color.parseColor("#C7B4AB"));
            h.txt_bieu_tuong.setText("✓");
            h.txt_bieu_tuong.setTextColor(Color.parseColor("#3E2B23"));
            doiMauXu(h, "#FFFFFF");

        } else if ("homnay".equals(trangThai)) {
            h.khung_o_ngay.setBackgroundResource(laMoc ? R.drawable.bg_o_moc : R.drawable.bg_o_homnay);
            h.txt_ten_ngay.setTextColor(Color.parseColor("#5D4037"));
            h.txt_diem_o.setTextColor(Color.parseColor("#8A6510"));
            if (laMoc) {
                h.txt_bieu_tuong.setVisibility(View.GONE);
                h.img_qua.setVisibility(View.VISIBLE);
            } else {
                h.txt_bieu_tuong.setText("Đ");
                h.txt_bieu_tuong.setTextColor(Color.parseColor("#8A6510"));
                doiMauXu(h, "#F5CE55");
            }

        } else {  // 'chua'
            h.khung_o_ngay.setBackgroundResource(laMoc ? R.drawable.bg_o_moc : R.drawable.bg_o_chua);
            h.txt_ten_ngay.setTextColor(Color.parseColor("#8D8074"));
            h.txt_diem_o.setTextColor(Color.parseColor("#A99C8F"));
            if (laMoc) {
                h.txt_bieu_tuong.setVisibility(View.GONE);
                h.img_qua.setVisibility(View.VISIBLE);
            } else {
                h.txt_bieu_tuong.setText("Đ");
                h.txt_bieu_tuong.setTextColor(Color.parseColor("#B5A99D"));
                doiMauXu(h, "#DCD5CF");
            }
        }
    }

    /**
     * Đổi màu nền tròn của đồng xu.
     *
     * mutate() bắt buộc: mọi ô dùng chung tệp `bg_xu_tron.xml`, không tách
     * bản sao thì đổi màu một ô sẽ đổi màu tất cả — và vì RecyclerView tái
     * dùng view, kết quả là cả bảng nhuộm theo ô được vẽ sau cùng.
     */
    private void doiMauXu(ViewHolder h, String mau) {
        android.graphics.drawable.Drawable nen = h.txt_bieu_tuong.getBackground();
        if (nen != null) {
            nen = nen.mutate();
            nen.setColorFilter(new android.graphics.PorterDuffColorFilter(
                    Color.parseColor(mau), android.graphics.PorterDuff.Mode.SRC_IN));
            h.txt_bieu_tuong.setBackground(nen);
        }
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout khung_o_ngay;
        TextView txt_ten_ngay, txt_bieu_tuong, txt_diem_o;
        ImageView img_qua;

        public ViewHolder(@NonNull View v) {
            super(v);
            khung_o_ngay   = v.findViewById(R.id.khung_o_ngay);
            txt_ten_ngay   = v.findViewById(R.id.txt_ten_ngay);
            txt_bieu_tuong = v.findViewById(R.id.txt_bieu_tuong);
            txt_diem_o     = v.findViewById(R.id.txt_diem_o);
            img_qua        = v.findViewById(R.id.img_qua);
        }
    }
}
