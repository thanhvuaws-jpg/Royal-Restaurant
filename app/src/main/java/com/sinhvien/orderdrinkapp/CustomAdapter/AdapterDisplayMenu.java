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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sinhvien.orderdrinkapp.Activities.AddMenuActivity;
import com.sinhvien.orderdrinkapp.Activities.AmountMenuActivity;
import com.sinhvien.orderdrinkapp.DTO.MonDTO;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import com.bumptech.glide.Glide;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class AdapterDisplayMenu extends BaseAdapter implements android.widget.Filterable {

    Context context;
    int layout;
    List<MonDTO> monDTOList;
    List<MonDTO> monDTOListFull;
    boolean isAdmin;
    int maban; // Mã bàn đang được phục vụ

    public AdapterDisplayMenu(Context context, int layout, List<MonDTO> monDTOList, int maban) {
        this.context = context;
        this.layout = layout;
        this.monDTOList = monDTOList;
        this.monDTOListFull = new ArrayList<>(monDTOList);
        this.isAdmin = SessionManager.isAdmin(context);
        this.maban = maban;
    }

    @Override
    public int getCount() { return monDTOList.size(); }

    @Override
    public Object getItem(int position) { return monDTOList.get(position); }

    @Override
    public long getItemId(int position) { return monDTOList.get(position).getMaMon(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout, parent, false);

            viewHolder.img_customdish_DishImage  = convertView.findViewById(R.id.img_customdish_DishImage);
            viewHolder.txt_customdish_DishName   = convertView.findViewById(R.id.txt_customdish_DishName);
            viewHolder.txt_customdish_DishStatus = convertView.findViewById(R.id.txt_customdish_DishStatus);
            viewHolder.txt_customdish_DishPrice  = convertView.findViewById(R.id.txt_customdish_DishPrice);
            viewHolder.sw_customdish_ToggleStatus = convertView.findViewById(R.id.sw_customdish_ToggleStatus);
            viewHolder.view_customdish_Overlay   = convertView.findViewById(R.id.view_customdish_Overlay);
            viewHolder.layout_AdminTools = convertView.findViewById(R.id.layout_customdish_AdminTools);
            viewHolder.img_Edit = convertView.findViewById(R.id.img_customdish_Edit);
            viewHolder.img_Delete = convertView.findViewById(R.id.img_customdish_Delete);
            
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        MonDTO monDTO = monDTOList.get(position);
        boolean coMon = "true".equals(monDTO.getTinhTrang());

        viewHolder.txt_customdish_DishName.setText(monDTO.getTenMon());
        
        // Format giá tiền có dấu chấm
        try {
            long price = Long.parseLong(monDTO.getGiaTien().replaceAll("[^0-9]", ""));
            String formattedPrice = java.text.NumberFormat.getIntegerInstance(java.util.Locale.GERMANY).format(price);
            viewHolder.txt_customdish_DishPrice.setText(formattedPrice + " VNĐ");
        } catch (Exception e) {
            viewHolder.txt_customdish_DishPrice.setText(monDTO.getGiaTien() + " VNĐ");
        }

        GradientDrawable badgeBg = (GradientDrawable)
                context.getResources().getDrawable(R.drawable.round_corner_textview).mutate();
        if (coMon) {
            viewHolder.txt_customdish_DishStatus.setText(context.getString(R.string.status_available));
            badgeBg.setColor(context.getResources().getColor(R.color.status_available));
            viewHolder.view_customdish_Overlay.setVisibility(View.GONE);
        } else {
            viewHolder.txt_customdish_DishStatus.setText(context.getString(R.string.status_unavailable));
            badgeBg.setColor(context.getResources().getColor(R.color.status_unavailable));
            viewHolder.view_customdish_Overlay.setVisibility(View.VISIBLE);
        }
        viewHolder.txt_customdish_DishStatus.setBackground(badgeBg);

        if (monDTO.getHinhAnhUrl() != null && !monDTO.getHinhAnhUrl().isEmpty()) {
            // Tải ảnh từ Server bằng Glide
            String url = ApiClient.getBaseUrl() + monDTO.getHinhAnhUrl();
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.cafe_americano)
                    .error(R.drawable.cafe_americano)
                    .into(viewHolder.img_customdish_DishImage);
        } else if (monDTO.getHinhAnh() != null) {
            // Tải ảnh từ SQLite cũ
            Bitmap bitmap = BitmapFactory.decodeByteArray(monDTO.getHinhAnh(), 0, monDTO.getHinhAnh().length);
            viewHolder.img_customdish_DishImage.setImageBitmap(bitmap);
        } else {
            viewHolder.img_customdish_DishImage.setImageResource(R.drawable.cafe_americano);
        }

        // Xử lý nút gạt trạng thái
        viewHolder.sw_customdish_ToggleStatus.setOnCheckedChangeListener(null);
        viewHolder.sw_customdish_ToggleStatus.setChecked(coMon);
        viewHolder.sw_customdish_ToggleStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String trangThaiMoi = isChecked ? "true" : "false";
            
            // Cập nhật lên Cloud VPS
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.updateDishStatus(monDTO.getMaMon(), trangThaiMoi).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                    if (response.isSuccessful()) {
                        monDTO.setTinhTrang(trangThaiMoi);
                        notifyDataSetChanged();
                    } else {
                        Toast.makeText(context, "Lỗi cập nhật Cloud", Toast.LENGTH_SHORT).show();
                        viewHolder.sw_customdish_ToggleStatus.setChecked(!isChecked);
                    }
                }

                @Override
                public void onFailure(Call<OrderResponse> call, Throwable t) {
                    Toast.makeText(context, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
                    viewHolder.sw_customdish_ToggleStatus.setChecked(!isChecked);
                }
            });
        });

        // Xử lý Admin Tools
        if (isAdmin) {
            viewHolder.layout_AdminTools.setVisibility(View.VISIBLE);
            viewHolder.img_Edit.setOnClickListener(v -> {
                Intent iEdit = new Intent(context, AddMenuActivity.class);
                iEdit.putExtra("mamon", monDTO.getMaMon());
                iEdit.putExtra("maLoai", monDTO.getMaLoai());
                context.startActivity(iEdit);
            });
            viewHolder.img_Delete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa món này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            ApiService apiService = ApiClient.getClient().create(ApiService.class);
                            apiService.manageDish("delete", monDTO.getMaMon(), "", "", 0, "", "").enqueue(new Callback<OrderResponse>() {
                                @Override
                                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                                    if (response.isSuccessful()) {
                                        monDTOList.remove(position);
                                        notifyDataSetChanged();
                                        Toast.makeText(context, "Đã xóa món khỏi Cloud", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<OrderResponse> call, Throwable t) {
                                    Toast.makeText(context, "Lỗi xóa Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        } else {
            viewHolder.layout_AdminTools.setVisibility(View.GONE);
        }

        // FIX: Xử lý click chọn món để đặt bàn
        convertView.setOnClickListener(v -> {
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

        return convertView;
    }

    @Override
    public android.widget.Filter getFilter() {
        return new android.widget.Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint.toString().toLowerCase().trim();
                List<MonDTO> filtered = new ArrayList<>();
                if (query.isEmpty()) {
                    filtered.addAll(monDTOListFull);
                } else {
                    for (MonDTO m : monDTOListFull) {
                        if (m.getTenMon().toLowerCase().contains(query)) filtered.add(m);
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                monDTOList = (List<MonDTO>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public class ViewHolder {
        ImageView img_customdish_DishImage, img_Edit, img_Delete;
        TextView txt_customdish_DishName, txt_customdish_DishPrice, txt_customdish_DishStatus;
        SwitchMaterial sw_customdish_ToggleStatus;
        View view_customdish_Overlay;
        LinearLayout layout_AdminTools;
    }
}
