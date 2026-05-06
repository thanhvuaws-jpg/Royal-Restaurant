package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sinhvien.orderdrinkapp.DTO.BanAnDTO;
import com.sinhvien.orderdrinkapp.DTO.DonDatDTO;
import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;
import com.sinhvien.orderdrinkapp.R;

import org.w3c.dom.Text;

import java.util.List;

public class AdapterDisplayStatistic extends BaseAdapter {

    Context context;
    int layout;
    List<DonDatDTO> donDatDTOS;
    ViewHolder viewHolder;

    public AdapterDisplayStatistic(Context context, int layout, List<DonDatDTO> donDatDTOS){
        this.context = context;
        this.layout = layout;
        this.donDatDTOS = donDatDTOS;
    }

    @Override
    public int getCount() {
        return donDatDTOS.size();
    }

    @Override
    public Object getItem(int position) {
        return donDatDTOS.get(position);
    }

    @Override
    public long getItemId(int position) {
        return donDatDTOS.get(position).getMaDonDat();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if(view == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(layout,parent,false);

            viewHolder.txt_customstatistic_OrderId = (TextView)view.findViewById(R.id.txt_customstatistic_OrderId);
            viewHolder.txt_customstatistic_OrderDate = (TextView)view.findViewById(R.id.txt_customstatistic_OrderDate);
            viewHolder.txt_customstatistic_StaffName = (TextView)view.findViewById(R.id.txt_customstatistic_StaffName);
            viewHolder.txt_customstatistic_TotalAmount = (TextView)view.findViewById(R.id.txt_customstatistic_TotalAmount);
            viewHolder.txt_customstatistic_Status = (TextView)view.findViewById(R.id.txt_customstatistic_Status);
            viewHolder.txt_customstatistic_TableName = (TextView)view.findViewById(R.id.txt_customstatistic_TableName);
            view.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) view.getTag();
        }
        DonDatDTO donDatDTO = donDatDTOS.get(position);

        viewHolder.txt_customstatistic_OrderId.setText("Mã đơn: "+donDatDTO.getMaDonDat());
        viewHolder.txt_customstatistic_OrderDate.setText(donDatDTO.getNgayDat());
        viewHolder.txt_customstatistic_TotalAmount.setText(donDatDTO.getTongTien()+" VNĐ");
        if (donDatDTO.getTinhTrang().equals("true"))
        {
            viewHolder.txt_customstatistic_Status.setText("Đã thanh toán");
        }else {
            viewHolder.txt_customstatistic_Status.setText("Chưa thanh toán");
        }
        viewHolder.txt_customstatistic_StaffName.setText(donDatDTO.getTenNV());
        viewHolder.txt_customstatistic_TableName.setText(donDatDTO.getTenBan());

        return view;
    }
    public class ViewHolder{
        TextView txt_customstatistic_OrderId, txt_customstatistic_OrderDate, txt_customstatistic_StaffName
                ,txt_customstatistic_TotalAmount,txt_customstatistic_Status, txt_customstatistic_TableName;

    }
}
