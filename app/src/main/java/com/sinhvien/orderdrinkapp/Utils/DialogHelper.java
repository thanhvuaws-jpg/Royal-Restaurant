package com.sinhvien.orderdrinkapp.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.sinhvien.orderdrinkapp.R;

/**
 * DialogHelper — Lớp tiện ích tạo các hộp thoại (Dialog) chuẩn dùng chung toàn app.
 *
 * Nhiệm vụ: Tránh việc phải viết lại code tạo Dialog Loading ở mọi Activity/Fragment.
 * Tất cả nơi cần hiện "Đang xử lý..." chỉ cần gọi một dòng lệnh duy nhất là xong.
 */
public class DialogHelper {

    /**
     * Tạo và trả về một hộp thoại "Loading" (vòng xoay chờ đợi).
     * Dialog này KHÔNG thể tắt bằng cách bấm ngoài (setCancelable = false),
     * đảm bảo người dùng không thể thao tác gì cho đến khi tác vụ hoàn thành.
     *
     * @param context Context của Activity/Fragment đang gọi hàm này.
     * @param message Nội dung thông báo muốn hiển thị (VD: "Đang đăng nhập...").
     *                Nếu null hoặc rỗng, sẽ dùng văn bản mặc định "Vui lòng chờ...".
     * @return Đối tượng AlertDialog đã được cấu hình sẵn. Cần gọi thêm .show() để hiện lên.
     */
    public static AlertDialog getLoadingDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // Thổi phồng (inflate) layout custom từ file dialog_loading.xml
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);
        TextView txtMessage = dialogView.findViewById(R.id.txt_loading_message);
        if (message != null && !message.isEmpty()) {
            txtMessage.setText(message);
        } else {
            txtMessage.setText("Vui lòng chờ...");
        }
        builder.setView(dialogView);
        builder.setCancelable(false); // Khóa, không cho bấm ra ngoài để tắt dialog
        return builder.create();
    }
}
