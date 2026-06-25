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

/**
 * AdapterDisplayMenuRecycler - Adapter quản lý hiển thị danh sách Món ăn (Dishes/Menu Items) dạng Grid.
 * - Trình bày thông tin món ăn: Hình ảnh, tên món, giá tiền được định dạng tiền tệ Việt Nam (VNĐ).
 * - Quản lý trạng thái Còn món / Hết món (Available/Unavailable):
 *   + Hiển thị Badge màu sắc tương ứng.
 *   + Đối với món hết hàng: Hiển thị một lớp phủ mờ (view_Overlay) và chặn người dùng chọn đặt.
 * - Hỗ trợ SwitchMaterial cho phép Admin bật/tắt nhanh trạng thái Còn/Hết món trực tiếp từ giao diện,
 *   tự động gọi API cập nhật trạng thái lên VPS và phát tín hiệu qua Socket.io ("refresh_orders", "menu_changed").
 * - Phân quyền Admin:
 *   + Hiển thị công cụ Sửa (mở AddMenuActivity) và Xóa (gọi API DELETE xóa món ăn).
 * - Xử lý click chọn món: Nếu bàn ăn khác 0, mở màn hình chọn số lượng món (AmountMenuActivity).
 */
public class AdapterDisplayMenuRecycler extends RecyclerView.Adapter<AdapterDisplayMenuRecycler.ViewHolder> {

    private final Context context;
    private final List<MonDTO> monDTOList;
    // Mã bàn ăn hiện hành đang thao tác gọi món
    private final int maban;
    // Cờ kiểm tra tài khoản là Admin
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
        // Trạng thái còn món: true = còn món, false = hết món
        boolean coMon = "true".equals(monDTO.getTinhTrang());

        holder.txt_DishName.setText(monDTO.getTenMon());

        // Định dạng hiển thị giá tiền (ví dụ: 25.000 VNĐ)
        try {
            long gia = Long.parseLong(monDTO.getGiaTien().replace(",", "").replace(".", "").trim());
            holder.txt_DishPrice.setText(String.format("%,d VNĐ", gia).replace(",", "."));
        } catch (Exception e) {
            holder.txt_DishPrice.setText(monDTO.getGiaTien() + " VNĐ");
        }

        // Tạo Background bo góc cho Badge trạng thái Còn/Hết món
        GradientDrawable badgeBg = (GradientDrawable)
                androidx.core.content.ContextCompat.getDrawable(context, R.drawable.round_corner_textview).mutate();
        if (coMon) {
            holder.txt_DishStatus.setText(context.getString(R.string.status_available));
            badgeBg.setColor(context.getResources().getColor(R.color.status_available));
            holder.view_Overlay.setVisibility(View.GONE); // Ẩn lớp phủ mờ khi còn hàng
        } else {
            holder.txt_DishStatus.setText(context.getString(R.string.status_unavailable));
            badgeBg.setColor(context.getResources().getColor(R.color.status_unavailable));
            holder.view_Overlay.setVisibility(View.VISIBLE); // Hiện lớp phủ mờ báo hết hàng
        }
        holder.txt_DishStatus.setBackground(badgeBg);

        // Nạp ảnh món ăn bằng Glide (hoặc giải mã mảng bytes dự phòng)
        if (monDTO.getHinhAnhUrl() != null && !monDTO.getHinhAnhUrl().isEmpty()) {
            String url = com.sinhvien.orderdrinkapp.Utils.ViewUtils.getImageUrl(monDTO.getHinhAnhUrl());
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

        // Đăng ký sự kiện thay đổi trạng thái Còn/Hết món nhanh qua Switch (Dành cho Admin)
        holder.sw_ToggleStatus.setOnCheckedChangeListener(null);
        holder.sw_ToggleStatus.setChecked(coMon);
        holder.sw_ToggleStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String trangThaiMoi = isChecked ? "true" : "false";
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            // Gửi API cập nhật trạng thái món ăn
            apiService.updateDishStatus(monDTO.getMaMon(), trangThaiMoi).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                    if (response.isSuccessful()) {
                        monDTO.setTinhTrang(trangThaiMoi);
                        int currentPos = monDTOList.indexOf(monDTO);
                        if (currentPos >= 0) notifyItemChanged(currentPos);
                        
                        // Phát tín hiệu Socket.io thông báo cho các thiết bị nhân viên khác cập nhật
                        io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                        if (socket != null && socket.connected()) {
                            socket.emit("refresh_orders");
                            socket.emit("menu_changed");
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

        // Bật/tắt thanh công cụ Admin
        if (isAdmin) {
            holder.sw_ToggleStatus.setVisibility(View.VISIBLE);
            holder.layout_AdminTools.setVisibility(View.VISIBLE);
            holder.img_Edit.setOnClickListener(v -> {
                // Mở màn hình AddMenuActivity để sửa thông tin món ăn
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
                            // Gọi API xóa món ăn
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

                                        io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                                        if (socket != null && socket.connected()) {
                                            socket.emit("menu_changed");
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

        // Bắt sự kiện click chọn món
        holder.itemView.setOnClickListener(v -> {
            if (maban != 0) {
                if ("true".equals(monDTO.getTinhTrang())) {
                    // Nếu còn món, chuyển sang màn hình AmountMenuActivity để chọn số lượng
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

    /**
     * ViewHolder nắm giữ cấu trúc hiển thị món ăn.
     */
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
