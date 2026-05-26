package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.DTO.MonDTO;
import com.sinhvien.orderdrinkapp.R;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreorderDishesAdapter extends RecyclerView.Adapter<PreorderDishesAdapter.ViewHolder> {

    private Context context;
    private List<MonDTO> dishList;
    private Map<Integer, Integer> quantityMap = new HashMap<>(); // Key: MaMon, Value: SoLuong
    private OnPreorderQuantityChanged listener;

    public interface OnPreorderQuantityChanged {
        void onQuantityChanged(Map<Integer, Integer> quantities);
    }

    public PreorderDishesAdapter(Context context, List<MonDTO> dishList, OnPreorderQuantityChanged listener) {
        this.context = context;
        this.dishList = dishList;
        this.listener = listener;
    }

    public Map<Integer, Integer> getSelectedQuantities() {
        return quantityMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_preorder_dish, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonDTO dish = dishList.get(position);

        holder.txt_dish_name.setText(dish.getTenMon());
        
        long price = 0;
        try {
            price = Long.parseLong(dish.getGiaTien());
        } catch (NumberFormatException ignored) {}
        DecimalFormat formatter = new DecimalFormat("#,###");
        holder.txt_dish_price.setText(formatter.format(price) + " đ");

        // Load image
        if (dish.getHinhAnhUrl() != null && !dish.getHinhAnhUrl().isEmpty()) {
            String url = ApiClient.getBaseUrl() + dish.getHinhAnhUrl();
            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.cafe_americano)
                    .error(R.drawable.cafe_americano)
                    .into(holder.img_dish);
        } else if (dish.getHinhAnh() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(dish.getHinhAnh(), 0, dish.getHinhAnh().length);
            holder.img_dish.setImageBitmap(bitmap);
        } else {
            holder.img_dish.setImageResource(R.drawable.cafe_americano);
        }

        // Qty
        int qty = quantityMap.containsKey(dish.getMaMon()) ? quantityMap.get(dish.getMaMon()) : 0;
        holder.txt_qty.setText(String.valueOf(qty));

        holder.img_plus.setOnClickListener(v -> {
            int newQty = (quantityMap.containsKey(dish.getMaMon()) ? quantityMap.get(dish.getMaMon()) : 0) + 1;
            quantityMap.put(dish.getMaMon(), newQty);
            holder.txt_qty.setText(String.valueOf(newQty));
            if (listener != null) {
                listener.onQuantityChanged(quantityMap);
            }
        });

        holder.img_minus.setOnClickListener(v -> {
            int currentQty = quantityMap.containsKey(dish.getMaMon()) ? quantityMap.get(dish.getMaMon()) : 0;
            if (currentQty > 0) {
                int newQty = currentQty - 1;
                if (newQty == 0) {
                    quantityMap.remove(dish.getMaMon());
                } else {
                    quantityMap.put(dish.getMaMon(), newQty);
                }
                holder.txt_qty.setText(String.valueOf(newQty));
                if (listener != null) {
                    listener.onQuantityChanged(quantityMap);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return dishList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img_dish, img_minus, img_plus;
        TextView txt_dish_name, txt_dish_price, txt_qty;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img_dish = itemView.findViewById(R.id.img_preorder_dish);
            img_minus = itemView.findViewById(R.id.img_preorder_minus);
            img_plus = itemView.findViewById(R.id.img_preorder_plus);
            txt_dish_name = itemView.findViewById(R.id.txt_preorder_dish_name);
            txt_dish_price = itemView.findViewById(R.id.txt_preorder_dish_price);
            txt_qty = itemView.findViewById(R.id.txt_preorder_qty);
        }
    }
}
