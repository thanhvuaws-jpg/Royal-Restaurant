package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Activities.PaymentActivity;
import com.sinhvien.orderdrinkapp.DTO.BanAnDTO;
import com.sinhvien.orderdrinkapp.DTO.DonDatDTO;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.Fragments.DisplayCategoryFragment;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdapterDisplayTable extends BaseAdapter {

    Context context;
    int layout;
    List<BanAnDTO> banAnDTOList;
    FragmentManager fragmentManager;
    boolean isAdmin;

    public AdapterDisplayTable(Context context, int layout, List<BanAnDTO> banAnDTOList) {
        this.context = context;
        this.layout = layout;
        this.banAnDTOList = banAnDTOList;
        this.fragmentManager = ((HomeActivity) context).getSupportFragmentManager();
        this.isAdmin = SessionManager.isAdmin(context);
    }

    @Override
    public int getCount() { return banAnDTOList.size(); }

    @Override
    public Object getItem(int position) { return banAnDTOList.get(position); }

    @Override
    public long getItemId(int position) { return banAnDTOList.get(position).getMaBan(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout, parent, false);

            vh.img_customtable_TableImage  = convertView.findViewById(R.id.img_customtable_TableImage);
            vh.txt_customtable_TableName   = convertView.findViewById(R.id.txt_customtable_TableName);
            vh.txt_customtable_Status      = convertView.findViewById(R.id.txt_customtable_Status);
            vh.txt_customtable_ActionHint  = convertView.findViewById(R.id.txt_customtable_ActionHint);
            vh.img_Delete = convertView.findViewById(R.id.img_customtable_Delete);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        BanAnDTO ban = banAnDTOList.get(position);
        boolean dangDung = "true".equals(ban.getTinhTrang());

        vh.txt_customtable_TableName.setText(ban.getTenBan());

        if (dangDung) {
            vh.img_customtable_TableImage.setImageResource(R.drawable.ic_baseline_event_seat_40);
        } else {
            vh.img_customtable_TableImage.setImageResource(R.drawable.ic_baseline_airline_seat_legroom_normal_40);
        }

        GradientDrawable badge = (GradientDrawable) context.getResources()
                .getDrawable(R.drawable.round_corner_textview).mutate();
        if (dangDung) {
            vh.txt_customtable_Status.setText("Đang dùng");
            badge.setColor(context.getResources().getColor(R.color.status_occupied));
            vh.txt_customtable_ActionHint.setText("Nhấn để xem đơn & thanh toán");
        } else {
            vh.txt_customtable_Status.setText("Trống");
            badge.setColor(context.getResources().getColor(R.color.status_free));
            vh.txt_customtable_ActionHint.setText("Nhấn để đặt món");
        }
        vh.txt_customtable_Status.setBackground(badge);

        // Xử lý nút Xóa bàn
        if (isAdmin) {
            vh.img_Delete.setVisibility(View.VISIBLE);
            vh.img_Delete.setOnClickListener(v -> {
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
                                        notifyDataSetChanged();
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
            vh.img_Delete.setVisibility(View.GONE);
        }

        convertView.setOnClickListener(v -> xuLyClickBan(position));

        return convertView;
    }

    private void xuLyClickBan(int position) {
        BanAnDTO ban = banAnDTOList.get(position);
        int maban = ban.getMaBan();
        String tenban = ban.getTenBan();
        boolean dangDung = "true".equals(ban.getTinhTrang());

        String ngaydat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        if (dangDung) {
            // LẤY MÃ ĐƠN HÀNG TỪ CLOUD TRƯỚC KHI THANH TOÁN
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
            // Mở màn hình chọn món (Mã đơn hàng sẽ được tạo bên AmountMenuActivity)
            DisplayCategoryFragment fragment = new DisplayCategoryFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("maban", maban);
            fragment.setArguments(bundle);

            FragmentTransaction tx = fragmentManager.beginTransaction();
            tx.replace(R.id.contentView, fragment);
            tx.addToBackStack("hienthibanan");
            tx.commit();
        }
    }

    public class ViewHolder {
        ImageView img_customtable_TableImage, img_Delete;
        TextView txt_customtable_TableName, txt_customtable_Status, txt_customtable_ActionHint;
    }
}
