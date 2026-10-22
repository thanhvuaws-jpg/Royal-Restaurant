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
    // Chữ người dùng gõ, giữ nguyên để vẽ lại khi cuộn. Trước đây vẽ lại bằng
    // df.format(...) có dấu phân cách hàng nghìn ("1.234,5" ở máy tiếng Việt),
    // gõ tiếp thì parseDouble hỏng và dòng đó lặng lẽ biến khỏi lượt đếm.
    private final Map<Integer, String> goMap = new HashMap<>();
    // Ô gõ không phải số: báo đỏ tại ô, chặn gửi (trước đây bị bỏ qua im lặng).
    private final Map<Integer, String> oSai = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("#,##0.##");

    public AdapterKiemKe(Context context) {
        this.context = context;
    }

    public void setData(List<NguyenLieuResponse> newList) {
        list.clear();
        thucTeMap.clear();
        goMap.clear();
        oSai.clear();
        if (newList != null) {
            list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public Map<Integer, Double> getThucTeMap() {
        return thucTeMap;
    }

    /** Tên nguyên liệu của ô gõ sai đầu tiên, hoặc null nếu mọi ô hợp lệ. */
    public String tenOSaiDau() {
        return oSai.isEmpty() ? null : oSai.values().iterator().next();
    }

    /**
     * Đọc số đếm: nhận "12", "1.5", "1,5" (dấu phẩy thập phân kiểu Việt).
     * Trả null nếu không phải số — máy chủ cũng kiểm lại theo đúng luật này.
     */
    public static Double docSo(String s) {
        String t = s == null ? "" : s.trim();
        if (!t.matches("\\d+([.,]\\d+)?")) return null;
        try {
            return Double.parseDouble(t.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
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

        // Điền lại đúng chữ đã gõ (không định dạng lại), kèm báo lỗi nếu ô đó sai
        String daGo = goMap.get(item.getMaNL());
        holder.edtThucTe.setText(daGo != null ? daGo : "");
        holder.edtThucTe.setError(oSai.containsKey(item.getMaNL()) ? "Không phải số" : null);

        // Lắng nghe thay đổi nhập liệu
        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s != null ? s.toString().trim() : "";
                int ma = item.getMaNL();
                if (input.isEmpty()) {
                    // Ô để trống -> Không đếm (Requirement 5.5)
                    thucTeMap.remove(ma);
                    goMap.remove(ma);
                    oSai.remove(ma);
                    holder.edtThucTe.setError(null);
                    return;
                }
                goMap.put(ma, input);
                Double val = docSo(input);
                if (val == null) {
                    thucTeMap.remove(ma);
                    oSai.put(ma, item.getTenNL());
                    holder.edtThucTe.setError("Không phải số");
                } else {
                    thucTeMap.put(ma, val);
                    oSai.remove(ma);
                    holder.edtThucTe.setError(null);
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
