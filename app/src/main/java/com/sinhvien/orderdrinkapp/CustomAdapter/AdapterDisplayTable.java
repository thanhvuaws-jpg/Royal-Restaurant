package com.sinhvien.orderdrinkapp.CustomAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.Activities.PaymentActivity;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.DTO.BanAnDTO;
import com.sinhvien.orderdrinkapp.Fragments.DisplayCategoryFragment;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AdapterDisplayTable - Adapter quản lý hiển thị trạng thái bàn ăn (Table Status) trên lưới RecyclerView.
 * - Xác định và hiển thị trạng thái bàn ăn qua 3 màu sắc/nội dung badge trực quan:
 *   1. Đang dùng (Màu đỏ - R.color.status_occupied): Bàn đang có khách ngồi ăn/uống và có hóa đơn đang phục vụ.
 *   2. Đã đặt trước (Màu cam - #FFAB40): Bàn đã được khách đặt giữ lịch qua ứng dụng, có giờ hẹn cụ thể.
 *   3. Trống (Màu xanh - R.color.status_available): Bàn sẵn sàng đón tiếp khách mới.
 * - Thay đổi hình ảnh icon ghế ngồi/bàn ăn tùy thuộc trạng thái để tăng tính trực quan.
 * - Phân quyền Admin: nút Xóa trên thẻ, và NHẤN GIỮ thẻ để sửa tên/ảnh, bật/tắt bảo trì hoặc xóa.
 *   Máy chủ chỉ cho xóa bàn chưa từng có đơn hay phiếu đặt; bị từ chối thì hiện đúng lý do
 *   (trước đây chỉ im lặng — không có dòng nào xử lý phản hồi lỗi).
 * - Xử lý click chọn bàn:
 *   + Nếu bàn trống: Chuyển hướng sang màn hình gọi món (DisplayCategoryFragment) đính kèm mã bàn.
 *   + Nếu bàn đang dùng: Gọi API lấy mã đơn đặt (Order) gắn liền với bàn, chuyển sang PaymentActivity để xem chi tiết hoặc thanh toán.
 *   + Nếu bàn đã đặt trước: Hiển thị cảnh báo chờ khách check-in.
 */
public class AdapterDisplayTable extends RecyclerView.Adapter<AdapterDisplayTable.ViewHolder> {

    private final Context context;
    private final List<BanAnDTO> banAnDTOList;
    private final boolean isAdmin;
    // Danh sách lưu trữ thông tin các bàn đã được đặt lịch hẹn từ server
    private List<com.sinhvien.orderdrinkapp.Api.TableResponse> reservedTables;

    /**
     * Việc cần Fragment làm sau thao tác quản lý bàn: mở màn sửa (Fragment
     * giữ ActivityResultLauncher) và nạp lại sơ đồ từ máy chủ.
     */
    public interface ThaoTacBan {
        void suaBan(BanAnDTO ban);
        void taiLaiDanhSach();
    }

    private ThaoTacBan thaoTacBan;

    public void setThaoTacBan(ThaoTacBan t) { this.thaoTacBan = t; }

    public AdapterDisplayTable(Context context, List<BanAnDTO> banAnDTOList) {
        this.context = context;
        this.banAnDTOList = new java.util.ArrayList<>(banAnDTOList);
        this.isAdmin = SessionManager.isAdmin(context);
    }

    public void updateData(List<BanAnDTO> newList) {
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult =
                androidx.recyclerview.widget.DiffUtil.calculateDiff(new TableDiffCallback(this.banAnDTOList, newList));
        this.banAnDTOList.clear();
        this.banAnDTOList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Nạp danh sách các bàn đặt lịch hẹn và thực hiện vẽ lại giao diện.
     */
    public void setReservedTables(List<com.sinhvien.orderdrinkapp.Api.TableResponse> reservedTables) {
        this.reservedTables = reservedTables;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_layout_displaytable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BanAnDTO ban = banAnDTOList.get(position);
        boolean dangDung = "true".equals(ban.getTinhTrang());

        holder.txt_TableName.setText(ban.getTenBan());

        // Kiểm tra xem bàn này có nằm trong danh sách đặt lịch hẹn không
        com.sinhvien.orderdrinkapp.Api.TableResponse reservedInfo = null;
        if (reservedTables != null) {
            for (com.sinhvien.orderdrinkapp.Api.TableResponse r : reservedTables) {
                if (r.getMaBan() == ban.getMaBan()) {
                    reservedInfo = r;
                    break;
                }
            }
        }
        boolean isReserved = reservedInfo != null;

        // Thay đổi icon bàn ăn theo trạng thái (icon chỉ hiện khi bàn chưa có ảnh)
        holder.img_TableImage.setImageResource(dangDung
                ? R.drawable.ic_baseline_event_seat_40                  // Icon ghế đã có người ngồi
                : R.drawable.ic_baseline_airline_seat_legroom_normal_40); // Icon ghế trống
        hienAnhBan(holder, ban);

        GradientDrawable badge = (GradientDrawable) ContextCompat
                .getDrawable(context, R.drawable.round_corner_textview).mutate();

        // Bàn đang bảo trì được xét TRƯỚC mọi trạng thái khác: dù có đang
        // trống hay đã được đặt trước thì nó cũng không phục vụ được.
        boolean dangBaoTri = "false".equals(ban.getHoatDong());

        if (dangBaoTri) {
            holder.txt_Status.setText("Bảo trì");
            badge.setColor(android.graphics.Color.parseColor("#78909C")); // xám xanh
            holder.txt_ActionHint.setText("Bàn tạm ngưng phục vụ");
            // Làm mờ cả thẻ để phân biệt rõ với bàn còn dùng được
            holder.itemView.setAlpha(0.55f);
        } else if (dangDung) {
            holder.txt_Status.setText("Đang dùng");
            badge.setColor(ContextCompat.getColor(context, R.color.status_occupied));
            holder.txt_ActionHint.setText("Nhấn để xem đơn & thanh toán");
        } else if (isReserved) {
            holder.txt_Status.setText("Đã đặt");
            badge.setColor(android.graphics.Color.parseColor("#FFAB40")); 
            String timeStr = reservedInfo.getThoigianhen();
            if (timeStr != null && timeStr.contains(" ")) {
                String[] parts = timeStr.split(" ");
                if (parts.length > 1) {
                    timeStr = parts[1].substring(0, 5); // Cắt lấy định dạng HH:mm
                }
            }
            holder.txt_ActionHint.setText("Giờ hẹn: " + timeStr);
        } else {
            holder.txt_Status.setText("Trống");
            badge.setColor(ContextCompat.getColor(context, R.color.status_available));
            holder.txt_ActionHint.setText("Nhấn để đặt món");
        }

        // BẮT BUỘC đặt lại độ mờ cho thẻ KHÔNG bảo trì.
        // RecyclerView tái sử dụng view, nên nếu chỉ đặt alpha ở nhánh bảo
        // trì mà không khôi phục ở đây, một thẻ từng dùng cho bàn bảo trì
        // sẽ tiếp tục mờ khi được tái dùng cho bàn bình thường.
        if (!dangBaoTri) {
            holder.itemView.setAlpha(1.0f);
        }

        holder.txt_Status.setBackground(badge);

        // Bật/tắt nút xóa bàn dành cho Admin
        if (isAdmin) {
            holder.img_Delete.setVisibility(View.VISIBLE);
            holder.img_Delete.setOnClickListener(v -> xacNhanXoaBan(ban));
            // Nhấn giữ: sửa tên & ảnh, bảo trì, xóa. Nhấn thường vẫn là gọi món.
            holder.itemView.setOnLongClickListener(v -> {
                moMenuQuanLyBan(ban);
                return true;
            });
        } else {
            holder.img_Delete.setVisibility(View.GONE);
            holder.itemView.setOnLongClickListener(null);
            holder.itemView.setLongClickable(false);
        }

        // Đăng ký click sự kiện chọn bàn ăn
        holder.itemView.setOnClickListener(v -> xuLyClickBan(position));
    }

    /* ═══════════════════════ Quản lý bàn (chỉ quản lý) ═══════════════════════ */

    private void moMenuQuanLyBan(BanAnDTO ban) {
        boolean dangBaoTri = "false".equals(ban.getHoatDong());
        String[] muc = {
                "Sửa tên & ảnh bàn",
                dangBaoTri ? "Cho bàn hoạt động lại" : "Chuyển sang bảo trì",
                "Xóa bàn"
        };
        new AlertDialog.Builder(context)
                .setTitle(ban.getTenBan())
                .setItems(muc, (d, which) -> {
                    if (which == 0) {
                        if (thaoTacBan != null) thaoTacBan.suaBan(ban);
                    } else if (which == 1) {
                        doiBaoTri(ban, dangBaoTri);
                    } else {
                        xacNhanXoaBan(ban);
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void doiBaoTri(BanAnDTO ban, boolean dangBaoTri) {
        ApiClient.getClient().create(ApiService.class)
                .doiHoatDongBan("toggle", ban.getMaBan(), dangBaoTri ? "true" : "false")
                .enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                        boolean ok = response.isSuccessful() && response.body() != null
                                && "success".equals(response.body().getStatus());
                        String msg = ok ? response.body().getMessage()
                                : ViewUtils.docLoiMayChu(response, "Không đổi được trạng thái bàn");
                        // Câu dài (còn phiếu đặt sắp tới) thì hiện hộp thoại cho đọc kịp.
                        new AlertDialog.Builder(context).setMessage(msg).setPositiveButton("Đã hiểu", null).show();
                        if (ok) daDoiBan();
                    }

                    @Override
                    public void onFailure(Call<OrderResponse> call, Throwable t) {
                        Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void xacNhanXoaBan(BanAnDTO ban) {
        if ("true".equals(ban.getTinhTrang())) {
            Toast.makeText(context, "Bàn đang dùng không thể xóa!", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle("Xóa " + ban.getTenBan() + "?")
                .setMessage("Chỉ xóa được bàn chưa từng có đơn hay phiếu đặt (tạo nhầm). "
                        + "Bàn đã dùng thì hãy chuyển sang bảo trì để giữ lịch sử.")
                .setPositiveButton("Xóa", (dialog, which) -> xoaBan(ban))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void xoaBan(BanAnDTO ban) {
        ApiClient.getClient().create(ApiService.class).deleteTable(ban.getMaBan()).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    Toast.makeText(context, "Đã xóa " + ban.getTenBan(), Toast.LENGTH_SHORT).show();
                    daDoiBan();
                    return;
                }
                // 409: bàn đã có đơn/phiếu đặt — máy chủ nói rõ và gợi ý bảo trì.
                new AlertDialog.Builder(context)
                        .setTitle("Không xóa được bàn")
                        .setMessage(ViewUtils.docLoiMayChu(response, "Không xóa được bàn (mã " + response.code() + ")"))
                        .setPositiveButton("Đã hiểu", null)
                        .show();
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                Toast.makeText(context, "Lỗi xóa: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sau khi đổi bàn thành công: nạp lại sơ đồ từ máy chủ (không vá tay theo
     * vị trí — vị trí đã cũ nếu danh sách vừa đổi) và báo các máy khác.
     */
    private void daDoiBan() {
        io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (socket != null && socket.connected()) socket.emit("refresh_orders");
        if (thaoTacBan != null) thaoTacBan.taiLaiDanhSach();
    }

    /**
     * Ảnh bàn ở đầu thẻ; bàn chưa có ảnh (hoặc tải lỗi) thì hiện icon ghế.
     *
     * Luôn gọi Glide.clear() khi không có ảnh: RecyclerView tái sử dụng thẻ,
     * nên một thẻ từng mang ảnh bàn VIP có thể được dùng lại cho bàn chưa có
     * ảnh — không xóa thì ảnh cũ vẫn nằm đó, sai bàn.
     */
    private void hienAnhBan(ViewHolder holder, BanAnDTO ban) {
        String url = ViewUtils.getImageUrl(ban.getAnhNho());
        if (url.isEmpty()) {
            Glide.with(context).clear(holder.img_Photo);
            holder.img_Photo.setVisibility(View.GONE);
            holder.img_TableImage.setVisibility(View.VISIBLE);
            return;
        }
        holder.img_Photo.setVisibility(View.VISIBLE);
        holder.img_TableImage.setVisibility(View.GONE);
        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                            boolean isFirstResource) {
                        // Tải lỗi (mất mạng, tệp bị xóa) thì quay về icon ghế
                        // thay vì để một khung kem trống.
                        holder.img_Photo.setVisibility(View.GONE);
                        holder.img_TableImage.setVisibility(View.VISIBLE);
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                            com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(holder.img_Photo);
    }

    /**
     * Điều hướng thông minh dựa trên trạng thái hiện tại của bàn ăn.
     */
    private void xuLyClickBan(int position) {
        BanAnDTO ban = banAnDTOList.get(position);

        // Chặn ngay từ đầu: bàn đang bảo trì thì không gọi món được.
        // Nếu để lọt, nhân viên sẽ mở đơn trên một bàn mà nhà hàng đã đánh
        // dấu là không phục vụ — và khách đặt trực tuyến cũng không được xếp
        // vào bàn đó, dẫn tới hai nguồn dữ liệu mâu thuẫn nhau.
        if ("false".equals(ban.getHoatDong())) {
            android.widget.Toast.makeText(context,
                    "Bàn \"" + ban.getTenBan() + "\" đang bảo trì, không thể gọi món.",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        int maban = ban.getMaBan();
        String tenban = ban.getTenBan();
        boolean dangDung = "true".equals(ban.getTinhTrang());
        String ngaydat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // Kiểm tra xem bàn có đang bị khóa giữ trước không
        boolean isReserved = false;
        if (reservedTables != null) {
            for (com.sinhvien.orderdrinkapp.Api.TableResponse r : reservedTables) {
                if (r.getMaBan() == maban) {
                    isReserved = true;
                    break;
                }
            }
        }

        if (isReserved) {
            Toast.makeText(context, "Bàn đã được đặt trước, chờ khách check-in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dangDung) {
            // Nếu bàn đang dùng -> lấy thông tin hóa đơn và chuyển sang màn hình Thanh toán/Xem hóa đơn
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.getOrderByTable(maban).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                        int madondat = response.body().getMaDonDat();
                        Intent intent = new Intent(context, PaymentActivity.class);
                        intent.putExtra("maban", maban);
                        intent.putExtra("tenban", tenban);
                        intent.putExtra("ngaydat", ngaydat);
                        intent.putExtra("madondat", madondat);
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "Không tìm thấy đơn hàng trên Cloud", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<OrderResponse> call, Throwable t) {
                    Toast.makeText(context, "Lỗi kết nối Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Bàn đang trống -> Chuyển sang màn hình chọn loại món ăn để bắt đầu đặt đơn
            DisplayCategoryFragment fragment = new DisplayCategoryFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("maban", maban);
            fragment.setArguments(bundle);

            ((HomeActivity) context).navigateToSubFragment(fragment, "hienthibanan");
        }
    }

    @Override
    public int getItemCount() { return banAnDTOList.size(); }

    /**
     * ViewHolder chứa cấu trúc hiển thị 1 ô bàn ăn.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img_TableImage, img_Delete, img_Photo;
        TextView txt_TableName, txt_Status, txt_ActionHint;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img_TableImage  = itemView.findViewById(R.id.img_customtable_TableImage);
            img_Photo       = itemView.findViewById(R.id.img_customtable_Photo);
            txt_TableName   = itemView.findViewById(R.id.txt_customtable_TableName);
            txt_Status      = itemView.findViewById(R.id.txt_customtable_Status);
            txt_ActionHint  = itemView.findViewById(R.id.txt_customtable_ActionHint);
            img_Delete      = itemView.findViewById(R.id.img_customtable_Delete);
        }
    }

    private static class TableDiffCallback extends androidx.recyclerview.widget.DiffUtil.Callback {
        private final List<BanAnDTO> oldList;
        private final List<BanAnDTO> newList;

        public TableDiffCallback(List<BanAnDTO> oldList, List<BanAnDTO> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getMaBan() == newList.get(newItemPosition).getMaBan();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            BanAnDTO oldItem = oldList.get(oldItemPosition);
            BanAnDTO newItem = newList.get(newItemPosition);
            // Phải so cả HOATDONG: thiếu nó thì khi quản lý bật/tắt bảo trì
            // từ web, DiffUtil sẽ coi hai bản ghi là giống nhau và KHÔNG vẽ
            // lại thẻ — giao diện đứng yên dù dữ liệu đã đổi.
            // Và cả ảnh: quản lý đổi ảnh bàn thì thẻ phải vẽ lại.
            return oldItem.getTenBan().equals(newItem.getTenBan()) &&
                   oldItem.getTinhTrang().equals(newItem.getTinhTrang()) &&
                   oldItem.getHoatDong().equals(newItem.getHoatDong()) &&
                   java.util.Objects.equals(oldItem.getAnhNho(), newItem.getAnhNho());
        }
    }
}
