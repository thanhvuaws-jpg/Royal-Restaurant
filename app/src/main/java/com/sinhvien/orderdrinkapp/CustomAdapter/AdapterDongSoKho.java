package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.Api.SoKhoResponse;
import com.sinhvien.orderdrinkapp.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * AdapterDongSoKho - Hiển thị các dòng biến động trong Sổ kho của nguyên liệu.
 */
public class AdapterDongSoKho extends RecyclerView.Adapter<AdapterDongSoKho.ViewHolder> {

    private final Context context;
    private final List<SoKhoResponse.DongSoKho> list = new ArrayList<>();
    private final String donVi;
    private final DecimalFormat df = new DecimalFormat("#,##0.##");

    public AdapterDongSoKho(Context context, String donVi) {
        this.context = context;
        this.donVi = donVi != null ? donVi : "";
    }

    public void setData(List<SoKhoResponse.DongSoKho> newList) {
        list.clear();
        if (newList != null) {
            list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_dong_so_kho, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SoKhoResponse.DongSoKho item = list.get(position);

        // Loại biến động
        String loaiPhieu = item.getLoaiPhieu();
        String loaiBienDong = item.getLoai();
        if ("nhap".equalsIgnoreCase(loaiPhieu) || "nhap".equalsIgnoreCase(loaiBienDong)) {
            holder.txtLoaiBienDong.setText("Nhập kho");
        } else if ("xuat".equalsIgnoreCase(loaiPhieu) || "xuat".equalsIgnoreCase(loaiBienDong)) {
            holder.txtLoaiBienDong.setText("Xuất kho");
        } else if ("huy".equalsIgnoreCase(loaiPhieu) || "huy".equalsIgnoreCase(loaiBienDong)) {
            holder.txtLoaiBienDong.setText("Hủy hàng");
        } else if ("ban_hang".equalsIgnoreCase(loaiBienDong) || "ban".equalsIgnoreCase(loaiBienDong)) {
            holder.txtLoaiBienDong.setText("Bán hàng");
        } else if ("kiem_ke".equalsIgnoreCase(loaiBienDong)) {
            holder.txtLoaiBienDong.setText("Kiểm kê điều chỉnh");
        } else {
            holder.txtLoaiBienDong.setText(loaiBienDong);
        }

        // Thay đổi
        double thayDoi = item.getThayDoi();
        if (thayDoi > 0) {
            holder.txtThayDoi.setText("+" + df.format(thayDoi) + " " + donVi);
            holder.txtThayDoi.setTextColor(ContextCompat.getColor(context, R.color.kho_du));
        } else {
            holder.txtThayDoi.setText(df.format(thayDoi) + " " + donVi);
            holder.txtThayDoi.setTextColor(ContextCompat.getColor(context, R.color.kho_het));
        }

        // Tồn sau
        holder.txtTonSau.setText("Tồn sau: " + df.format(item.getTonSau()) + " " + donVi);

        // Mã phiếu chứng từ
        String maPhieu = item.getMaPhieu();
        if (maPhieu != null && !maPhieu.isEmpty()) {
            holder.txtMaPhieu.setText("Chứng từ: " + maPhieu);
        } else {
            holder.txtMaPhieu.setText("Chứng từ: -");
        }

        // Ghi chú
        String ghiChu = item.getGhiChu();
        if (ghiChu != null && !ghiChu.trim().isEmpty()) {
            holder.txtGhiChu.setVisibility(View.VISIBLE);
            holder.txtGhiChu.setText("Ghi chú: " + ghiChu);
        } else {
            holder.txtGhiChu.setVisibility(View.GONE);
        }

        // Ngày tạo
        holder.txtNgayTao.setText(item.getNgayTao());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtLoaiBienDong;
        TextView txtThayDoi;
        TextView txtMaPhieu;
        TextView txtTonSau;
        TextView txtGhiChu;
        TextView txtNgayTao;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtLoaiBienDong = itemView.findViewById(R.id.txt_loai_bien_dong);
            txtThayDoi = itemView.findViewById(R.id.txt_thay_doi);
            txtMaPhieu = itemView.findViewById(R.id.txt_ma_phieu);
            txtTonSau = itemView.findViewById(R.id.txt_ton_sau);
            txtGhiChu = itemView.findViewById(R.id.txt_ghi_chu);
            txtNgayTao = itemView.findViewById(R.id.txt_ngay_tao);
        }
    }
}
