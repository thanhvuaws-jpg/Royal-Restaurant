package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;
import com.sinhvien.orderdrinkapp.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterDisplayStaff extends BaseAdapter {

    Context context;
    int layout;
    List<NhanVienDTO> nhanVienDTOList;

    public AdapterDisplayStaff(Context context, int layout, List<NhanVienDTO> nhanVienDTOList){
        this.context = context;
        this.layout = layout;
        this.nhanVienDTOList = nhanVienDTOList;
    }

    @Override
    public int getCount() {
        return nhanVienDTOList.size();
    }

    @Override
    public Object getItem(int position) {
        return nhanVienDTOList.get(position);
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
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout,parent,false);

            holder.img_custom_staff_Avatar = (CircleImageView)convertView.findViewById(R.id.img_custom_staff_Avatar);
            holder.txt_custom_staff_Name = (TextView)convertView.findViewById(R.id.txt_custom_staff_Name);
            holder.txt_custom_staff_RoleName = (TextView)convertView.findViewById(R.id.txt_custom_staff_RoleName);
            holder.txt_custom_staff_Phone = (TextView)convertView.findViewById(R.id.txt_custom_staff_Phone);
            holder.txt_custom_staff_Email = (TextView)convertView.findViewById(R.id.txt_custom_staff_Email);

            convertView.setTag(holder);
        }else {
            holder = (ViewHolder)convertView.getTag();
        }

        NhanVienDTO nhanVienDTO = nhanVienDTOList.get(position);

        holder.txt_custom_staff_Name.setText(nhanVienDTO.getHOTENNV());
        holder.txt_custom_staff_Phone.setText(nhanVienDTO.getSDT());
        holder.txt_custom_staff_Email.setText(nhanVienDTO.getEMAIL());

        if(nhanVienDTO.getMAQUYEN() == 1){
            holder.txt_custom_staff_RoleName.setText("QUẢN LÝ");
        }else {
            holder.txt_custom_staff_RoleName.setText("NHÂN VIÊN");
        }

        return convertView;
    }

    public class ViewHolder{
        CircleImageView img_custom_staff_Avatar;
        TextView txt_custom_staff_Name, txt_custom_staff_RoleName, txt_custom_staff_Phone, txt_custom_staff_Email;
    }
}
