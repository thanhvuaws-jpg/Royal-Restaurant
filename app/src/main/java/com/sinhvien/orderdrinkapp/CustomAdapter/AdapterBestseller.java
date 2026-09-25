package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sinhvien.orderdrinkapp.Api.BestsellerResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * AdapterBestseller - Quản lý hiển thị dải món bán chạy (Bestseller) trên Android.
 * Hiển thị tên món, giá và lượng bán trung bình mỗi ngày (TB: x phần/ngày).
 * Click vào món để thêm vào đơn đang mở hoặc giỏ đặt trước.
 */
public class AdapterBestseller extends RecyclerView.Adapter<AdapterBestseller.ViewHolder> {

    public interface OnBestsellerItemClickListener {
        void onBestsellerClick(BestsellerResponse.BestsellerItem item);
    }

    private final Context context;
    private final List<BestsellerResponse.BestsellerItem> list = new ArrayList<>();
    private final OnBestsellerItemClickListener listener;
    private final DecimalFormat priceDf = com.sinhvien.orderdrinkapp.Utils.TienTe.dinhDang();
    private final DecimalFormat tbDf = new DecimalFormat("#0.#");

    public AdapterBestseller(Context context, OnBestsellerItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<BestsellerResponse.BestsellerItem> newList) {
        list.clear();
        if (newList != null) {
            list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_bestseller, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BestsellerResponse.BestsellerItem item = list.get(position);

        holder.txtRankBadge.setText("#" + (position + 1));
        holder.txtName.setText(item.getTenMon());
        holder.txtPrice.setText(priceDf.format(item.getGiaTien()) + " đ");
        holder.txtTb.setText("TB: " + tbDf.format(item.getTbMoiNgay()) + " phần/ngày");

        // Xu hướng bán
        String xuHuong = item.getXuHuong();
        if ("tang".equalsIgnoreCase(xuHuong)) {
            holder.txtXuHuong.setText("↗ Tăng");
            holder.txtXuHuong.setVisibility(View.VISIBLE);
        } else if ("giam".equalsIgnoreCase(xuHuong)) {
            holder.txtXuHuong.setText("↘ Giảm");
            holder.txtXuHuong.setVisibility(View.VISIBLE);
        } else {
            holder.txtXuHuong.setText("★ Hot");
            holder.txtXuHuong.setVisibility(View.VISIBLE);
        }

        // Tải ảnh món
        String imgUrl = ViewUtils.getImageUrl(item.getHinhAnh());
        if (!imgUrl.isEmpty()) {
            Glide.with(context)
                    .load(imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.cafe_americano)
                    .error(R.drawable.cafe_americano)
                    .into(holder.imgBestseller);
        } else {
            holder.imgBestseller.setImageResource(R.drawable.cafe_americano);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBestsellerClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBestseller;
        TextView txtRankBadge;
        TextView txtXuHuong;
        TextView txtName;
        TextView txtPrice;
        TextView txtTb;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBestseller = itemView.findViewById(R.id.img_bestseller);
            txtRankBadge = itemView.findViewById(R.id.txt_rank_badge);
            txtXuHuong = itemView.findViewById(R.id.txt_xu_huong);
            txtName = itemView.findViewById(R.id.txt_bestseller_name);
            txtPrice = itemView.findViewById(R.id.txt_bestseller_price);
            txtTb = itemView.findViewById(R.id.txt_bestseller_tb);
        }
    }
}
