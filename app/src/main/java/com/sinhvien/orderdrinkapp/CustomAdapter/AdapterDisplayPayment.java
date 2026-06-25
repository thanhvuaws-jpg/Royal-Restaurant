package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.DTO.ThanhToanDTO;
import com.sinhvien.orderdrinkapp.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * AdapterDisplayPayment - Adapter quản lý hiển thị các dòng món ăn kèm số lượng và đơn giá trong hóa đơn/biên lai thanh toán.
 * - Được sử dụng trong các màn hình xác nhận thanh toán (Payment/Invoice confirmation dialog hoặc Activity).
 * - Hiển thị:
 *   + Tên món ăn.
 *   + Số lượng đã gọi.
 *   + Đơn giá từng món (Định dạng phân cách hàng nghìn theo Locale của Đức giúp hiển thị dấu chấm đẹp mắt).
 *   + Hình ảnh món ăn được bo tròn thông qua CircleImageView.
 * - Tự động tải ảnh bất đồng bộ từ URL bằng thư viện Glide kết hợp cache.
 */
public class AdapterDisplayPayment extends RecyclerView.Adapter<AdapterDisplayPayment.ViewHolder> {

    private final Context context;
    private final List<ThanhToanDTO> thanhToanDTOList;

    public AdapterDisplayPayment(Context context, List<ThanhToanDTO> thanhToanDTOList) {
        this.context = context;
        this.thanhToanDTOList = thanhToanDTOList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_layout_paymentmenu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThanhToanDTO item = thanhToanDTOList.get(position);

        holder.txt_DishName.setText(item.getTenMon());
        holder.txt_Quantity.setText(String.valueOf(item.getSoLuong()));

        // Định dạng số tiền (Ví dụ: 10.000 VNĐ)
        String formattedPrice = java.text.NumberFormat
                .getIntegerInstance(java.util.Locale.GERMANY)
                .format(item.getGiaTien());
        holder.txt_Price.setText(formattedPrice + " " + context.getString(R.string.currency_vnd));

        // Tải ảnh món ăn bo tròn bằng Glide
        if (item.getHinhAnhPath() != null && !item.getHinhAnhPath().isEmpty()) {
            String imageUrl = com.sinhvien.orderdrinkapp.Utils.ViewUtils.getImageUrl(item.getHinhAnhPath());
            Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.img_DishImage);
        } else {
            holder.img_DishImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() { return thanhToanDTOList.size(); }

    /**
     * ViewHolder nắm giữ cấu trúc dòng thanh toán món ăn.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Hình ảnh món ăn dạng tròn
        CircleImageView img_DishImage;
        TextView txt_DishName, txt_Quantity, txt_Price;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img_DishImage = itemView.findViewById(R.id.img_custompayment_DishImage);
            txt_DishName  = itemView.findViewById(R.id.txt_custompayment_DishName);
            txt_Quantity  = itemView.findViewById(R.id.txt_custompayment_Quantity);
            txt_Price     = itemView.findViewById(R.id.txt_custompayment_Price);
        }
    }
}
