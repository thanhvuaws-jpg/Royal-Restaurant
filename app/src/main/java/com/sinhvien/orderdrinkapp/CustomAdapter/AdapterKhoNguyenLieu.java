package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sinhvien.orderdrinkapp.Api.NguyenLieuResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * AdapterKhoNguyenLieu - Hiển thị danh sách tồn kho nguyên vật liệu.
 * - Sử dụng DiffUtil để tối ưu cập nhật danh sách mượt mà.
 * - Tải ảnh thumbnail 200px (getUrlAnhNho) qua Glide.
 * - Hiển thị huy hiệu trạng thái tồn (Âm kho, Hết hàng, Sắp hết, Đầy đủ).
 */
public class AdapterKhoNguyenLieu extends RecyclerView.Adapter<AdapterKhoNguyenLieu.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(NguyenLieuResponse item);
    }

    private final Context context;
    private final List<NguyenLieuResponse> items = new ArrayList<>();
    private final OnItemClickListener listener;
    private final DecimalFormat df = new DecimalFormat("#,##0.##");

    public AdapterKhoNguyenLieu(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<NguyenLieuResponse> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(this.items, newItems));
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        diffResult.dispatchUpdatesTo(this);
    }

    public List<NguyenLieuResponse> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_kho_nguyen_lieu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NguyenLieuResponse item = items.get(position);

        holder.txtTenNl.setText(item.getTenNL());
        holder.txtTenNhom.setText(item.getTenNhom());

        String tonStr = "Tồn: " + df.format(item.getTonHienTai()) + " " + item.getDonVi();
        holder.txtTonHienTai.setText(tonStr);

        // Huy hiệu Ngừng dùng
        if (!item.isHoatDong()) {
            holder.txtNgungHoatDong.setVisibility(View.VISIBLE);
            GradientDrawable ngungBg = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.round_corner_textview).mutate();
            ngungBg.setColor(ContextCompat.getColor(context, R.color.kho_ngung));
            holder.txtNgungHoatDong.setBackground(ngungBg);
        } else {
            holder.txtNgungHoatDong.setVisibility(View.GONE);
        }

        // Trạng thái tồn: am, het, sap_het, du
        GradientDrawable statusBg = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.round_corner_textview).mutate();
        String status = item.getTinhTrangTon();
        if ("am".equalsIgnoreCase(status)) {
            holder.txtTinhTrangTon.setText("Âm kho");
            statusBg.setColor(ContextCompat.getColor(context, R.color.kho_am));
        } else if ("het".equalsIgnoreCase(status)) {
            holder.txtTinhTrangTon.setText("Hết hàng");
            statusBg.setColor(ContextCompat.getColor(context, R.color.kho_het));
        } else if ("sap_het".equalsIgnoreCase(status)) {
            holder.txtTinhTrangTon.setText("Sắp hết");
            statusBg.setColor(ContextCompat.getColor(context, R.color.kho_sap_het));
        } else {
            holder.txtTinhTrangTon.setText("Đầy đủ");
            statusBg.setColor(ContextCompat.getColor(context, R.color.kho_du));
        }
        holder.txtTinhTrangTon.setBackground(statusBg);

        // Tải ảnh 200px qua Glide
        String rawPath = item.getUrlAnhNho();
        String fullUrl = ViewUtils.getImageUrl(rawPath);
        if (!fullUrl.isEmpty()) {
            Glide.with(context)
                    .load(fullUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.cafe_americano)
                    .error(R.drawable.cafe_americano)
                    .into(holder.imgNguyenLieu);
        } else {
            holder.imgNguyenLieu.setImageResource(R.drawable.cafe_americano);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgNguyenLieu;
        TextView txtTenNl;
        TextView txtTenNhom;
        TextView txtTonHienTai;
        TextView txtTinhTrangTon;
        TextView txtNgungHoatDong;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgNguyenLieu = itemView.findViewById(R.id.img_nguyen_lieu);
            txtTenNl = itemView.findViewById(R.id.txt_ten_nl);
            txtTenNhom = itemView.findViewById(R.id.txt_ten_nhom);
            txtTonHienTai = itemView.findViewById(R.id.txt_ton_hientai);
            txtTinhTrangTon = itemView.findViewById(R.id.txt_tinh_trang_ton);
            txtNgungHoatDong = itemView.findViewById(R.id.txt_ngung_hoat_dong);
        }
    }

    private static class DiffCallback extends DiffUtil.Callback {
        private final List<NguyenLieuResponse> oldList;
        private final List<NguyenLieuResponse> newList;

        DiffCallback(List<NguyenLieuResponse> oldList, List<NguyenLieuResponse> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList != null ? oldList.size() : 0;
        }

        @Override
        public int getNewListSize() {
            return newList != null ? newList.size() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getMaNL() == newList.get(newItemPosition).getMaNL();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            NguyenLieuResponse o = oldList.get(oldItemPosition);
            NguyenLieuResponse n = newList.get(newItemPosition);
            return o.getTonHienTai() == n.getTonHienTai()
                    && o.getTonToiThieu() == n.getTonToiThieu()
                    && o.getTenNL().equals(n.getTenNL())
                    && o.getTinhTrangTon().equals(n.getTinhTrangTon())
                    && o.isHoatDong() == n.isHoatDong();
        }
    }
}
