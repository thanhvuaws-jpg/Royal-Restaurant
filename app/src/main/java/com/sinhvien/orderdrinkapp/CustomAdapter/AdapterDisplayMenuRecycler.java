package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sinhvien.orderdrinkapp.Activities.AddMenuActivity;
import com.sinhvien.orderdrinkapp.Activities.AmountMenuActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.DTO.MonDTO;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdapterDisplayMenuRecycler extends RecyclerView.Adapter<AdapterDisplayMenuRecycler.ViewHolder> {

    private final Context context;
    private final List<MonDTO> monDTOList;
    private final int maban;
    private final boolean isAdmin;

    public AdapterDisplayMenuRecycler(Context context, List<MonDTO> monDTOList, int maban) {
        this.context = context;
        this.monDTOList = monDTOList;
        this.maban = maban;
        this.isAdmin = SessionManager.isAdmin(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_layout_displaymenu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonDTO monDTO = monDTOList.get(position);
        boolean coMon = "true".equals(monDTO.getTinhTrang());

        holder.txt_DishName.setText(monDTO.getTenMon());

        // Format giá tiền
        try {
            long gia = Long.parseLong(monDTO.getGiaTien().replace(",", "").replace(".", "").trim());
            holder.txt_DishPrice.setText(String.format("%,d VNĐ", gia).replace(",", "."));
        } catch (Exception e) {
            holder.txt_DishPrice.setText(monDTO.getGiaTien() + " VNĐ");
        }

        // Badge trạng thái
        GradientDrawable badgeBg = (GradientDrawable)
                androidx.core.content.ContextCompat.getDrawable(context, R.drawable.round_corner_textview).mutate();
        if (coMon) {
            holder.txt_DishStatus.setText(context.getString(R.string.status_available));
            badgeBg.setColor(context.getResources().getColor(R.color.status_available));
            holder.view_Overlay.setVisibility(View.GONE);
        } else {
            holder.txt_DishStatus.setText(context.getString(R.string.status_unavailable));
            badgeBg.setColor(context.getResources().getColor(R.color.status_unavailable));
            holder.view_Overlay.setVisibility(View.VISIBLE);
        }
        holder.txt_DishStatus.setBackground(badgeBg);

        // Tải ảnh bằng Glide (vẫn dùng Glide để load ảnh)
        if (monDTO.getHinhAnhUrl() != null && !monDTO.getHinhAnhUrl().isEmpty()) {
            String url = ApiClient.getBaseUrl() + monDTO.getHinhAnhUrl();
            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.cafe_americano)
                    .error(R.drawable.cafe_americano)
                    .into(holder.img_DishImage);
        } else if (monDTO.getHinhAnh() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(monDTO.getHinhAnh(), 0, monDTO.getHinhAnh().length);
            holder.img_DishImage.setImageBitmap(bitmap);
        } else {
            holder.img_DishImage.setImageResource(R.drawable.cafe_americano);
        }

        // Toggle trạng thái
        holder.sw_ToggleStatus.setOnCheckedChangeListener(null);
        holder.sw_ToggleStatus.setChecked(coMon);
        holder.sw_ToggleStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String trangThaiMoi = isChecked ? "true" : "false";
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.updateDishStatus(monDTO.getMaMon(), trangThaiMoi).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                    if (response.isSuccessful()) {
                        monDTO.setTinhTrang(trangThaiMoi);
                        int currentPos = monDTOList.indexOf(monDTO);
                        if (currentPos >= 0) notifyItemChanged(currentPos);
                        
                        io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                        if (socket != null && socket.connected()) {
                            socket.emit("refresh_orders");
                        }
                    } else {
                        Toast.makeText(context, "Lỗi cập nhật Cloud", Toast.LENGTH_SHORT).show();
                        holder.sw_ToggleStatus.setChecked(!isChecked);
                    }
                }

                @Override
                public void onFailure(Call<OrderResponse> call, Throwable t) {
                    Toast.makeText(context, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
                    holder.sw_ToggleStatus.setChecked(!isChecked);
                }
            });
        });

        // Nút Admin: Sửa / Xóa
        if (isAdmin) {
            holder.sw_ToggleStatus.setVisibility(View.VISIBLE);
            holder.layout_AdminTools.setVisibility(View.VISIBLE);
            holder.img_Edit.setOnClickListener(v -> {
                Intent iEdit = new Intent(context, AddMenuActivity.class);
                iEdit.putExtra("mamon", monDTO.getMaMon());
                iEdit.putExtra("maLoai", monDTO.getMaLoai());
                context.startActivity(iEdit);
            });
            holder.img_Delete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa món này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            ApiService apiService = ApiClient.getClient().create(ApiService.class);
                            apiService.manageDish("delete", monDTO.getMaMon(), "", "", 0, "", "").enqueue(new Callback<OrderResponse>() {
                                @Override
                                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                                    if (response.isSuccessful()) {
                                        int currentPos = monDTOList.indexOf(monDTO);
                                        if (currentPos >= 0) {
                                            monDTOList.remove(currentPos);
                                            notifyItemRemoved(currentPos);
                                            notifyItemRangeChanged(currentPos, monDTOList.size());
                                        }
                                        Toast.makeText(context, "Đã xóa món", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<OrderResponse> call, Throwable t) {
                                    Toast.makeText(context, "Lỗi xóa: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        } else {
            holder.sw_ToggleStatus.setVisibility(View.GONE);
            holder.layout_AdminTools.setVisibility(View.GONE);
        }

        // Click chọn món để đặt bàn
        holder.itemView.setOnClickListener(v -> {
            if (maban != 0) {
                if ("true".equals(monDTO.getTinhTrang())) {
                    Intent iAmount = new Intent(context, AmountMenuActivity.class);
                    iAmount.putExtra("maban", maban);
                    iAmount.putExtra("mamon", monDTO.getMaMon());
                    context.startActivity(iAmount);
                } else {
                    Toast.makeText(context, R.string.dish_out_of_stock_msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return monDTOList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img_DishImage, img_Edit, img_Delete;
        TextView txt_DishName, txt_DishPrice, txt_DishStatus;
        SwitchMaterial sw_ToggleStatus;
        View view_Overlay;
        LinearLayout layout_AdminTools;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img_DishImage    = itemView.findViewById(R.id.img_customdish_DishImage);
            txt_DishName     = itemView.findViewById(R.id.txt_customdish_DishName);
            txt_DishPrice    = itemView.findViewById(R.id.txt_customdish_DishPrice);
            txt_DishStatus   = itemView.findViewById(R.id.txt_customdish_DishStatus);
            sw_ToggleStatus  = itemView.findViewById(R.id.sw_customdish_ToggleStatus);
            view_Overlay     = itemView.findViewById(R.id.view_customdish_Overlay);
            layout_AdminTools = itemView.findViewById(R.id.layout_customdish_AdminTools);
            img_Edit         = itemView.findViewById(R.id.img_customdish_Edit);
            img_Delete       = itemView.findViewById(R.id.img_customdish_Delete);
        }
    }
}
