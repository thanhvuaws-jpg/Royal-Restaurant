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

public class AdapterDisplayTable extends RecyclerView.Adapter<AdapterDisplayTable.ViewHolder> {

    private final Context context;
    private final List<BanAnDTO> banAnDTOList;
    private final FragmentManager fragmentManager;
    private final boolean isAdmin;
    private List<com.sinhvien.orderdrinkapp.Api.TableResponse> reservedTables;

    public AdapterDisplayTable(Context context, List<BanAnDTO> banAnDTOList) {
        this.context = context;
        this.banAnDTOList = banAnDTOList;
        this.fragmentManager = ((HomeActivity) context).getSupportFragmentManager();
        this.isAdmin = SessionManager.isAdmin(context);
    }

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

        holder.img_TableImage.setImageResource(dangDung
                ? R.drawable.ic_baseline_event_seat_40
                : R.drawable.ic_baseline_airline_seat_legroom_normal_40);

        GradientDrawable badge = (GradientDrawable) context.getResources()
                .getDrawable(R.drawable.round_corner_textview).mutate();
        if (dangDung) {
            holder.txt_Status.setText("Đang dùng");
            badge.setColor(context.getResources().getColor(R.color.status_occupied));
            holder.txt_ActionHint.setText("Nhấn để xem đơn & thanh toán");
        } else if (isReserved) {
            holder.txt_Status.setText("Đã đặt");
            badge.setColor(android.graphics.Color.parseColor("#FFAB40")); // Orange/yellow
            String timeStr = reservedInfo.getThoigianhen();
            if (timeStr != null && timeStr.contains(" ")) {
                String[] parts = timeStr.split(" ");
                if (parts.length > 1) {
                    timeStr = parts[1].substring(0, 5); // HH:mm
                }
            }
            holder.txt_ActionHint.setText("Giờ hẹn: " + timeStr);
        } else {
            holder.txt_Status.setText("Trống");
            badge.setColor(context.getResources().getColor(R.color.status_free));
            holder.txt_ActionHint.setText("Nhấn để đặt món");
        }
        holder.txt_Status.setBackground(badge);

        // Nút xóa bàn (chỉ Admin)
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

        holder.itemView.setOnClickListener(v -> xuLyClickBan(position));
    }

    private void xuLyClickBan(int position) {
        BanAnDTO ban = banAnDTOList.get(position);
        int maban = ban.getMaBan();
        String tenban = ban.getTenBan();
        boolean dangDung = "true".equals(ban.getTinhTrang());
        String ngaydat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // Kiểm tra xem bàn có đang được đặt trước không
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
            DisplayCategoryFragment fragment = new DisplayCategoryFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("maban", maban);
            fragment.setArguments(bundle);

            ((HomeActivity) context).navigateToSubFragment(fragment, "hienthibanan");
        }
    }

    @Override
    public int getItemCount() { return banAnDTOList.size(); }

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
