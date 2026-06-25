package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.DTO.DonDatDTO;
import com.sinhvien.orderdrinkapp.R;

import java.util.List;

/**
 * AdapterRecycleViewStatistic - Adapter phụ hiển thị danh sách đơn đặt để thống kê.
 * - Hỗ trợ nạp một file layout XML tùy chọn được truyền vào từ constructor (layout).
 * - Trình bày thông tin đơn đặt: Mã đơn đặt, ngày lập, tổng tiền (ẩn nếu tổng tiền là 0), trạng thái (Đã/Chưa thanh toán), tên nhân viên, tên bàn.
 */
public class AdapterRecycleViewStatistic extends RecyclerView.Adapter<AdapterRecycleViewStatistic.ViewHolder>{

    Context context;
    // Resource ID của layout dòng danh sách tùy biến
    int layout;
    List<DonDatDTO> donDatDTOList;

    public AdapterRecycleViewStatistic(Context context, int layout, List<DonDatDTO> donDatDTOList){
        this.context = context;
        this.layout = layout;
        this.donDatDTOList = donDatDTOList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AdapterRecycleViewStatistic.ViewHolder holder, int position) {
        DonDatDTO donDatDTO = donDatDTOList.get(position);
        holder.txt_customstatistic_OrderId.setText("Mã đơn: #"+donDatDTO.getMaDonDat());
        holder.txt_customstatistic_OrderDate.setText(donDatDTO.getNgayDat());
        
        // Ẩn tổng tiền nếu đơn chưa phát sinh chi phí
        if("0".equals(donDatDTO.getTongTien())) {
            holder.txt_customstatistic_TotalAmount.setVisibility(View.INVISIBLE);
        } else {
            holder.txt_customstatistic_TotalAmount.setVisibility(View.VISIBLE);
            holder.txt_customstatistic_TotalAmount.setText(donDatDTO.getTongTien() + " VNĐ");
        }

        // Dịch trạng thái thanh toán đơn hàng sang văn bản tiếng Việt
        if ("true".equals(donDatDTO.getTinhTrang())) {
            holder.txt_customstatistic_Status.setText("Đã thanh toán");
        } else {
            holder.txt_customstatistic_Status.setText("Chưa thanh toán");
        }
        holder.txt_customstatistic_StaffName.setText(donDatDTO.getTenNV());
        holder.txt_customstatistic_TableName.setText(donDatDTO.getTenBan());
    }

    @Override
    public int getItemCount() {
        return donDatDTOList.size();
    }

    /**
     * ViewHolder chứa cấu trúc dòng hiển thị thống kê đơn đặt.
     */
    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView txt_customstatistic_OrderId, txt_customstatistic_OrderDate, txt_customstatistic_StaffName,
                txt_customstatistic_TableName, txt_customstatistic_TotalAmount, txt_customstatistic_Status;

        public ViewHolder(@NonNull View itemView){
            super(itemView);
            txt_customstatistic_OrderId = itemView.findViewById(R.id.txt_customstatistic_OrderId);
            txt_customstatistic_OrderDate = itemView.findViewById(R.id.txt_customstatistic_OrderDate);
            txt_customstatistic_StaffName = itemView.findViewById(R.id.txt_customstatistic_StaffName);
            txt_customstatistic_TableName = itemView.findViewById(R.id.txt_customstatistic_TableName);
            txt_customstatistic_TotalAmount = itemView.findViewById(R.id.txt_customstatistic_TotalAmount);
            txt_customstatistic_Status = itemView.findViewById(R.id.txt_customstatistic_Status);
        }
    }
}
