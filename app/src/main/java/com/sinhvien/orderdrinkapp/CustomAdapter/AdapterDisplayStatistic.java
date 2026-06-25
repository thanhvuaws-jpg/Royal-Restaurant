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
 * AdapterDisplayStatistic - Adapter quản lý hiển thị các dòng danh sách hóa đơn đơn hàng (Orders) trong báo cáo thống kê hoặc lịch sử thu ngân.
 * - Trình bày thông tin hóa đơn bao gồm:
 *   + Mã đơn đặt (OrderId).
 *   + Ngày giờ lập đơn (OrderDate).
 *   + Tên nhân viên phục vụ lập đơn (StaffName).
 *   + Tên bàn ăn đi kèm phương thức thanh toán ví dụ "Bàn số 3 (Tiền mặt)" hoặc "Mang đi (Chuyển khoản)" (TableName).
 *   + Tổng giá trị đơn hàng (TotalAmount).
 *   + Badge trạng thái đơn hàng (Đã thanh toán / Chờ duyệt / Chưa thanh toán) với màu viền tương thích.
 * - Hỗ trợ cập nhật nhanh dữ liệu thông qua phương thức updateData().
 */
public class AdapterDisplayStatistic extends RecyclerView.Adapter<AdapterDisplayStatistic.ViewHolder> {

    private final Context context;
    private final List<DonDatDTO> donDatDTOS;

    // Interface truyền sự kiện click dòng hóa đơn về cho Activity/Fragment gọi nó
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public AdapterDisplayStatistic(Context context, List<DonDatDTO> donDatDTOS) {
        this.context = context;
        this.donDatDTOS = donDatDTOS;
    }

    /**
     * Cập nhật mới danh sách đơn hàng và thông báo làm mới RecyclerView.
     */
    public void updateData(List<DonDatDTO> newList) {
        if (this.donDatDTOS != newList) {
            this.donDatDTOS.clear();
            this.donDatDTOS.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_layout_displaystatistic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DonDatDTO don = donDatDTOS.get(position);

        holder.txt_OrderId.setText("Mã đơn: " + don.getMaDonDat());
        holder.txt_OrderDate.setText(don.getNgayDat());
        holder.txt_TotalAmount.setText(don.getTongTien() + " VNĐ");
        holder.txt_StaffName.setText(don.getTenNV());
        
        // Tạo chuỗi văn bản bàn ăn đi kèm phương thức thanh toán
        String banText = don.getTenBan();
        if (don.getPhuongThucTT() != null && !don.getPhuongThucTT().isEmpty()) {
            banText += " (" + don.getPhuongThucTT() + ")";
        }
        holder.txt_TableName.setText(banText);

        // Thiết lập trạng thái hóa đơn kèm background viền tương ứng
        if ("true".equals(don.getTinhTrang())) {
            holder.txt_Status.setText("Đã thanh toán");
            holder.txt_Status.setBackgroundResource(R.drawable.corner_border_primary); // Viền xanh lá
        } else if ("pending".equals(don.getTinhTrang())) {
            holder.txt_Status.setText("Chờ duyệt");
            holder.txt_Status.setBackgroundResource(R.drawable.corner_border_black);   // Viền đen
        } else {
            holder.txt_Status.setText("Chưa thanh toán");
            holder.txt_Status.setBackgroundResource(R.drawable.corner_border_black);   // Viền đen
        }

        // Bắt sự kiện click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() { return donDatDTOS.size(); }

    /**
     * ViewHolder chứa cấu trúc dòng hóa đơn thống kê.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt_OrderId, txt_OrderDate, txt_StaffName, txt_TotalAmount, txt_Status, txt_TableName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txt_OrderId     = itemView.findViewById(R.id.txt_customstatistic_OrderId);
            txt_OrderDate   = itemView.findViewById(R.id.txt_customstatistic_OrderDate);
            txt_StaffName   = itemView.findViewById(R.id.txt_customstatistic_StaffName);
            txt_TotalAmount = itemView.findViewById(R.id.txt_customstatistic_TotalAmount);
            txt_Status      = itemView.findViewById(R.id.txt_customstatistic_Status);
            txt_TableName   = itemView.findViewById(R.id.txt_customstatistic_TableName);
        }
    }
}
