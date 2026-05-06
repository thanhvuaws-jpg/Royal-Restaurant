package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.DTO.ThanhToanDTO;
import com.sinhvien.orderdrinkapp.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterDisplayPayment extends BaseAdapter {

    Context context;
    int layout;
    List<ThanhToanDTO> thanhToanDTOList;

    public AdapterDisplayPayment(Context context, int layout, List<ThanhToanDTO> thanhToanDTOList){
        this.context = context;
        this.layout = layout;
        this.thanhToanDTOList = thanhToanDTOList;
    }

    @Override
    public int getCount() {
        return thanhToanDTOList.size();
    }

    @Override
    public Object getItem(int position) {
        return thanhToanDTOList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){
            holder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout,parent,false);

            holder.img_custompayment_DishImage = (CircleImageView)convertView.findViewById(R.id.img_custompayment_DishImage);
            holder.txt_custompayment_DishName = (TextView)convertView.findViewById(R.id.txt_custompayment_DishName);
            holder.txt_custompayment_Quantity = (TextView)convertView.findViewById(R.id.txt_custompayment_Quantity);
            holder.txt_custompayment_Price = (TextView)convertView.findViewById(R.id.txt_custompayment_Price);

            convertView.setTag(holder);
        }else {
            holder = (ViewHolder)convertView.getTag();
        }
        ThanhToanDTO thanhToanDTO = thanhToanDTOList.get(position);

        holder.txt_custompayment_DishName.setText(thanhToanDTO.getTenMon());
        holder.txt_custompayment_Quantity.setText(String.valueOf(thanhToanDTO.getSoLuong()));
        
        String formattedPrice = java.text.NumberFormat.getIntegerInstance(java.util.Locale.GERMANY).format(thanhToanDTO.getGiaTien());
        holder.txt_custompayment_Price.setText(formattedPrice + " " + context.getResources().getString(R.string.currency_vnd));

        // TẢI ẢNH TỪ CLOUD BẰNG GLIDE
        if (thanhToanDTO.getHinhAnhPath() != null && !thanhToanDTO.getHinhAnhPath().isEmpty()) {
            String imageUrl = ApiClient.BASE_URL + thanhToanDTO.getHinhAnhPath();
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery) // Ảnh chờ mặc định hệ thống
                    .error(android.R.drawable.ic_menu_gallery)       // Ảnh lỗi mặc định hệ thống
                    .into(holder.img_custompayment_DishImage);
        } else {
            holder.img_custompayment_DishImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        return convertView;
    }

    public class ViewHolder{
        CircleImageView img_custompayment_DishImage;
        TextView txt_custompayment_DishName, txt_custompayment_Quantity, txt_custompayment_Price;
    }
}
