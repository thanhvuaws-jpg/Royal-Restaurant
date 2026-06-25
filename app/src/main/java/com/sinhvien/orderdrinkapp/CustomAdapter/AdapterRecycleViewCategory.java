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
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.R;

import java.util.List;

/**
 * AdapterRecycleViewCategory - Adapter phụ hiển thị danh mục món ăn (dành riêng cho các màn hình phụ hoặc danh sách nhỏ gọn).
 * - Tương tự như AdapterDisplayCategory nhưng được tối giản hóa, hỗ trợ nạp một layout động truyền vào từ constructor (layout).
 * - Hiển thị: Tên loại thực đơn, hình ảnh tải từ URL VPS qua Glide, hoặc giải mã mảng bytes ảnh từ SQLite dự phòng.
 */
public class AdapterRecycleViewCategory extends RecyclerView.Adapter<AdapterRecycleViewCategory.ViewHolder>{

    Context context;
    // Resource ID của file Layout XML tùy biến
    int layout;
    List<LoaiMonDTO> loaiMonDTOList;

    public AdapterRecycleViewCategory(Context context, int layout, List<LoaiMonDTO> loaiMonDTOList){
        this.context = context;
        this.layout = layout;
        this.loaiMonDTOList = loaiMonDTOList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AdapterRecycleViewCategory.ViewHolder holder, int position) {
        LoaiMonDTO loaiMonDTO = loaiMonDTOList.get(position);
        holder.txt_customcategory_CategoryName.setText(loaiMonDTO.getTenLoai());
        
        // Tải ảnh đại diện cho danh mục thực đơn
        if (loaiMonDTO.getHinhAnhPath() != null && !loaiMonDTO.getHinhAnhPath().isEmpty()) {
            String url = com.sinhvien.orderdrinkapp.Utils.ViewUtils.getImageUrl(loaiMonDTO.getHinhAnhPath());
            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_dash_menu)
                    .error(R.drawable.ic_dash_menu)
                    .into(holder.img_customcategory_CategoryImage);
        } else if (loaiMonDTO.getHinhAnh() != null) {
            byte[] categoryimage = loaiMonDTO.getHinhAnh();
            Bitmap bitmap = BitmapFactory.decodeByteArray(categoryimage, 0, categoryimage.length);
            holder.img_customcategory_CategoryImage.setImageBitmap(bitmap);
        } else {
            holder.img_customcategory_CategoryImage.setImageResource(R.drawable.ic_dash_menu);
        }
    }

    @Override
    public int getItemCount() {
        return loaiMonDTOList.size();
    }

    /**
     * ViewHolder lưu giữ các view thành phần danh mục.
     */
    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView txt_customcategory_CategoryName;
        ImageView img_customcategory_CategoryImage;

        public ViewHolder(@NonNull View itemView){
            super(itemView);
            txt_customcategory_CategoryName = itemView.findViewById(R.id.txt_customcategory_CategoryName);
            img_customcategory_CategoryImage = itemView.findViewById(R.id.img_customcategory_CategoryImage);
        }
    }

}
