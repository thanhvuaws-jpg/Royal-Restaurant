package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sinhvien.orderdrinkapp.Activities.AddCategoryActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdapterDisplayCategory extends RecyclerView.Adapter<AdapterDisplayCategory.ViewHolder> {

    private final Context context;
    private final List<LoaiMonDTO> loaiMonDTOList;
    private final boolean isAdmin;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public AdapterDisplayCategory(Context context, List<LoaiMonDTO> loaiMonDTOList) {
        this.context = context;
        this.loaiMonDTOList = loaiMonDTOList;
        this.isAdmin = SessionManager.isAdmin(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_layout_displaycategory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LoaiMonDTO loaiMonDTO = loaiMonDTOList.get(position);
        holder.txt_CategoryName.setText(loaiMonDTO.getTenLoai());

        // Tải ảnh bằng Glide
        if (loaiMonDTO.getHinhAnhPath() != null && !loaiMonDTO.getHinhAnhPath().isEmpty()) {
            String url = ApiClient.getBaseUrl() + loaiMonDTO.getHinhAnhPath();
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.ic_dash_menu)
                    .error(R.drawable.ic_dash_menu)
                    .into(holder.img_CategoryImage);
        } else if (loaiMonDTO.getHinhAnh() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(loaiMonDTO.getHinhAnh(), 0, loaiMonDTO.getHinhAnh().length);
            holder.img_CategoryImage.setImageBitmap(bitmap);
        }

        // Admin tools
        if (isAdmin) {
            holder.layout_AdminTools.setVisibility(View.VISIBLE);
            holder.img_Edit.setOnClickListener(v -> {
                Intent iEdit = new Intent(context, AddCategoryActivity.class);
                iEdit.putExtra("maloai", loaiMonDTO.getMaLoai());
                context.startActivity(iEdit);
            });
            holder.img_Delete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa loại thực đơn này sẽ xóa tất cả món ăn bên trong. Bạn chắc chứ?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            ApiService apiService = ApiClient.getClient().create(ApiService.class);
                            apiService.manageCategory("delete", loaiMonDTO.getMaLoai(), "", "").enqueue(new Callback<OrderResponse>() {
                                @Override
                                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                                    if (response.isSuccessful()) {
                                        loaiMonDTOList.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, loaiMonDTOList.size());
                                        Toast.makeText(context, "Đã xóa loại thực đơn", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "Lỗi xóa Cloud", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<OrderResponse> call, Throwable t) {
                                    Toast.makeText(context, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        } else {
            holder.layout_AdminTools.setVisibility(View.GONE);
        }

        // Click item để chuyển sang danh sách món
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() { return loaiMonDTOList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt_CategoryName;
        ImageView img_CategoryImage, img_Edit, img_Delete;
        LinearLayout layout_AdminTools;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txt_CategoryName  = itemView.findViewById(R.id.txt_customcategory_CategoryName);
            img_CategoryImage = itemView.findViewById(R.id.img_customcategory_CategoryImage);
            layout_AdminTools = itemView.findViewById(R.id.layout_customcategory_AdminTools);
            img_Edit          = itemView.findViewById(R.id.img_customcategory_Edit);
            img_Delete        = itemView.findViewById(R.id.img_customcategory_Delete);
        }
    }
}
