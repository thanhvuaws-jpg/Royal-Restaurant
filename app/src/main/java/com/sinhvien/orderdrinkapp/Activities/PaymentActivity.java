package com.sinhvien.orderdrinkapp.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayPayment;
import com.sinhvien.orderdrinkapp.DTO.ThanhToanDTO;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderDetailResponse;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.ReceiptHelper;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PaymentActivity - Màn hình thanh toán hóa đơn tạm tính của Bàn ăn (Dành cho nhân viên phục vụ / Khách hàng).
 * Chức năng chính:
 * - Hiển thị danh sách món ăn, số lượng và tổng số tiền của đơn hàng thuộc bàn ăn được chọn.
 * - Cung cấp 2 hình thức thanh toán chính:
 *   + Tiền mặt: Gửi yêu cầu thanh toán trực tiếp lên máy thu ngân.
 *   + Chuyển khoản (VietQR): Tạo mã VietQR động theo tiêu chuẩn VietQR.io (chứa số tiền, nội dung thanh toán và thông tin ngân hàng thụ hưởng cấu hình tại ApiClient), hiển thị hình ảnh QR bằng Glide.
 * - Đồng bộ thời gian thực bằng Socket.io (kênh refresh_orders): Khi phát sinh thanh toán, phát tín hiệu lên server để thông báo cho máy thu ngân. Đồng thời lắng nghe phản hồi của Thu ngân duyệt đơn để kết thúc quá trình.
 * - Cơ chế Polling dự phòng (3s/5s): Chủ động gọi API checkOrderStatus liên tục để kiểm tra trạng thái thanh toán đã được thu ngân duyệt thành công chưa.
 * - Xuất và chia sẻ hóa đơn (HienThiHoaDon): Tạo ảnh chụp bitmap biên lai hóa đơn để chia sẻ hoặc lưu trữ dưới dạng ảnh.
 */
public class PaymentActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "PaymentActivity";

    // Khai báo các đối tượng giao diện XML
    ImageView img_payment_BackBtn;
    TextView txt_payment_TableName, txt_payment_OrderDate, txt_payment_TotalAmount;
    RecyclerView rv_payment_DishList;
    Button btn_payment_Pay;
    List<ThanhToanDTO> thanhToanDTOList;
    AdapterDisplayPayment adapterDisplayPayment;
    long tongtien = 0;
    int maban, madondat;
    String tenban, ngaydat;

    // ══════════════════ Mã giảm giá ══════════════════
    private android.widget.EditText edt_ma_giam_gia;
    private com.google.android.material.button.MaterialButton btn_ap_ma;
    private TextView txt_ket_qua_ma;

    /** Mã đã được máy chủ xác nhận hợp lệ. null = chưa áp mã nào. */
    private String maDaAp = null;

    /** Số tiền mã đó giảm trên hóa đơn hiện tại. */
    private long tienGiam = 0;

    // Các biến phục vụ việc Polling & Socket đồng bộ
    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private androidx.appcompat.app.AlertDialog waitingDialog; // Dialog hiển thị chờ thu ngân duyệt
    private boolean isPolling = false;
    private boolean isReceiptShowing = false;
    /** Phương thức khách đã chọn khi gửi thanh toán — in lên hóa đơn (H18). */
    private String phuongThucDaChon;
    private boolean shouldShowReceipt = false;
    private android.os.Parcelable savedLayoutState;

    private io.socket.client.Socket mSocket;
    private io.socket.emitter.Emitter.Listener onRefreshOrders = new io.socket.emitter.Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    checkApprovalStatus(); // Kiểm tra trạng thái hóa đơn khi nhận được tin nhắn socket
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_layout);

        // Ánh xạ các View XML
        img_payment_BackBtn    = findViewById(R.id.img_payment_BackBtn);
        txt_payment_TableName  = findViewById(R.id.txt_payment_TableName);
        txt_payment_OrderDate  = findViewById(R.id.txt_payment_OrderDate);
        txt_payment_TotalAmount = findViewById(R.id.txt_payment_TotalAmount);
        rv_payment_DishList    = findViewById(R.id.rv_payment_DishList);
        rv_payment_DishList.setLayoutManager(new LinearLayoutManager(this));
        btn_payment_Pay        = findViewById(R.id.btn_payment_Pay);

        edt_ma_giam_gia = findViewById(R.id.edt_ma_giam_gia);
        btn_ap_ma       = findViewById(R.id.btn_ap_ma);
        txt_ket_qua_ma  = findViewById(R.id.txt_ket_qua_ma);
        ganSuKienMaGiamGia();

        // Khôi phục trạng thái cũ (nếu có) khi quay màn hình
        boolean wasPolling = false;
        if (savedInstanceState != null) {
            savedLayoutState = savedInstanceState.getParcelable("list_state");
            wasPolling = savedInstanceState.getBoolean("is_polling", false);
            shouldShowReceipt = savedInstanceState.getBoolean("is_receipt_showing", false);
            phuongThucDaChon = savedInstanceState.getString("phuong_thuc_da_chon");
        }

        // Nhận dữ liệu truyền sang từ màn hình chính
        maban   = getIntent().getIntExtra("maban", 0);
        tenban  = getIntent().getStringExtra("tenban");
        ngaydat = getIntent().getStringExtra("ngaydat");
        madondat = getIntent().getIntExtra("madondat", 0);

        txt_payment_TableName.setText(tenban);
        txt_payment_OrderDate.setText(ngaydat);

        // Hiển thị danh sách các món ăn cần thanh toán
        HienThiDSMonThanhToan();

        if (wasPolling) {
            startPollingForApproval();
        }

        img_payment_BackBtn.setOnClickListener(this);
        btn_payment_Pay.setOnClickListener(this);
    }

    /**
     * Tải thông tin chi tiết các món ăn thuộc đơn hàng từ Server API.
     */
    private void HienThiDSMonThanhToan() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getOrderDetails(madondat).enqueue(new Callback<List<OrderDetailResponse>>() {
            @Override
            public void onResponse(Call<List<OrderDetailResponse>> call, Response<List<OrderDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    thanhToanDTOList = new ArrayList<>();
                    tongtien = 0;
                    for (OrderDetailResponse res : response.body()) {
                        ThanhToanDTO tt = new ThanhToanDTO();
                        tt.setTenMon(res.getTenMon());
                        tt.setGiaTien((int) res.getGiaTien());
                        tt.setSoLuong(res.getSoLuong());
                        tt.setHinhAnhPath(res.getHinhAnh());
                        thanhToanDTOList.add(tt);
                        tongtien += ((long) res.getSoLuong() * res.getGiaTien());
                    }
                    capNhatGiaoDien();
                }
            }

            @Override
            public void onFailure(Call<List<OrderDetailResponse>> call, Throwable t) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(PaymentActivity.this, "Lỗi lấy chi tiết đơn: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* ═══════════════════════ MÃ GIẢM GIÁ ═══════════════════════ */

    private void ganSuKienMaGiamGia() {
        btn_ap_ma.setOnClickListener(v -> {
            if (maDaAp != null) {
                goMa();
            } else {
                kiemTraMa(edt_ma_giam_gia.getText().toString().trim().toUpperCase());
            }
        });

        // Cho phép bấm Enter trên bàn phím thay vì phải với tay sang nút.
        edt_ma_giam_gia.setOnEditorActionListener((v, actionId, event) -> {
            if (maDaAp == null) {
                kiemTraMa(edt_ma_giam_gia.getText().toString().trim().toUpperCase());
            }
            return true;
        });
    }

    /**
     * Hỏi máy chủ xem mã có dùng được không, TRƯỚC khi bấm thanh toán.
     *
     * Vì sao cần bước xem trước: nhân viên gõ mã khách đọc qua, rất dễ sai
     * một ký tự. Thấy ngay "giảm 30.000đ, còn lại 120.000đ" thì sai sót
     * được phát hiện lúc còn sửa được, thay vì lúc đơn đã gửi đi.
     *
     * Bước này KHÔNG đánh dấu mã đã dùng — việc đó xảy ra ở checkout.
     */
    private void kiemTraMa(String ma) {
        if (ma.isEmpty()) {
            hienKetQuaMa(getString(R.string.tt_chua_nhap_ma), false);
            return;
        }

        btn_ap_ma.setEnabled(false);
        hienKetQuaMa(getString(R.string.tt_dang_kiem_tra), true);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.kiemTraMa(ma, madondat).enqueue(
                new Callback<com.sinhvien.orderdrinkapp.Api.KiemTraMaResponse>() {
            @Override
            public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.KiemTraMaResponse> call,
                                   Response<com.sinhvien.orderdrinkapp.Api.KiemTraMaResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                btn_ap_ma.setEnabled(true);

                boolean ok = response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus());

                if (!ok) {
                    hienKetQuaMa(docLoiMa(response), false);
                    return;
                }

                com.sinhvien.orderdrinkapp.Api.KiemTraMaResponse r = response.body();
                maDaAp   = r.getMaCode();
                tienGiam = r.getTienGiam() != null ? r.getTienGiam() : 0;

                // Chỉ nói số tiền giảm: tên mã thường đã là "Giảm 10.000đ", ghép
                // vào ra "✓ Giảm 10.000đ · giảm 10.000 đ" (H17).
                hienKetQuaMa(getString(R.string.tt_ma_hop_le,
                        com.sinhvien.orderdrinkapp.Utils.TienTe.so(tienGiam)), true);

                // Khóa ô nhập lại: mã đã chốt, sửa tiếp chỉ gây nhầm lẫn
                // giữa cái đang gõ và cái thực sự sẽ được áp.
                edt_ma_giam_gia.setText(maDaAp);
                edt_ma_giam_gia.setEnabled(false);
                btn_ap_ma.setText(R.string.tt_go_ma);

                capNhatGiaoDien();
            }

            @Override
            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.KiemTraMaResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                btn_ap_ma.setEnabled(true);
                hienKetQuaMa("Lỗi kết nối: " + t.getMessage(), false);
            }
        });
    }

    private void goMa() {
        maDaAp   = null;
        tienGiam = 0;
        edt_ma_giam_gia.setText("");
        edt_ma_giam_gia.setEnabled(true);
        btn_ap_ma.setText(R.string.tt_ap_ma);
        txt_ket_qua_ma.setVisibility(View.GONE);
        capNhatGiaoDien();
    }

    private void hienKetQuaMa(String noiDung, boolean tot) {
        txt_ket_qua_ma.setText(noiDung);
        txt_ket_qua_ma.setTextColor(android.graphics.Color.parseColor(tot ? "#2E7D32" : "#E53935"));
        txt_ket_qua_ma.setVisibility(View.VISIBLE);
    }

    /** Máy chủ trả 409 kèm lý do cụ thể, và lý do đó nằm ở errorBody. */
    private String docLoiMa(Response<?> response) {
        if (response.errorBody() != null) {
            try {
                org.json.JSONObject o = new org.json.JSONObject(response.errorBody().string());
                String m = o.optString("message", "");
                if (!m.isEmpty()) return m;
            } catch (Exception ignored) { }
        }
        return "Mã không dùng được";
    }

    /**
     * Đồng bộ nạp danh sách món ăn lên RecyclerView và tính tổng số tiền.
     */
    private void capNhatGiaoDien() {
        adapterDisplayPayment = new AdapterDisplayPayment(this, thanhToanDTOList);
        rv_payment_DishList.setAdapter(adapterDisplayPayment);

        // Hiện tổng SAU KHI TRỪ giảm giá, kèm chú thích số đã giảm.
        //
        // Hiện số gốc rồi để khách tự trừ nhẩm là cách nhanh nhất để sinh
        // tranh cãi ở quầy. Con số to nhất trên màn hình phải đúng bằng số
        // tiền khách sắp đưa.
        if (tienGiam > 0) {
            // Hai dòng: số phải trả to, phần "đã giảm" nhỏ bên dưới. Gộp một
            // dòng 22sp như trước thì bị cắt mất phần sau (H17).
            String phaiTra = com.sinhvien.orderdrinkapp.Utils.TienTe.dong(Math.max(0, tongtien - tienGiam));
            String daGiam = "(đã giảm " + com.sinhvien.orderdrinkapp.Utils.TienTe.dong(tienGiam) + ")";
            android.text.SpannableString hienThi = new android.text.SpannableString(phaiTra + "\n" + daGiam);
            hienThi.setSpan(new android.text.style.RelativeSizeSpan(0.6f), phaiTra.length() + 1,
                    hienThi.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            hienThi.setSpan(new android.text.style.ForegroundColorSpan(
                            androidx.core.content.ContextCompat.getColor(this, R.color.status_available)),
                    phaiTra.length() + 1, hienThi.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            txt_payment_TotalAmount.setSingleLine(false);
            txt_payment_TotalAmount.setMaxLines(2);
            txt_payment_TotalAmount.setText(hienThi);
        } else {
            txt_payment_TotalAmount.setSingleLine(true);
            txt_payment_TotalAmount.setText(com.sinhvien.orderdrinkapp.Utils.TienTe.dong(tongtien));
        }

        if (savedLayoutState != null && rv_payment_DishList.getLayoutManager() != null) {
            rv_payment_DishList.getLayoutManager().onRestoreInstanceState(savedLayoutState);
            savedLayoutState = null;
        }

        if (shouldShowReceipt) {
            shouldShowReceipt = false;
            HienThiHoaDon();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_payment_Pay) {
            if (ViewUtils.isFastDoubleClick()) return; // Khóa double click
            hienThiDialogChonPhuongThuc();
        } else if (id == R.id.img_payment_BackBtn) {
            finish();
        }
    }

    /**
     * Mở Dialog lựa chọn phương thức thanh toán.
     */
    private void hienThiDialogChonPhuongThuc() {
        String[] options = {"Tiền mặt", "Chuyển khoản (VietQR)"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn phương thức thanh toán")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        thucHienThanhToan("Tiền mặt");
                    } else {
                        hienThiDialogVietQR();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Mở Dialog chứa mã QR chuyển khoản động (sử dụng VietQR API).
     */
    private void hienThiDialogVietQR() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_vietqr, null);
        ImageView imgQR = dialogView.findViewById(R.id.img_dialogqr_QR);
        TextView txtBank = dialogView.findViewById(R.id.txt_dialogqr_Bank);
        TextView txtAccount = dialogView.findViewById(R.id.txt_dialogqr_Account);
        TextView txtAmount = dialogView.findViewById(R.id.txt_dialogqr_Amount);
        TextView txtMessage = dialogView.findViewById(R.id.txt_dialogqr_Message);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialogqr_Confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialogqr_Cancel);

        // Nội dung chuyển khoản: KHÔNG DẤU, chữ hoa, ngắn. Bản cũ là
        // "Thanh toan Ban " + tên bàn — tên bàn đã có chữ "bàn" nên ra
        // "Ban bàn 1", lại còn dấu tiếng Việt mà nhiều ngân hàng cắt hoặc từ
        // chối (H19). Mã đơn đứng đầu để thu ngân dò sao kê cho nhanh.
        String message = noiDungChuyenKhoan();
        // Số tiền trên mã QR là số SAU khi trừ mã giảm giá. Bản cũ dùng tổng
        // các món — khách có mã vẫn quét ra số tiền đầy đủ và chuyển dư.
        long canTra = Math.max(0, tongtien - tienGiam);
        String qrUrl = "";
        try {
            String encodedMsg = java.net.URLEncoder.encode(message, "UTF-8");
            String encodedName = java.net.URLEncoder.encode(getString(R.string.vietqr_bank_name), "UTF-8");
            qrUrl = "https://img.vietqr.io/image/" + getString(R.string.vietqr_bank_id) + "-" + getString(R.string.vietqr_bank_acc) + "-compact2.png"
                    + "?amount=" + canTra
                    + "&addInfo=" + encodedMsg
                    + "&accountName=" + encodedName;
        } catch (Exception e) {
            qrUrl = "https://img.vietqr.io/image/" + getString(R.string.vietqr_bank_id) + "-" + getString(R.string.vietqr_bank_acc) + "-compact2.png"
                    + "?amount=" + canTra
                    + "&addInfo=" + message;
        }

        txtBank.setText(getString(R.string.vietqr_bank_id));
        txtAccount.setText(getString(R.string.vietqr_bank_acc));
        txtAmount.setText(com.sinhvien.orderdrinkapp.Utils.TienTe.dong(canTra));
        txtMessage.setText(message);

        // Nạp ảnh QR trực tuyến bằng Glide
        com.bumptech.glide.Glide.with(this)
                .load(qrUrl)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(imgQR);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnConfirm.setOnClickListener(v -> {
            com.bumptech.glide.Glide.with(this).clear(imgQR);
            dialog.dismiss();
            thucHienThanhToan("Chuyển khoản"); // Gửi yêu cầu chuyển khoản lên thu ngân duyệt
        });

        btnCancel.setOnClickListener(v -> {
            com.bumptech.glide.Glide.with(this).clear(imgQR);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Gọi API yêu cầu thanh toán (checkoutOrder) gửi lên phía thu ngân phê duyệt.
     */
    private void thucHienThanhToan(String phuongthuc) {
        phuongThucDaChon = phuongthuc;
        androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(this, "Đang gửi yêu cầu thanh toán...");
        progressDialog.show();

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        // maDaAp = null khi khong ap ma — Retrofit bo qua @Field null nen
        // may chu nhan dung mot yeu cau khong co truong macode.
        //
        // May chu kiem tra lai ma va TU TINH so tien giam; con so tienGiam
        // ben nay chi de hien thi, khong phai de tin.
        apiService.checkoutOrder(madondat, tongtien, phuongthuc, maDaAp)
                .enqueue(new Callback<com.sinhvien.orderdrinkapp.Api.CheckoutResponse>() {
            @Override
            public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.CheckoutResponse> call,
                                   Response<com.sinhvien.orderdrinkapp.Api.CheckoutResponse> response) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    Log.d(TAG, "Gửi yêu cầu thanh toán thành công: madon=" + madondat + ", phuongthuc=" + phuongthuc);
                    
                    // Phát sự kiện Socket real-time thông báo Thu Ngân
                    io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                    if (socket != null && socket.connected()) {
                        socket.emit("refresh_orders");
                    }
                    startPollingForApproval(); // Chuyển sang chế độ chờ thu ngân duyệt đơn
                } else {
                    Toast.makeText(PaymentActivity.this, "Lỗi gửi yêu cầu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.CheckoutResponse> call, Throwable t) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                Log.e(TAG, "Lỗi gửi yêu cầu thanh toán: " + t.getMessage());
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(PaymentActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Bật cơ chế lắng nghe thu ngân xác nhận duyệt đơn.
     * Sử dụng kết hợp Socket.io và cơ chế Polling (gọi lại định kỳ) để đảm bảo không bị mất gói tin.
     */
    private void startPollingForApproval() {
        // ------------------------------------------------------------------
        // Hộp thoại chờ CÓ ĐƯỜNG THOÁT
        // ------------------------------------------------------------------
        // Trước đây dùng DialogHelper.getLoadingDialog(), vốn đặt
        // setCancelable(false). Hệ quả: nếu thu ngân không online (nghỉ ca,
        // chưa đăng nhập, mất mạng), máy của nhân viên phục vụ bị khóa cứng
        // trong hộp thoại này vô thời hạn — không thoát được, không phục vụ
        // bàn khác được. Đây là nửa nghiệp vụ của lỗi kẹt đơn: nửa kia nằm
        // ở giao diện thu ngân (xem QĐ-013).
        //
        // Điểm cần hiểu đúng: yêu cầu thanh toán ĐÃ được ghi vào cơ sở dữ
        // liệu ở trạng thái 'pending' trước khi hộp thoại này hiện ra. Đóng
        // hộp thoại KHÔNG hủy đơn — thu ngân vẫn thấy và duyệt được bất cứ
        // lúc nào. Vì vậy việc giam nhân viên ở đây không mang lại lợi ích
        // nào cả.
        //
        // Không sửa DialogHelper vì nó còn dùng cho các thao tác chờ ngắn
        // khác, nơi việc khóa thao tác là hợp lý.
        View viewCho = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        TextView txtCho = viewCho.findViewById(R.id.txt_loading_message);
        if (txtCho != null) {
            txtCho.setText("Đang chờ Thu ngân xác nhận...");
        }

        waitingDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(viewCho)
                .setCancelable(true)  // cho phép bấm nút Back để thoát
                .setNegativeButton("Để thu ngân duyệt sau", (d, which) -> {
                    stopPolling();
                    Toast.makeText(PaymentActivity.this,
                            "Đơn đã gửi sang thu ngân. Bàn sẽ được giải phóng sau khi duyệt.",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .create();

        // Bấm Back cũng phải dừng polling, nếu không Runnable vẫn chạy nền
        // và tiếp tục gọi API sau khi màn hình đã đóng.
        waitingDialog.setOnCancelListener(d -> {
            stopPolling();
            finish();
        });

        waitingDialog.show();

        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        if (mSocket != null && mSocket.connected()) {
            mSocket.on("refresh_orders", onRefreshOrders);
            // Polling dự phòng chạy mỗi 5s
            isPolling = true;
            pollingRunnable = new Runnable() {
                @Override
                public void run() {
                    checkApprovalStatus();
                    if (isPolling) {
                        pollingHandler.postDelayed(this, 5000);
                    }
                }
            };
            pollingHandler.postDelayed(pollingRunnable, 5000);
        } else {
            // Không có kết nối Socket -> Chạy Polling định kỳ mỗi 3s
            isPolling = true;
            pollingRunnable = new Runnable() {
                @Override
                public void run() {
                    checkApprovalStatus();
                    if (isPolling) {
                        pollingHandler.postDelayed(this, 3000);
                    }
                }
            };
            pollingHandler.post(pollingRunnable);
        }
    }

    /**
     * Dừng lắng nghe duyệt đơn và đóng các hộp thoại chờ.
     */
    private void stopPolling() {
        isPolling = false;
        if (pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
        if (mSocket != null) {
            mSocket.off("refresh_orders", onRefreshOrders);
        }
        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
        }
    }

    /**
     * Gọi API checkOrderStatus để kiểm tra xem đơn hàng đã được Thu ngân chuyển trạng thái sang đã thanh toán (tinhTrang = true) chưa.
     */
    private void checkApprovalStatus() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.checkOrderStatus(madondat).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().getTinhTrang();
                    if ("true".equals(status)) { // Đã được thu ngân xác nhận duyệt
                        stopPolling();
                        HienThiHoaDon(); // Xuất hóa đơn cho khách
                    }
                }
            }
            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                // Bỏ qua lỗi trong lúc polling chờ
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu giữ trạng thái màn hình
        outState.putBoolean("is_polling", isPolling);
        outState.putBoolean("is_receipt_showing", isReceiptShowing);
        outState.putString("phuong_thuc_da_chon", phuongThucDaChon);
        if (rv_payment_DishList != null && rv_payment_DishList.getLayoutManager() != null) {
            outState.putParcelable("list_state", rv_payment_DishList.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }

    /**
     * Hiển thị biên lai Hóa đơn (Receipt Layout) và hỗ trợ chia sẻ thông qua ReceiptHelper.
     */
    /** "ROYAL DON 17976 BAN VIP 7" — tối đa 50 ký tự, chỉ chữ số và chữ không dấu. */
    private String noiDungChuyenKhoan() {
        String ban = java.text.Normalizer.normalize(tenban == null ? "" : tenban, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(java.util.Locale.ROOT);
        String kq = ("ROYAL DON " + madondat + " " + ban).trim();
        return kq.length() > 50 ? kq.substring(0, 50).trim() : kq;
    }

    /** "bàn 1" -> "Bàn 1": tên bàn đã nói rõ là bàn, không cần nhãn "Bàn:" đứng trước. */
    private static String vietHoaChuDau(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(new java.util.Locale("vi")) + s.substring(1);
    }

    /** "2026-09-24 13:05:00" -> "13:05 · 24/09/2026"; chuỗi lạ thì trả nguyên. */
    private static String ngayGioDep(String raw) {
        try {
            java.util.Date d = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).parse(raw);
            return new java.text.SimpleDateFormat("HH:mm · dd/MM/yyyy", java.util.Locale.US).format(d);
        } catch (Exception e) {
            return raw == null ? "" : raw;
        }
    }

    private void HienThiHoaDon() {
        isReceiptShowing = true;
        View receiptView = LayoutInflater.from(this)
                .inflate(R.layout.receipt_layout, null);

        // Ánh xạ các View con trong hóa đơn
        TextView txt_receipt_TableName = receiptView.findViewById(R.id.txt_receipt_TableName);
        TextView txt_receipt_Date      = receiptView.findViewById(R.id.txt_receipt_Date);
        TextView txt_receipt_Total     = receiptView.findViewById(R.id.txt_receipt_Total);
        LinearLayout layout_receipt_ItemList = receiptView.findViewById(R.id.layout_receipt_ItemList);
        Button btn_receipt_Share       = receiptView.findViewById(R.id.btn_receipt_Share);
        Button btn_receipt_Close       = receiptView.findViewById(R.id.btn_receipt_Close);

        // Gán thông tin hóa đơn
        // Trước đây: getString("Bàn: ") + tên — Android cắt khoảng trắng cuối
        // của chuỗi tài nguyên nên ra "Bàn:bàn 1"; "Ngày:" cũng dính liền (H18).
        txt_receipt_TableName.setText(vietHoaChuDau(tenban));
        txt_receipt_Date.setText(ngayGioDep(ngaydat));
        // tongtien là tổng các món; số khách trả là sau khi trừ mã giảm giá.
        long canTra = Math.max(0, tongtien - tienGiam);
        txt_receipt_Total.setText(com.sinhvien.orderdrinkapp.Utils.TienTe.dong(canTra));
        TextView txtGiam = receiptView.findViewById(R.id.txt_receipt_Discount);
        if (tienGiam > 0) {
            txtGiam.setText("Tạm tính " + com.sinhvien.orderdrinkapp.Utils.TienTe.dong(tongtien)
                    + " · Giảm" + (maDaAp != null ? " (" + maDaAp + ")" : "")
                    + " −" + com.sinhvien.orderdrinkapp.Utils.TienTe.dong(tienGiam));
            txtGiam.setVisibility(View.VISIBLE);
        }
        TextView txtPhuongThuc = receiptView.findViewById(R.id.txt_receipt_Method);
        if (phuongThucDaChon != null && !phuongThucDaChon.isEmpty()) {
            txtPhuongThuc.setText("Thanh toán: " + phuongThucDaChon);
            txtPhuongThuc.setVisibility(View.VISIBLE);
        }

        // Nạp động danh sách món ăn vào Layout hóa đơn
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ThanhToanDTO item : thanhToanDTOList) {
            View rowView = inflater.inflate(
                    R.layout.custom_layout_receipt_item, layout_receipt_ItemList, false);

            ((TextView) rowView.findViewById(R.id.txt_receiptItem_Name))
                    .setText(item.getTenMon());
            ((TextView) rowView.findViewById(R.id.txt_receiptItem_Quantity))
                    .setText("x" + item.getSoLuong());
            long subtotal = (long) item.getSoLuong() * item.getGiaTien();
            ((TextView) rowView.findViewById(R.id.txt_receiptItem_Subtotal))
                    .setText(com.sinhvien.orderdrinkapp.Utils.TienTe.dong(subtotal));

            layout_receipt_ItemList.addView(rowView);
        }

        // Tạo và mở hộp thoại AlertDialog hiển thị biên lai
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(receiptView)
                .setCancelable(false)
                .create();

        // Nút chia sẻ hóa đơn (Chụp màn hình layout và chia sẻ)
        btn_receipt_Share.setOnClickListener(v -> {
            // Tạm thời ẩn các nút bấm để bức ảnh chụp biên lai sạch đẹp hơn
            btn_receipt_Share.setVisibility(View.GONE);
            btn_receipt_Close.setVisibility(View.GONE);

            View contentOnly = receiptView.findViewById(R.id.layout_receipt_Content);
            contentOnly.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            getResources().getDisplayMetrics().widthPixels,
                            View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED));
            contentOnly.layout(0, 0,
                    contentOnly.getMeasuredWidth(),
                    contentOnly.getMeasuredHeight());

            // Chụp view thành ảnh Bitmap và gọi Intent chia sẻ hệ thống
            Bitmap bitmap = ReceiptHelper.captureView(contentOnly);
            ReceiptHelper.shareBitmap(this, bitmap);

            // Hiện lại các nút sau khi chụp xong
            btn_receipt_Share.setVisibility(View.VISIBLE);
            btn_receipt_Close.setVisibility(View.VISIBLE);
        });

        // Nút đóng hộp thoại -> hoàn thành và đóng Activity thanh toán
        btn_receipt_Close.setOnClickListener(v -> {
            isReceiptShowing = false;
            dialog.dismiss();
            Toast.makeText(this,
                    getString(R.string.payment_success_msg),
                    Toast.LENGTH_SHORT).show();
            finish();
        });

        dialog.show();
    }
}