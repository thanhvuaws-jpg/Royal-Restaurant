package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;
import com.sinhvien.orderdrinkapp.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * AdapterDisplayStaff - Adapter quản lý hiển thị danh sách Nhân viên & Khách hàng.
 * - Trình bày các thông tin cơ bản: Họ tên, Số điện thoại, Email, Chức vụ.
 * - Ánh xạ mã quyền (MAQUYEN) sang văn bản hiển thị:
 *   + 1: QUẢN LÝ
 *   + 3: THU NGÂN
 *   + 4: KHÁCH HÀNG
 *   + Khác: NHÂN VIÊN (Phục vụ)
 * - Tích hợp interface OnItemClickListener hỗ trợ:
 *   + Click thường (chọn chỉnh sửa thông tin).
 *   + Click giữ lâu (Long Click) để mở menu tùy chọn nâng cao như Xóa tài khoản.
 */
public class AdapterDisplayStaff extends RecyclerView.Adapter<AdapterDisplayStaff.ViewHolder> {

    private final Context context;
    private final List<NhanVienDTO> nhanVienDTOList;

    // Interface truyền sự kiện click và giữ lâu về cho Fragment
    public interface OnItemClickListener {
        void onItemClick(int position);
        void onItemLongClick(int position);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public AdapterDisplayStaff(Context context, List<NhanVienDTO> nhanVienDTOList) {
        this.context = context;
        this.nhanVienDTOList = nhanVienDTOList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_layout_displaystaff, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NhanVienDTO nv = nhanVienDTOList.get(position);

        holder.txt_Name.setText(nv.getHOTENNV());
        holder.txt_Phone.setText(nv.getSDT());
        holder.txt_Email.setText(nv.getEMAIL());
        
        // Chuyển đổi mã quyền sang chuỗi chữ viết hoa hiển thị rõ chức năng
        if (nv.getMAQUYEN() == 1) {
            holder.txt_Role.setText("QUẢN LÝ");
        } else if (nv.getMAQUYEN() == 3) {
            holder.txt_Role.setText("THU NGÂN");
        } else if (nv.getMAQUYEN() == 4) {
            holder.txt_Role.setText("KHÁCH HÀNG");
        } else {
            holder.txt_Role.setText("NHÂN VIÊN");
        }

        // Đăng ký sự kiện tương tác
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(position);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onItemLongClick(position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return nhanVienDTOList.size();
    }

    /**
     * ViewHolder chứa các view thành phần hiển thị thông tin nhân sự.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView img_Avatar;
        TextView txt_Name, txt_Role, txt_Phone, txt_Email;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img_Avatar = itemView.findViewById(R.id.img_custom_staff_Avatar);
            txt_Name   = itemView.findViewById(R.id.txt_custom_staff_Name);
            txt_Role   = itemView.findViewById(R.id.txt_custom_staff_RoleName);
            txt_Phone  = itemView.findViewById(R.id.txt_custom_staff_Phone);
            txt_Email  = itemView.findViewById(R.id.txt_custom_staff_Email);
        }
    }
}
