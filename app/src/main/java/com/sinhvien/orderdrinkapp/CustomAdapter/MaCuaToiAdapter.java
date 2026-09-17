package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.Api.LoyaltyResponse;
import com.sinhvien.orderdrinkapp.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * MaCuaToiAdapter — danh sách mã giảm giá của khách.
 *
 * Nhấn vào mã là sao chép. Khách thường đọc mã cho nhân viên nghe, nhưng
 * cũng có lúc muốn nhắn cho người đi cùng — sao chép rẻ hơn nhiều so với
 * gõ lại sáu ký tự.
 */
public class MaCuaToiAdapter extends RecyclerView.Adapter<MaCuaToiAdapter.ViewHolder> {

    private final Context context;
    private final List<LoyaltyResponse.MaGiamGia> danhSach = new ArrayList<>();
    private final DecimalFormat dinhDangTien = new DecimalFormat("#,###");

    public MaCuaToiAdapter(Context context) {
        this.context = context;
    }

    public void capNhat(List<LoyaltyResponse.MaGiamGia> ds) {
        danhSach.clear();
        if (ds != null) danhSach.addAll(ds);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_ma_cua_toi, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        LoyaltyResponse.MaGiamGia m = danhSach.get(position);

        h.txt_ten_ma.setText(m.getTen());
        h.txt_macode.setText(m.getMaCode());

        String tt = m.getTinhTrang();
        String nhan;
        int mau;
        if ("dadung".equals(tt)) {
            nhan = context.getString(R.string.dd_tt_dadung);
            mau  = Color.parseColor("#9E9E9E");
        } else if ("hethan".equals(tt)) {
            nhan = context.getString(R.string.dd_tt_hethan);
            mau  = Color.parseColor("#E53935");
        } else {
            nhan = context.getString(R.string.dd_tt_chuadung);
            mau  = Color.parseColor("#43A047");
        }

        h.txt_trang_thai_ma.setText(nhan);
        h.dai_mau_ma.setBackgroundColor(mau);

        // Tạo Drawable mới mỗi lần bind: dùng chung một cái thì mọi thẻ sẽ
        // mang màu của thẻ được vẽ sau cùng, vì RecyclerView tái dùng view.
        GradientDrawable vien = new GradientDrawable();
        vien.setShape(GradientDrawable.RECTANGLE);
        vien.setCornerRadius(context.getResources().getDisplayMetrics().density * 20);
        vien.setColor(mau);
        h.txt_trang_thai_ma.setBackground(vien);

        // Mã đã dùng hoặc hết hạn thì làm mờ, để lúc thanh toán khách không
        // đọc nhầm một mã không còn giá trị.
        boolean conDung = "chuadung".equals(tt);
        h.txt_macode.setAlpha(conDung ? 1f : 0.4f);

        h.txt_han_ma.setText(context.getString(R.string.dd_han_dung, rutGonNgay(m.getNgayHetHan())));

        if (m.getDonToiThieu() > 0) {
            h.txt_dk_ma.setText(context.getString(R.string.dd_don_toi_thieu,
                    dinhDangTien.format(m.getDonToiThieu())));
            h.txt_dk_ma.setVisibility(View.VISIBLE);
        } else {
            h.txt_dk_ma.setText(R.string.dd_nhan_de_chep);
            h.txt_dk_ma.setVisibility(conDung ? View.VISIBLE : View.GONE);
        }

        if (conDung) {
            h.txt_macode.setOnClickListener(v -> {
                ClipboardManager cb =
                        (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) {
                    cb.setPrimaryClip(ClipData.newPlainText("macode", m.getMaCode()));
                    Toast.makeText(context,
                            context.getString(R.string.dd_da_sao_chep, m.getMaCode()),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            h.txt_macode.setOnClickListener(null);
        }
    }

    /** '2026-09-19 08:15:00' thành '19/09/2026'. Giờ phút không giúp gì ở đây. */
    private String rutGonNgay(String chuoi) {
        if (chuoi == null || chuoi.length() < 10) return chuoi != null ? chuoi : "";
        return chuoi.substring(8, 10) + "/" + chuoi.substring(5, 7) + "/" + chuoi.substring(0, 4);
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt_ten_ma, txt_trang_thai_ma, txt_macode, txt_han_ma, txt_dk_ma;
        View dai_mau_ma;

        public ViewHolder(@NonNull View v) {
            super(v);
            txt_ten_ma        = v.findViewById(R.id.txt_ten_ma);
            txt_trang_thai_ma = v.findViewById(R.id.txt_trang_thai_ma);
            txt_macode        = v.findViewById(R.id.txt_macode);
            txt_han_ma        = v.findViewById(R.id.txt_han_ma);
            txt_dk_ma         = v.findViewById(R.id.txt_dk_ma);
            dai_mau_ma        = v.findViewById(R.id.dai_mau_ma);
        }
    }
}
