package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sinhvien.orderdrinkapp.Api.NguyenLieuResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdapterKiemKe - Adapter hiển thị danh sách nguyên liệu để đếm kiểm kê.
 * Tuân thủ yêu cầu 5.5:
 * "Ô để trống nghĩa là không đếm, khác số 0."
 */
public class AdapterKiemKe extends RecyclerView.Adapter<AdapterKiemKe.ViewHolder> {

    private final Context context;
    private final List<NguyenLieuResponse> list = new ArrayList<>();
    // Lưu các mục thực tế có nhập: Map<maNL, soDemThucTe>
    private final Map<Integer, Double> thucTeMap = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("#,##0.##");

    public AdapterKiemKe(Context context) {
        this.context = context;
    }

    public void setData(List<NguyenLieuResponse> newList) {
        list.clear();
        thucTeMap.clear();
        if (newList != null) {
            list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public Map<Integer, Double> getThucTeMap() {
        return thucTeMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_kiem_ke, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NguyenLieuResponse item = list.get(position);

        holder.txtTenNL.setText(item.getTenNL());
        holder.txtTonSo.setText("Sổ: " + df.format(item.getTonHienTai()) + " " + item.getDonVi());
        holder.txtDonVi.setText(item.getDonVi());

        // Tải ảnh thumbnail 200px
        String fullUrl = ViewUtils.getImageUrl(item.getUrlAnhNho());
        if (!fullUrl.isEmpty()) {
            Glide.with(context)
                    .load(fullUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.cafe_americano)
                    .error(R.drawable.cafe_americano)
                    .into(holder.imgKiemKe);
        } else {
            holder.imgKiemKe.setImageResource(R.drawable.cafe_americano);
        }

        // Xóa TextWatcher cũ để tránh gán nhầm khi tái sử dụng view
        if (holder.textWatcher != null) {
            holder.edtThucTe.removeTextChangedListener(holder.textWatcher);
        }

        // Điền lại giá trị nếu đã từng nhập
        if (thucTeMap.containsKey(item.getMaNL())) {
            Double val = thucTeMap.get(item.getMaNL());
            holder.edtThucTe.setText(val != null ? df.format(val) : "");
        } else {
            holder.edtThucTe.setText("");
        }

        // Lắng nghe thay đổi nhập liệu
        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s != null ? s.toString().trim() : "";
                if (input.isEmpty()) {
                    // Ô để trống -> Không đếm (Requirement 5.5)
                    thucTeMap.remove(item.getMaNL());
                } else {
                    try {
                        double val = Double.parseDouble(input);
                        if (val >= 0) {
                            thucTeMap.put(item.getMaNL(), val);
                        } else {
                            thucTeMap.remove(item.getMaNL());
                        }
                    } catch (Exception e) {
                        thucTeMap.remove(item.getMaNL());
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        holder.edtThucTe.addTextChangedListener(holder.textWatcher);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgKiemKe;
        TextView txtTenNL;
        TextView txtTonSo;
        EditText edtThucTe;
        TextView txtDonVi;
        TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgKiemKe = itemView.findViewById(R.id.img_kiem_ke);
            txtTenNL = itemView.findViewById(R.id.txt_ten_nl);
            txtTonSo = itemView.findViewById(R.id.txt_ton_so);
            edtThucTe = itemView.findViewById(R.id.edt_thuc_te);
            txtDonVi = itemView.findViewById(R.id.txt_don_vi);
        }
    }
}
