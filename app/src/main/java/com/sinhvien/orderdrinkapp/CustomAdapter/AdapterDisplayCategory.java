package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sinhvien.orderdrinkapp.Activities.AddCategoryActivity;
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;

import com.bumptech.glide.Glide;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class AdapterDisplayCategory extends BaseAdapter {

    Context context;
    int layout;
    List<LoaiMonDTO> loaiMonDTOList ;
    boolean isAdmin;

    public AdapterDisplayCategory(Context context, int layout, List<LoaiMonDTO> loaiMonDTOList){
        this.context = context;
        this.layout = layout;
        this.loaiMonDTOList = loaiMonDTOList;
        this.isAdmin = SessionManager.isAdmin(context);
    }

    @Override
    public int getCount() { return loaiMonDTOList.size(); }

    @Override
    public Object getItem(int position) { return loaiMonDTOList.get(position); }

    @Override
    public long getItemId(int position) { return loaiMonDTOList.get(position).getMaLoai(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout,parent,false);

            viewHolder.img_CategoryImage = convertView.findViewById(R.id.img_customcategory_CategoryImage);
            viewHolder.txt_CategoryName = convertView.findViewById(R.id.txt_customcategory_CategoryName);
            viewHolder.layout_AdminTools = convertView.findViewById(R.id.layout_customcategory_AdminTools);
            viewHolder.img_Edit = convertView.findViewById(R.id.img_customcategory_Edit);
            viewHolder.img_Delete = convertView.findViewById(R.id.img_customcategory_Delete);
            
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        LoaiMonDTO loaiMonDTO = loaiMonDTOList.get(position);
        viewHolder.txt_CategoryName.setText(loaiMonDTO.getTenLoai());

        if (loaiMonDTO.getHinhAnhPath() != null && !loaiMonDTO.getHinhAnhPath().isEmpty()) {
            // Tải ảnh từ Server bằng Glide
            String url = ApiClient.getBaseUrl() + loaiMonDTO.getHinhAnhPath();
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.ic_dash_menu)
                    .error(R.drawable.ic_dash_menu)
                    .into(viewHolder.img_CategoryImage);
        } else if (loaiMonDTO.getHinhAnh() != null) {
            // Tải ảnh từ Database SQLite cũ
            Bitmap bitmap = BitmapFactory.decodeByteArray(loaiMonDTO.getHinhAnh(), 0, loaiMonDTO.getHinhAnh().length);
            viewHolder.img_CategoryImage.setImageBitmap(bitmap);
        }

        // Xử lý Admin Tools
        if (isAdmin) {
            viewHolder.layout_AdminTools.setVisibility(View.VISIBLE);
            viewHolder.img_Edit.setOnClickListener(v -> {
                Intent iEdit = new Intent(context, AddCategoryActivity.class);
                iEdit.putExtra("maloai", loaiMonDTO.getMaLoai());
                context.startActivity(iEdit);
            });
            viewHolder.img_Delete.setOnClickListener(v -> {
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
                                        notifyDataSetChanged();
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
            viewHolder.layout_AdminTools.setVisibility(View.GONE);
        }

        return convertView;
    }

    public class ViewHolder{
        TextView txt_CategoryName;
        ImageView img_CategoryImage, img_Edit, img_Delete;
        LinearLayout layout_AdminTools;
    }
}
