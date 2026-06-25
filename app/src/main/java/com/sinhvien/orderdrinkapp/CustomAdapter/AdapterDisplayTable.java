package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Activities.PaymentActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.DTO.BanAnDTO;
import com.sinhvien.orderdrinkapp.Fragments.DisplayCategoryFragment;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AdapterDisplayTable - Adapter quản lý hiển thị trạng thái bàn ăn (Table Status) trên lưới RecyclerView.
 * - Xác định và hiển thị trạng thái bàn ăn qua 3 màu sắc/nội dung badge trực quan:
 *   1. Đang dùng (Màu đỏ - R.color.status_occupied): Bàn đang có khách ngồi ăn/uống và có hóa đơn đang phục vụ.
 *   2. Đã đặt trước (Màu cam - #FFAB40): Bàn đã được khách đặt giữ lịch qua ứng dụng, có giờ hẹn cụ thể.
 *   3. Trống (Màu xanh - R.color.status_available): Bàn sẵn sàng đón tiếp khách mới.
 * - Thay đổi hình ảnh icon ghế ngồi/bàn ăn tùy thuộc trạng thái để tăng tính trực quan.
 * - Phân quyền Admin: Cho phép hiển thị nút Xóa bàn ăn trống (chặn xóa bàn đang dùng). Gọi API DELETE và cập nhật giao diện lập tức.
 * - Xử lý click chọn bàn:
 *   + Nếu bàn trống: Chuyển hướng sang màn hình gọi món (DisplayCategoryFragment) đính kèm mã bàn.
 *   + Nếu bàn đang dùng: Gọi API lấy mã đơn đặt (Order) gắn liền với bàn, chuyển sang PaymentActivity để xem chi tiết hoặc thanh toán.
 *   + Nếu bàn đã đặt trước: Hiển thị cảnh báo chờ khách check-in.
 */
public class AdapterDisplayTable extends RecyclerView.Adapter<AdapterDisplayTable.ViewHolder> {

    private final Context context;
    private final List<BanAnDTO> banAnDTOList;
    private final boolean isAdmin;
    // Danh sách lưu trữ thông tin các bàn đã được đặt lịch hẹn từ server
    private List<com.sinhvien.orderdrinkapp.Api.TableResponse> reservedTables;

    public AdapterDisplayTable(Context context, List<BanAnDTO> banAnDTOList) {
        this.context = context;
        this.banAnDTOList = banAnDTOList;
        this.isAdmin = SessionManager.isAdmin(context);
    }

    /**
     * Nạp danh sách các bàn đặt lịch hẹn và thực hiện vẽ lại giao diện.
     */
    public void setReservedTables(List<com.sinhvien.orderdrinkapp.Api.TableResponse> reservedTables) {
        this.reservedTables = reservedTables;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_layout_displaytable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BanAnDTO ban = banAnDTOList.get(position);
        boolean dangDung = "true".equals(ban.getTinhTrang());

        holder.txt_TableName.setText(ban.getTenBan());

        // Kiểm tra xem bàn này có nằm trong danh sách đặt lịch hẹn không
        com.sinhvien.orderdrinkapp.Api.TableResponse reservedInfo = null;
        if (reservedTables != null) {
            for (com.sinhvien.orderdrinkapp.Api.TableResponse r : reservedTables) {
                if (r.getMaBan() == ban.getMaBan()) {
                    reservedInfo = r;
                    break;
                }
            }
        }
        boolean isReserved = reservedInfo != null;

        // Thay đổi icon bàn ăn theo trạng thái
        holder.img_TableImage.setImageResource(dangDung
                ? R.drawable.ic_baseline_event_seat_40                  // Icon ghế đã có người ngồi
                : R.drawable.ic_baseline_airline_seat_legroom_normal_40); // Icon ghế trống

        GradientDrawable badge = (GradientDrawable) ContextCompat
                .getDrawable(context, R.drawable.round_corner_textview).mutate();
        if (dangDung) {
            holder.txt_Status.setText("Đang dùng");
            badge.setColor(ContextCompat.getColor(context, R.color.status_occupied));
            holder.txt_ActionHint.setText("Nhấn để xem đơn & thanh toán");
        } else if (isReserved) {
            holder.txt_Status.setText("Đã đặt");
            badge.setColor(android.graphics.Color.parseColor("#FFAB40")); 
            String timeStr = reservedInfo.getThoigianhen();
            if (timeStr != null && timeStr.contains(" ")) {
                String[] parts = timeStr.split(" ");
                if (parts.length > 1) {
                    timeStr = parts[1].substring(0, 5); // Cắt lấy định dạng HH:mm
                }
            }
            holder.txt_ActionHint.setText("Giờ hẹn: " + timeStr);
        } else {
            holder.txt_Status.setText("Trống");
            badge.setColor(ContextCompat.getColor(context, R.color.status_available));
            holder.txt_ActionHint.setText("Nhấn để đặt món");
        }
        holder.txt_Status.setBackground(badge);

        // Bật/tắt nút xóa bàn dành cho Admin
        if (isAdmin) {
            holder.img_Delete.setVisibility(View.VISIBLE);
            holder.img_Delete.setOnClickListener(v -> {
                if (dangDung) {
                    Toast.makeText(context, "Bàn đang dùng không thể xóa!", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(context)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa bàn này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            ApiService apiService = ApiClient.getClient().create(ApiService.class);
                            // Gọi API xóa bàn ăn khỏi hệ thống
                            apiService.deleteTable(ban.getMaBan()).enqueue(new Callback<OrderResponse>() {
                                @Override
                                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                                    if (response.isSuccessful()) {
                                        banAnDTOList.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, banAnDTOList.size());
                                        Toast.makeText(context, "Đã xóa bàn trên Cloud", Toast.LENGTH_SHORT).show();
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
            holder.img_Delete.setVisibility(View.GONE);
        }

        // Đăng ký click sự kiện chọn bàn ăn
        holder.itemView.setOnClickListener(v -> xuLyClickBan(position));
    }

    /**
     * Điều hướng thông minh dựa trên trạng thái hiện tại của bàn ăn.
     */
    private void xuLyClickBan(int position) {
        BanAnDTO ban = banAnDTOList.get(position);
        int maban = ban.getMaBan();
        String tenban = ban.getTenBan();
        boolean dangDung = "true".equals(ban.getTinhTrang());
        String ngaydat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // Kiểm tra xem bàn có đang bị khóa giữ trước không
        boolean isReserved = false;
        if (reservedTables != null) {
            for (com.sinhvien.orderdrinkapp.Api.TableResponse r : reservedTables) {
                if (r.getMaBan() == maban) {
                    isReserved = true;
                    break;
                }
            }
        }

        if (isReserved) {
            Toast.makeText(context, "Bàn đã được đặt trước, chờ khách check-in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dangDung) {
            // Nếu bàn đang dùng -> lấy thông tin hóa đơn và chuyển sang màn hình Thanh toán/Xem hóa đơn
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.getOrderByTable(maban).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                        int madondat = response.body().getMaDonDat();
                        Intent intent = new Intent(context, PaymentActivity.class);
                        intent.putExtra("maban", maban);
                        intent.putExtra("tenban", tenban);
                        intent.putExtra("ngaydat", ngaydat);
                        intent.putExtra("madondat", madondat);
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "Không tìm thấy đơn hàng trên Cloud", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<OrderResponse> call, Throwable t) {
                    Toast.makeText(context, "Lỗi kết nối Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Bàn đang trống -> Chuyển sang màn hình chọn loại món ăn để bắt đầu đặt đơn
            DisplayCategoryFragment fragment = new DisplayCategoryFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("maban", maban);
            fragment.setArguments(bundle);

            ((HomeActivity) context).navigateToSubFragment(fragment, "hienthibanan");
        }
    }

    @Override
    public int getItemCount() { return banAnDTOList.size(); }

    /**
     * ViewHolder chứa cấu trúc hiển thị 1 ô bàn ăn.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img_TableImage, img_Delete;
        TextView txt_TableName, txt_Status, txt_ActionHint;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img_TableImage  = itemView.findViewById(R.id.img_customtable_TableImage);
            txt_TableName   = itemView.findViewById(R.id.txt_customtable_TableName);
            txt_Status      = itemView.findViewById(R.id.txt_customtable_Status);
            txt_ActionHint  = itemView.findViewById(R.id.txt_customtable_ActionHint);
            img_Delete      = itemView.findViewById(R.id.img_customtable_Delete);
        }
    }
}
