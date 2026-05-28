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

public class AdapterDisplayStaff extends RecyclerView.Adapter<AdapterDisplayStaff.ViewHolder> {

    private final Context context;
    private final List<NhanVienDTO> nhanVienDTOList;

    // Interface để Fragment bắt sự kiện click/giữ
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
        if (nv.getMAQUYEN() == 1) {
            holder.txt_Role.setText("QUẢN LÝ");
        } else if (nv.getMAQUYEN() == 3) {
            holder.txt_Role.setText("THU NGÂN");
        } else if (nv.getMAQUYEN() == 4) {
            holder.txt_Role.setText("KHÁCH HÀNG");
        } else {
            holder.txt_Role.setText("NHÂN VIÊN");
        }

        // Xử lý click
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
