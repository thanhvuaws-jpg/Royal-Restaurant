package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sinhvien.orderdrinkapp.Api.LoyaltyResponse;
import com.sinhvien.orderdrinkapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * VoucherDoiAdapter — kho phiếu có thể đổi bằng điểm.
 *
 * Nút "ĐỔI" bị vô hiệu hóa khi khách chưa đủ điểm hoặc chưa đủ hạng, kèm
 * một dòng đỏ nói rõ thiếu gì.
 *
 * Vì sao không ẩn hẳn phiếu chưa đủ điều kiện: ẩn thì khách không biết có
 * mục tiêu để phấn đấu. Hiện mà không giải thích thì họ bấm rồi nhận lỗi.
 * Hiện kèm lý do là cách duy nhất vừa rõ ràng vừa không gây hụt hẫng.
 */
public class VoucherDoiAdapter extends RecyclerView.Adapter<VoucherDoiAdapter.ViewHolder> {

    public interface OnDoi {
        void doi(LoyaltyResponse.Voucher v);
    }

    private final Context context;
    private final List<LoyaltyResponse.Voucher> danhSach = new ArrayList<>();
    private final OnDoi boLangNghe;
    private int diemHienTai = 0;

    public VoucherDoiAdapter(Context context, OnDoi boLangNghe) {
        this.context = context;
        this.boLangNghe = boLangNghe;
    }

    public void capNhat(List<LoyaltyResponse.Voucher> ds, int diem) {
        danhSach.clear();
        if (ds != null) danhSach.addAll(ds);
        this.diemHienTai = diem;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_voucher_doi, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        LoyaltyResponse.Voucher v = danhSach.get(position);

        h.txt_ten_voucher.setText(v.getTen());
        h.txt_mota_voucher.setText(v.getMoTa() != null ? v.getMoTa() : "");
        h.btn_doi.setText(context.getString(R.string.dd_gia_diem, v.getDiemDoi()));

        boolean duocDoi = v.isDuDiem() && v.isDuHang();
        h.btn_doi.setEnabled(duocDoi);

        if (duocDoi) {
            h.btn_doi.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D4AF37")));
            h.btn_doi.setTextColor(Color.parseColor("#3E2B23"));
            h.txt_dieu_kien.setVisibility(View.GONE);
            h.btn_doi.setOnClickListener(x -> boLangNghe.doi(v));
        } else {
            h.btn_doi.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0DAD5")));
            h.btn_doi.setTextColor(Color.parseColor("#9E9E9E"));

            // Nói thiếu HẠNG trước, vì đó là thứ không khắc phục được bằng
            // cách điểm danh thêm vài ngày.
            String lyDo;
            if (!v.isDuHang()) {
                lyDo = context.getString(R.string.dd_can_hang, tenHang(v.getHangToiThieu()));
            } else {
                lyDo = context.getString(R.string.dd_thieu_diem, v.getDiemDoi() - diemHienTai);
            }
            h.txt_dieu_kien.setText(lyDo);
            h.txt_dieu_kien.setVisibility(View.VISIBLE);

            // Gỡ listener cũ: view được tái dùng, để nguyên thì thẻ này có
            // thể mang hành động của lần bind trước.
            h.btn_doi.setOnClickListener(null);
        }
    }

    private String tenHang(String ma) {
        if ("vang".equals(ma)) return "Thành viên Vàng";
        if ("bac".equals(ma))  return "Thành viên Bạc";
        return "Thành viên Đồng";
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt_ten_voucher, txt_mota_voucher, txt_dieu_kien;
        MaterialButton btn_doi;

        public ViewHolder(@NonNull View v) {
            super(v);
            txt_ten_voucher  = v.findViewById(R.id.txt_ten_voucher);
            txt_mota_voucher = v.findViewById(R.id.txt_mota_voucher);
            txt_dieu_kien    = v.findViewById(R.id.txt_dieu_kien);
            btn_doi          = v.findViewById(R.id.btn_doi);
        }
    }
}
