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

        String formattedPrice = java.text.NumberFormat
                .getIntegerInstance(java.util.Locale.GERMANY)
                .format(item.getGiaTien());
        holder.txt_Price.setText(formattedPrice + " " + context.getString(R.string.currency_vnd));

        // Tải ảnh bằng Glide
        if (item.getHinhAnhPath() != null && !item.getHinhAnhPath().isEmpty()) {
            String imageUrl = ApiClient.BASE_URL + item.getHinhAnhPath();
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
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
