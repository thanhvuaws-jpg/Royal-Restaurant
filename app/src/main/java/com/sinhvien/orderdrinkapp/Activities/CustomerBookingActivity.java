package com.sinhvien.orderdrinkapp.Activities;

import android.app.DatePickerDialog;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.BanTrongResponse;
import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.Api.TableResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.PreorderDishesAdapter;
import com.sinhvien.orderdrinkapp.DTO.MonDTO;
import com.sinhvien.orderdrinkapp.Database.LocalDatabaseHelper;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.Utils.ViewUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CustomerBookingActivity - Màn hình dành cho Khách hàng (Customer) thực hiện Đặt bàn và Gọi món ăn trước.
 * Chức năng chính:
 * - Khách hàng lựa chọn bàn trống từ Spinner (dữ liệu đồng bộ giữa SQLite cache và Server cloud).
 * - Chọn thời gian hẹn trước thông qua DatePickerDialog & TimePickerDialog trực quan (kiểm duyệt giờ hẹn phải trong tương lai).
 * - Xem danh sách món ăn khả dụng, chọn số lượng tương ứng để đặt trước, cập nhật hiển thị tổng tiền tự động.
 * - Gửi yêu cầu đặt bàn và đặt món lên server thông qua API createBooking (mã món và số lượng được mã hóa JSON).
 * - Tích hợp Socket.io: Phát tín hiệu real-time booking_status_updated và nhận phản hồi menu_changed để tải lại món ăn.
 */
public class CustomerBookingActivity extends AppCompatActivity {

    private static final String TAG = "CustomerBookingActivity";

    // Khai báo View thành phần UI
    Spinner spinner_tables;
    // Ảnh xem trước của bàn đang chọn trong spinner
    View cardBanXemTruoc;
    android.widget.ImageView imgBanXemTruoc;
    Button btn_select_date, btn_select_time, btn_confirm_booking;
    TextView txt_selected_datetime, txt_total_preorder;
    // Trạng thái danh sách bàn: "chọn giờ trước" / "còn N bàn trống" / "đã kín"
    TextView txtTrangThaiBan;
    ProgressBar progressBanTrong;
    RecyclerView rv_booking_dishes;

    // Bàn CÒN TRỐNG vào giờ đã chọn (ban_trong.php) & adapter của Spinner.
    // Rỗng khi chưa chọn đủ ngày giờ.
    List<TableResponse> tableList = new ArrayList<>();
    List<String> tableNames = new ArrayList<>();
    ArrayAdapter<String> tableAdapter;

    // Cơ sở dữ liệu SQLite & Adapter danh sách món ăn
    LocalDatabaseHelper dbHelper;
    PreorderDishesAdapter dishesAdapter;
    List<MonDTO> dishList = new ArrayList<>();

    // Các biến lưu trữ ngày giờ hẹn được chọn
    int selectedYear = -1, selectedMonth = -1, selectedDay = -1;
    int selectedHour = -1, selectedMinute = -1;

    long totalPreorderPrice = 0; // Tổng tiền tạm tính của các món đặt trước

    private io.socket.client.Socket mSocket;
    private io.socket.emitter.Emitter.Listener onMenuChanged;
    // Khách khác vừa đặt/hủy: nạp lại bàn trống để bàn vừa bị đặt biến mất ngay.
    private io.socket.emitter.Emitter.Listener onBookingChanged;

    /** Lời gọi ban_trong.php đang chạy — hủy khi khách đổi giờ, để kết quả cũ không đè kết quả mới. */
    private Call<BanTrongResponse> yeuCauBanTrong;

    private int savedTableId = -1;
    private Map<Integer, Integer> savedQuantities = new java.util.HashMap<>();

    // ══════════════════ Khối chọn món: lọc danh mục + tìm + phân trang ══════════════════

    /** Số món hiển thị mỗi trang. 6 vừa khít khung 400dp, không phải cuộn lồng nhau. */
    private static final int MON_MOI_TRANG = 6;

    /** Trễ (ms) trước khi gõ xong mới gọi máy chủ, tránh bắn một yêu cầu mỗi ký tự. */
    private static final long TRE_TIM_KIEM = 350;

    private EditText edt_tim_mon;
    private ImageButton btn_xoa_tim, btn_trang_truoc, btn_trang_sau;
    private LinearLayout layout_chip_danh_muc;
    private TextView txt_khong_co_mon, txt_tong_so_mon, txt_so_trang;
    private ProgressBar progress_mon;

    /**
     * Kho món đã từng tải về, khóa theo MAMON.
     *
     * VÌ SAO CẦN: trước khi phân trang, `dishList` chứa TOÀN BỘ 62 món nên
     * `updateTotalPriceDisplay()` tra giá bằng cách quét thẳng nó. Sau khi
     * phân trang, `dishList` chỉ còn 6 món của trang đang xem — khách chọn
     * 2 món ở trang 1 rồi sang trang 2 thì hai món đó biến mất khỏi tổng
     * tiền, dù vẫn nằm trong giỏ và vẫn được gửi lên khi đặt bàn.
     *
     * Kho này tích lũy qua mọi trang và mọi danh mục, không bao giờ xóa
     * trong một phiên, nên giá luôn tra được.
     */
    private final Map<Integer, MonDTO> khoMon = new HashMap<>();

    private final List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> danhSachLoai = new ArrayList<>();

    private int maLoaiDangChon = 0;   // 0 = Tất cả
    private int trangHienTai   = 1;
    private int tongSoTrang    = 1;
    private int tongSoMon      = 0;
    private String tuKhoa      = "";

    private final android.os.Handler handlerTimKiem =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable viecTimKiemDangCho;

    // ══════════════════ Mã giảm giá ══════════════════
    private LinearLayout khung_chon_voucher;
    private TextView txt_voucher_da_chon, txt_voucher_thao_tac;

    /** Mã khách đã chọn. null = không gắn mã nào. */
    private String maVoucherDaChon = null;

    /** Danh sách mã còn dùng được, nạp cùng lúc mở màn hình. */
    private final List<com.sinhvien.orderdrinkapp.Api.LoyaltyResponse.MaGiamGia> maKhaDung =
            new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Khôi phục lại trạng thái biểu mẫu nếu Activity bị xoay chiều
        if (savedInstanceState != null) {
            selectedYear = savedInstanceState.getInt("selectedYear", -1);
            selectedMonth = savedInstanceState.getInt("selectedMonth", -1);
            selectedDay = savedInstanceState.getInt("selectedDay", -1);
            selectedHour = savedInstanceState.getInt("selectedHour", -1);
            selectedMinute = savedInstanceState.getInt("selectedMinute", -1);
            savedTableId = savedInstanceState.getInt("selectedTableId", -1);
            String jsonQuantities = savedInstanceState.getString("selectedQuantities");
            if (jsonQuantities != null && !jsonQuantities.isEmpty()) {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<Integer, Integer>>(){}.getType();
                savedQuantities = new Gson().fromJson(jsonQuantities, type);
            }
        }
        setContentView(R.layout.activity_customer_booking);

        // Thiết lập Toolbar tiêu đề thanh tác vụ
        Toolbar toolbar = findViewById(R.id.booking_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Ánh xạ View
        spinner_tables = findViewById(R.id.spinner_tables);
        cardBanXemTruoc = findViewById(R.id.card_ban_xem_truoc);
        imgBanXemTruoc = findViewById(R.id.img_ban_xem_truoc);
        btn_select_date = findViewById(R.id.btn_select_date);
        btn_select_time = findViewById(R.id.btn_select_time);
        btn_confirm_booking = findViewById(R.id.btn_confirm_booking);
        txt_selected_datetime = findViewById(R.id.txt_selected_datetime);
        txt_total_preorder = findViewById(R.id.txt_total_preorder);
        rv_booking_dishes = findViewById(R.id.rv_booking_dishes);
        txtTrangThaiBan = findViewById(R.id.txt_trang_thai_ban);
        progressBanTrong = findViewById(R.id.progress_ban_trong);

        // Khối chọn món
        edt_tim_mon          = findViewById(R.id.edt_tim_mon);
        btn_xoa_tim          = findViewById(R.id.btn_xoa_tim);
        layout_chip_danh_muc = findViewById(R.id.layout_chip_danh_muc);
        txt_khong_co_mon     = findViewById(R.id.txt_khong_co_mon);
        progress_mon         = findViewById(R.id.progress_mon);
        txt_tong_so_mon      = findViewById(R.id.txt_tong_so_mon);
        txt_so_trang         = findViewById(R.id.txt_so_trang);
        btn_trang_truoc      = findViewById(R.id.btn_trang_truoc);
        btn_trang_sau        = findViewById(R.id.btn_trang_sau);

        khung_chon_voucher   = findViewById(R.id.khung_chon_voucher);
        txt_voucher_da_chon  = findViewById(R.id.txt_voucher_da_chon);
        txt_voucher_thao_tac = findViewById(R.id.txt_voucher_thao_tac);
        khung_chon_voucher.setOnClickListener(v -> {
            if (maVoucherDaChon != null) boChonVoucher(); else moDanhSachVoucher();
        });
        taiMaKhaDung();

        dbHelper = LocalDatabaseHelper.getInstance(this);

        // Khởi tạo Spinner danh sách bàn ăn
        tableAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tableNames);
        tableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_tables.setAdapter(tableAdapter);
        spinner_tables.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                hienAnhBanDangChon(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                hienAnhBanDangChon(-1);
            }
        });

        // Đăng ký picker và tải món ăn. Danh sách bàn CHƯA tải: bàn trống
        // hay không phụ thuộc giờ hẹn, nên chỉ tải sau khi khách chọn giờ.
        setupDateTimePickers();
        khoiTaoKhoiChonMon();
        taiDanhMuc();
        loadDishes();

        updateDateTimeDisplay();
        taiBanTrong();   // xoay màn hình khi đã chọn giờ: nạp lại ngay

        // Đăng ký sự kiện click chuột xác nhận đặt bàn
        btn_confirm_booking.setOnClickListener(v -> {
            if (ViewUtils.isFastDoubleClick()) return; // Khóa double click liên tục
            submitBooking();
        });

        // Kết nối Socket.io để lắng nghe sự thay đổi menu từ các máy khác real-time
        mSocket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
        onMenuChanged = args -> {
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    loadDishes(); // Cập nhật lại danh sách món ăn khi có thay đổi từ quản trị viên
                }
            });
        };
        onBookingChanged = args -> runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed() && daChonDuGio()) taiBanTrong();
        });
        if (mSocket != null) {
            mSocket.on("menu_changed", onMenuChanged);
            mSocket.on("booking_status_updated", onBookingChanged);
        }
    }

    private boolean daChonDuGio() {
        return selectedYear != -1 && selectedHour != -1;
    }

    private String gioHenDangChon() {
        return String.format(java.util.Locale.US, "%04d-%02d-%02d %02d:%02d:00",
                selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);
    }

    /**
     * Hỏi máy chủ những bàn còn trống vào giờ đã chọn (ban_trong.php).
     *
     * THIẾT KẾ LẠI 25/09/2026 (QĐ-095). Trước đây màn hình lấy get_tables và
     * lọc TINHTRANG = "false" — nghĩa là "lúc này không có ai ngồi", chẳng
     * liên quan gì tới giờ hẹn. Bàn đã có người đặt tối nay vẫn hiện để
     * chọn, còn bàn đang có khách ăn trưa lại bị ẩn khỏi lịch buổi tối.
     *
     * Không dùng bộ nhớ đệm SQLite ở đây: danh sách bàn trống đổi theo từng
     * lượt đặt của khách khác, bản cũ vài phút là đã sai.
     */
    private void taiBanTrong() {
        if (yeuCauBanTrong != null) yeuCauBanTrong.cancel();
        if (!daChonDuGio()) {
            capNhatDanhSachBan(new ArrayList<>(), "Chưa chọn giờ hẹn");
            txtTrangThaiBan.setText(R.string.booking_chon_gio_truoc);
            return;
        }

        progressBanTrong.setVisibility(View.VISIBLE);
        txtTrangThaiBan.setText("Đang tìm bàn trống...");
        spinner_tables.setEnabled(false);

        yeuCauBanTrong = ApiClient.getClient().create(ApiService.class).getBanTrong(gioHenDangChon());
        yeuCauBanTrong.enqueue(new Callback<BanTrongResponse>() {
            @Override
            public void onResponse(Call<BanTrongResponse> call, Response<BanTrongResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                progressBanTrong.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    // 400: giờ không hợp lệ (quá khứ, ngoài 08–22h) — máy chủ
                    // nói rõ lý do, hiện nguyên văn.
                    capNhatDanhSachBan(new ArrayList<>(), "Chọn giờ khác");
                    txtTrangThaiBan.setText(ViewUtils.docLoiMayChu(response, "Không tải được danh sách bàn trống."));
                    return;
                }
                BanTrongResponse kq = response.body();
                capNhatDanhSachBan(kq.getBan(), "Không còn bàn trống");
                int soTrong = kq.getBan().size();
                String gio = String.format(java.util.Locale.US, "%02d:%02d %02d/%02d",
                        selectedHour, selectedMinute, selectedDay, selectedMonth);
                if (soTrong == 0) {
                    txtTrangThaiBan.setText("Lúc " + gio + " đã kín bàn. Vui lòng chọn giờ khác.");
                } else {
                    txtTrangThaiBan.setText("Còn " + soTrong + " bàn trống lúc " + gio
                            + (kq.getSoBanKin() > 0 ? " (" + kq.getSoBanKin() + " bàn đã có khách đặt)" : "")
                            + ". Mỗi lượt đặt giữ bàn " + (kq.getThoiLuongPhut() / 60) + " tiếng.");
                }
            }

            @Override
            public void onFailure(Call<BanTrongResponse> call, Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                progressBanTrong.setVisibility(View.GONE);
                capNhatDanhSachBan(new ArrayList<>(), "Chưa tải được");
                txtTrangThaiBan.setText("Lỗi kết nối, chưa tải được bàn trống. Chọn lại giờ để thử lại.");
            }
        });
    }

    /**
     * Đổ danh sách bàn trống vào Spinner, giữ bàn khách đang chọn nếu nó
     * vẫn còn trống (đổi giờ từ 18:00 sang 18:30 thì không bắt chọn lại).
     *
     * @param chuKhiRong dòng hiện trong Spinner khi không có bàn nào — phải
     *                   nói đúng lý do: chưa chọn giờ, giờ không hợp lệ hay kín bàn.
     */
    private void capNhatDanhSachBan(List<TableResponse> ds, String chuKhiRong) {
        int banDangChon = savedTableId;
        int viTri = spinner_tables.getSelectedItemPosition();
        if (viTri >= 0 && viTri < tableList.size()) banDangChon = tableList.get(viTri).getMaBan();

        tableList.clear();
        tableNames.clear();
        tableList.addAll(ds);
        for (TableResponse b : ds) tableNames.add(b.getTenBan());
        if (tableList.isEmpty()) tableNames.add(chuKhiRong);
        tableAdapter.notifyDataSetChanged();
        spinner_tables.setEnabled(!tableList.isEmpty());

        int chon = 0;
        for (int i = 0; i < tableList.size(); i++) {
            if (tableList.get(i).getMaBan() == banDangChon) { chon = i; break; }
        }
        spinner_tables.setSelection(chon);
        if (!tableList.isEmpty()) savedTableId = -1;

        // Danh sách vừa nạp lại mà vị trí chọn không đổi thì spinner KHÔNG
        // gọi onItemSelected — ảnh xem trước sẽ đứng ở bàn cũ. Gọi thẳng.
        hienAnhBanDangChon(spinner_tables.getSelectedItemPosition());
    }

    /**
     * Ảnh bàn đang chọn. Dùng ảnh GỐC chứ không dùng bản 480px: khung xem
     * trước rộng gần hết màn hình, kéo bản nhỏ lên sẽ mờ. Chỉ một ảnh mỗi
     * lần nên tải ảnh gốc (~200 KB) vẫn nhẹ.
     */
    private void hienAnhBanDangChon(int viTri) {
        if (cardBanXemTruoc == null || imgBanXemTruoc == null) return;
        // Danh sách bàn nạp bất đồng bộ; màn đã đóng thì Glide ném lỗi.
        if (isFinishing() || isDestroyed()) return;
        String duongDan = null;
        if (viTri >= 0 && viTri < tableList.size()) {
            TableResponse ban = tableList.get(viTri);
            duongDan = ban.getUrlAnh() != null ? ban.getUrlAnh() : ban.getUrlAnhNho();
        }
        String url = com.sinhvien.orderdrinkapp.Utils.ViewUtils.getImageUrl(duongDan);
        if (url.isEmpty()) {
            com.bumptech.glide.Glide.with(this).clear(imgBanXemTruoc);
            cardBanXemTruoc.setVisibility(View.GONE);
            return;
        }
        cardBanXemTruoc.setVisibility(View.VISIBLE);
        com.bumptech.glide.Glide.with(this)
                .load(url)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .centerCrop()
                .into(imgBanXemTruoc);
    }

    /**
     * Khởi tạo và thiết lập các hộp thoại chọn Ngày (DatePickerDialog) và Giờ (TimePickerDialog).
     */
    private void setupDateTimePickers() {
        // Click để hiển thị hộp chọn Ngày. Không cho chọn ngày đã qua.
        btn_select_date.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int y = selectedYear != -1 ? selectedYear : c.get(Calendar.YEAR);
            int m = selectedMonth != -1 ? selectedMonth - 1 : c.get(Calendar.MONTH);
            int d = selectedDay != -1 ? selectedDay : c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedYear = year;
                selectedMonth = month + 1;
                selectedDay = dayOfMonth;
                updateDateTimeDisplay();
                taiBanTrong();
            }, y, m, d);
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        // Click để hiển thị hộp chọn Giờ. Mặc định giờ kế tiếp, kẹp trong 08–21h.
        btn_select_time.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int h = selectedHour != -1 ? selectedHour : Math.max(8, Math.min(21, c.get(Calendar.HOUR_OF_DAY) + 1));
            int mi = selectedMinute != -1 ? selectedMinute : 0;
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                selectedHour = hourOfDay;
                selectedMinute = minute;
                updateDateTimeDisplay();
                taiBanTrong();
            }, h, mi, true);
            timePickerDialog.show();
        });
    }

    /**
     * Cập nhật chuỗi hiển thị ngày giờ đã chọn lên giao diện người dùng.
     */
    private void updateDateTimeDisplay() {
        String display = "";
        if (selectedYear != -1) {
            display += String.format(java.util.Locale.US, "%02d/%02d/%04d", selectedDay, selectedMonth, selectedYear);
        }
        if (selectedHour != -1) {
            if (!display.isEmpty()) display += " lúc ";
            display += String.format(java.util.Locale.US, "%02d:%02d", selectedHour, selectedMinute);
        }
        if (display.isEmpty()) {
            txt_selected_datetime.setText(R.string.booking_no_datetime);
        } else {
            txt_selected_datetime.setText(daChonDuGio() ? "Thời gian hẹn: " + display : display);
        }
    }

    /**
     * Tính toán tổng giá trị của các món ăn đặt trước và hiển thị lên giao diện.
     */
    private void updateTotalPriceDisplay(Map<Integer, Integer> quantities) {
        totalPreorderPrice = 0;

        // Tra giá trong KHO MÓN chứ không trong dishList.
        //
        // dishList nay chỉ chứa món của TRANG đang xem (6 món). Nếu tra ở
        // đó thì món khách chọn ở trang khác sẽ không cộng vào tổng — số
        // tiền hiện trên màn hình thấp hơn số thực sự đặt.
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            MonDTO mon = khoMon.get(entry.getKey());
            if (mon == null) continue;
            try {
                totalPreorderPrice += (long) entry.getValue() * Long.parseLong(mon.getGiaTien());
            } catch (NumberFormatException ignored) {}
        }

        DecimalFormat formatter = com.sinhvien.orderdrinkapp.Utils.TienTe.dinhDang();
        txt_total_preorder.setText("Tổng: " + formatter.format(totalPreorderPrice) + " đ");
        capNhatThanhPhanTrang();
    }

    /** Gom món vào kho để tra giá được ở mọi trang. */
    private void ghiVaoKhoMon(List<MonDTO> ds) {
        for (MonDTO m : ds) khoMon.put(m.getMaMon(), m);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  KHỐI CHỌN MÓN: lọc theo danh mục, tìm kiếm, phân trang
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Gắn sự kiện cho ô tìm, hai nút chuyển trang, và tạo adapter MỘT LẦN.
     *
     * Adapter phải được tạo đúng một lần và giữ nguyên suốt màn hình: nó
     * là nơi cất `selectedQuantities` (MAMON -> số lượng). Tạo lại mỗi lần
     * đổi trang đồng nghĩa xóa sạch giỏ của khách.
     */
    private void khoiTaoKhoiChonMon() {
        dishesAdapter = new PreorderDishesAdapter(this, dishList,
                quantities -> updateTotalPriceDisplay(quantities));

        if (savedQuantities != null && !savedQuantities.isEmpty()) {
            dishesAdapter.getSelectedQuantities().putAll(savedQuantities);
            savedQuantities.clear();
        }

        rv_booking_dishes.setLayoutManager(new LinearLayoutManager(this));
        rv_booking_dishes.setAdapter(dishesAdapter);

        setupBestsellerForCustomer();

        btn_trang_truoc.setOnClickListener(v -> {
            if (trangHienTai > 1) { trangHienTai--; taiMon(); }
        });
        btn_trang_sau.setOnClickListener(v -> {
            if (trangHienTai < tongSoTrang) { trangHienTai++; taiMon(); }
        });

        btn_xoa_tim.setOnClickListener(v -> {
            edt_tim_mon.setText("");
            edt_tim_mon.clearFocus();
        });

        edt_tim_mon.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void afterTextChanged(android.text.Editable e) {
                String moi = e.toString().trim();
                btn_xoa_tim.setVisibility(moi.isEmpty() ? View.GONE : View.VISIBLE);
                if (moi.equals(tuKhoa)) return;

                // Hoãn lại: gõ "bánh canh" là 9 ký tự, không nên thành 9
                // lượt gọi máy chủ. Mỗi ký tự mới hủy lượt chờ trước đó.
                if (viecTimKiemDangCho != null) handlerTimKiem.removeCallbacks(viecTimKiemDangCho);
                viecTimKiemDangCho = () -> {
                    tuKhoa = moi;
                    trangHienTai = 1;   // từ khóa đổi thì số trang cũ vô nghĩa
                    taiMon();
                };
                handlerTimKiem.postDelayed(viecTimKiemDangCho, TRE_TIM_KIEM);
            }
        });
    }

    /** Lấy danh sách món bán chạy và nhúng vào màn đặt món của khách. */
    private void setupBestsellerForCustomer() {
        View layoutBestseller = findViewById(R.id.layout_booking_bestseller);
        RecyclerView rvBestseller = findViewById(R.id.rv_booking_bestseller);
        if (layoutBestseller == null || rvBestseller == null) return;

        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvBestseller.setLayoutManager(lm);
        rvBestseller.setHasFixedSize(true);

        com.sinhvien.orderdrinkapp.CustomAdapter.AdapterBestseller bestsellerAdapter =
                new com.sinhvien.orderdrinkapp.CustomAdapter.AdapterBestseller(this, item -> {
            int currentQty = 0;
            if (dishesAdapter.getSelectedQuantities().containsKey(item.getMaMon())) {
                Integer q = dishesAdapter.getSelectedQuantities().get(item.getMaMon());
                currentQty = (q != null) ? q : 0;
            }
            dishesAdapter.getSelectedQuantities().put(item.getMaMon(), currentQty + 1);

            // Đảm bảo món có trong khoMon để tính tổng tiền
            if (!khoMon.containsKey(item.getMaMon())) {
                MonDTO monDTO = new MonDTO();
                monDTO.setMaMon(item.getMaMon());
                monDTO.setTenMon(item.getTenMon());
                monDTO.setGiaTien(String.valueOf((long)item.getGiaTien()));
                monDTO.setHinhAnhUrl(item.getHinhAnh());
                monDTO.setTinhTrang("true");
                khoMon.put(item.getMaMon(), monDTO);
            }

            dishesAdapter.notifyDataSetChanged();
            updateTotalPriceDisplay(dishesAdapter.getSelectedQuantities());
            Toast.makeText(this, "Đã thêm " + item.getTenMon() + " vào đơn đặt trước (+1)", Toast.LENGTH_SHORT).show();
        });
        rvBestseller.setAdapter(bestsellerAdapter);

        ApiService api = ApiClient.getApiService();
        api.getBestseller(30, 8, null).enqueue(new Callback<com.sinhvien.orderdrinkapp.Api.BestsellerResponse>() {
            @Override
            public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.BestsellerResponse> call, Response<com.sinhvien.orderdrinkapp.Api.BestsellerResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<com.sinhvien.orderdrinkapp.Api.BestsellerResponse.BestsellerItem> list = response.body().getDanhSach();
                    if (list != null && !list.isEmpty()) {
                        bestsellerAdapter.setData(list);
                        layoutBestseller.setVisibility(View.VISIBLE);
                    } else {
                        // Ẩn khối khi chưa đủ dữ liệu (Requirement 13.4, 16.4)
                        layoutBestseller.setVisibility(View.GONE);
                    }
                } else {
                    // Ẩn khối khi yêu cầu thất bại (Requirement 13.4, 16.4)
                    layoutBestseller.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.BestsellerResponse> call, Throwable t) {
                layoutBestseller.setVisibility(View.GONE);
            }
        });
    }

    /** Lấy danh mục từ máy chủ rồi dựng dãy chip lọc. */
    private void taiDanhMuc() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCategories().enqueue(
                new Callback<List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>>() {
            @Override
            public void onResponse(Call<List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>> call,
                                   Response<List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    danhSachLoai.clear();
                    danhSachLoai.addAll(response.body());
                    veChipDanhMuc();
                }
            }

            @Override
            public void onFailure(Call<List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse>> call, Throwable t) {
                // Mất mạng thì vẫn còn chip "Tất cả" để khách xem thực đơn
                // từ bộ nhớ đệm — không chặn cả màn hình vì một dãy lọc.
                Log.w(TAG, "Không tải được danh mục: " + t.getMessage());
                if (!isFinishing() && !isDestroyed()) veChipDanhMuc();
            }
        });
    }

    /** Dựng dãy chip: "Tất cả" + mỗi danh mục một chip. */
    private void veChipDanhMuc() {
        layout_chip_danh_muc.removeAllViews();
        layout_chip_danh_muc.addView(taoChip(0, getString(R.string.booking_all_categories)));
        for (com.sinhvien.orderdrinkapp.Api.LoaiMonResponse loai : danhSachLoai) {
            layout_chip_danh_muc.addView(taoChip(loai.getMaLoai(), loai.getTenLoai()));
        }
        capNhatChipDangChon();
    }

    private TextView taoChip(int maLoai, String nhan) {
        TextView chip = new TextView(this);
        chip.setText(nhan);
        chip.setTextSize(13);
        chip.setAllCaps(false);
        chip.setSingleLine(true);
        chip.setBackgroundResource(R.drawable.bg_chip_danh_muc);
        chip.setTextColor(androidx.core.content.ContextCompat
                .getColorStateList(this, R.color.text_chip_danh_muc));

        int dx = (int) (14 * getResources().getDisplayMetrics().density);
        int dy = (int) (8  * getResources().getDisplayMetrics().density);
        chip.setPadding(dx, dy, dx, dy);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = (int) (8 * getResources().getDisplayMetrics().density);
        chip.setLayoutParams(lp);

        chip.setTag(maLoai);
        chip.setOnClickListener(v -> {
            if (maLoaiDangChon == maLoai) return;
            maLoaiDangChon = maLoai;
            trangHienTai = 1;   // đổi danh mục thì số trang cũ vô nghĩa
            capNhatChipDangChon();
            taiMon();
        });
        return chip;
    }

    private void capNhatChipDangChon() {
        for (int i = 0; i < layout_chip_danh_muc.getChildCount(); i++) {
            View v = layout_chip_danh_muc.getChildAt(i);
            Object tag = v.getTag();
            v.setSelected(tag instanceof Integer && (Integer) tag == maLoaiDangChon);
        }
    }

    /** Bật/tắt hai nút chuyển trang và cập nhật hai dòng chữ đi kèm. */
    private void capNhatThanhPhanTrang() {
        txt_so_trang.setText(getString(R.string.booking_page_indicator, trangHienTai, tongSoTrang));

        int daChon = dishesAdapter != null ? dishesAdapter.getSelectedQuantities().size() : 0;
        txt_tong_so_mon.setText(daChon > 0
                ? getString(R.string.booking_dish_count_selected, tongSoMon, daChon)
                : getString(R.string.booking_dish_count, tongSoMon));

        boolean coTruoc = trangHienTai > 1;
        boolean coSau   = trangHienTai < tongSoTrang;
        btn_trang_truoc.setEnabled(coTruoc);
        btn_trang_sau.setEnabled(coSau);
        btn_trang_truoc.setAlpha(coTruoc ? 1f : 0.45f);
        btn_trang_sau.setAlpha(coSau ? 1f : 0.45f);
    }

    /**
     * Nạp một trang món theo danh mục và từ khóa đang chọn.
     *
     * Máy chủ lo cả ba việc lọc — danh mục, từ khóa, phân trang — qua
     * `get_dishes.php`. Trước đây màn hình này gọi `limit=1000` lấy sạch
     * 62 món rồi tự lọc ở client, nên tham số phân trang của endpoint gần
     * như không được dùng tới.
     */
    private void taiMon() {
        progress_mon.setVisibility(View.VISIBLE);
        txt_khong_co_mon.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getDishesPhanTrang(maLoaiDangChon, trangHienTai, MON_MOI_TRANG, tuKhoa, 1)
                .enqueue(new Callback<com.sinhvien.orderdrinkapp.Api.DishPageResponse>() {
            @Override
            public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> call,
                                   Response<com.sinhvien.orderdrinkapp.Api.DishPageResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                progress_mon.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null
                        || !"success".equals(response.body().getStatus())) {
                    Log.w(TAG, "Tải món thất bại, HTTP " + response.code());
                    hienThiTrangTuBoNhoDem();
                    return;
                }

                com.sinhvien.orderdrinkapp.Api.DishPageResponse body = response.body();
                List<MonDTO> trang = new ArrayList<>();
                if (body.getData() != null) {
                    for (com.sinhvien.orderdrinkapp.Api.MonResponse r : body.getData()) {
                        MonDTO m = new MonDTO();
                        m.setMaMon(r.getMaMon());
                        m.setTenMon(r.getTenMon());
                        m.setGiaTien(r.getGiaTien());
                        m.setMaLoai(r.getMaLoai());
                        m.setTinhTrang(r.getTinhTrang());
                        m.setHinhAnhUrl(r.getHinhAnh());
                        trang.add(m);
                    }
                }

                tongSoMon   = body.getTotal();
                tongSoTrang = body.getTotalPages();

                // Người dùng đang ở trang 3 rồi quản trị viên xóa bớt món:
                // trang 3 không còn tồn tại. Lùi về trang cuối thay vì hiện
                // một danh sách rỗng khó hiểu.
                if (trang.isEmpty() && trangHienTai > tongSoTrang && tongSoTrang >= 1) {
                    trangHienTai = tongSoTrang;
                    taiMon();
                    return;
                }

                ghiVaoKhoMon(trang);
                hienThiTrang(trang);
            }

            @Override
            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                progress_mon.setVisibility(View.GONE);
                Log.w(TAG, "Lỗi mạng khi tải món: " + t.getMessage());
                hienThiTrangTuBoNhoDem();
            }
        });
    }

    /**
     * Đường lùi khi không gọi được máy chủ: lấy từ SQLite rồi tự cắt trang.
     *
     * Giữ lại nhánh này vì thực đơn là thứ khách cần xem được cả khi mạng
     * chập chờn. Sắp xếp theo tên để ranh giới trang trùng với máy chủ
     * (`ORDER BY TENMON ASC`), nếu không thì lúc mạng có lại, cùng một số
     * trang sẽ hiện món khác và trông như lỗi.
     */
    private void hienThiTrangTuBoNhoDem() {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            // Lọc từ khóa ở đây chứ không bằng LIKE của SQLite: LIKE phân biệt
            // dấu, gõ "pho" sẽ không ra "Phở Bò" (máy chủ thì có, MySQL so
            // không phân biệt dấu) — mất mạng là kết quả tìm kiếm đổi theo.
            List<MonDTO> nguon = (maLoaiDangChon == 0)
                    ? dbHelper.getAllDishes()
                    : dbHelper.getDishes(maLoaiDangChon, null);
            String khoa = com.sinhvien.orderdrinkapp.Utils.ViewUtils.boDau(tuKhoa);

            List<MonDTO> loc = new ArrayList<>();
            for (MonDTO m : nguon) {
                if (!"true".equalsIgnoreCase(m.getTinhTrang())) continue;
                if (!khoa.isEmpty()
                        && !com.sinhvien.orderdrinkapp.Utils.ViewUtils.boDau(m.getTenMon()).contains(khoa)) {
                    continue;
                }
                loc.add(m);
            }
            java.util.Collections.sort(loc, (a, b) ->
                    String.valueOf(a.getTenMon()).compareToIgnoreCase(String.valueOf(b.getTenMon())));

            int tong = loc.size();
            int soTrang = Math.max(1, (int) Math.ceil(tong / (double) MON_MOI_TRANG));
            int trang = Math.min(trangHienTai, soTrang);
            int tu = (trang - 1) * MON_MOI_TRANG;
            int den = Math.min(tu + MON_MOI_TRANG, tong);
            List<MonDTO> cat = (tu < den) ? new ArrayList<>(loc.subList(tu, den)) : new ArrayList<>();

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                tongSoMon = tong;
                tongSoTrang = soTrang;
                trangHienTai = trang;
                ghiVaoKhoMon(cat);
                hienThiTrang(cat);
            });
        });
    }

    /** Đổ một trang món lên RecyclerView. KHÔNG đụng tới giỏ đã chọn. */
    private void hienThiTrang(List<MonDTO> trang) {
        dishList.clear();
        dishList.addAll(trang);
        dishesAdapter.notifyDataSetChanged();

        txt_khong_co_mon.setVisibility(trang.isEmpty() ? View.VISIBLE : View.GONE);
        rv_booking_dishes.scrollToPosition(0);

        updateTotalPriceDisplay(dishesAdapter.getSelectedQuantities());
    }

    /* ═══════════════════════ MÃ GIẢM GIÁ ═══════════════════════ */

    /**
     * Nạp các mã còn dùng được của khách.
     *
     * Dùng chung `loyalty_home.php` thay vì viết endpoint riêng: nó đã trả
     * về đủ danh sách mã kèm trạng thái, và một endpoint ít hơn là một chỗ
     * ít hơn để hai bên lệch nhau về "mã nào còn dùng được".
     *
     * Lọc 'chuadung' ở client vì máy chủ trả cả mã đã dùng và hết hạn để
     * màn hình Điểm danh hiển thị lịch sử — ở đây chỉ cần cái còn xài.
     */
    private void taiMaKhaDung() {
        int makh = SessionManager.getMaNV(this);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.layDuLieuDiemDanh(makh).enqueue(
                new Callback<com.sinhvien.orderdrinkapp.Api.LoyaltyResponse>() {
            @Override
            public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.LoyaltyResponse> call,
                                   Response<com.sinhvien.orderdrinkapp.Api.LoyaltyResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getMaCuaToi() == null) return;

                maKhaDung.clear();
                for (com.sinhvien.orderdrinkapp.Api.LoyaltyResponse.MaGiamGia m
                        : response.body().getMaCuaToi()) {
                    if ("chuadung".equals(m.getTinhTrang())) maKhaDung.add(m);
                }
                capNhatHienThiVoucher();
            }

            @Override
            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.LoyaltyResponse> call, Throwable t) {
                // Không báo lỗi: mã giảm giá là tùy chọn, mất mạng thì khách
                // vẫn phải đặt bàn được. Chỉ là không chọn được mã lần này.
                Log.w(TAG, "Không tải được danh sách mã: " + t.getMessage());
            }
        });
    }

    private void moDanhSachVoucher() {
        if (maKhaDung.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.booking_voucher_tieu_de)
                    .setMessage(R.string.booking_voucher_khong_co)
                    .setPositiveButton("Đã hiểu", null)
                    .show();
            return;
        }

        final java.text.DecimalFormat dt = com.sinhvien.orderdrinkapp.Utils.TienTe.dinhDang();
        String[] nhan = new String[maKhaDung.size()];
        for (int i = 0; i < maKhaDung.size(); i++) {
            com.sinhvien.orderdrinkapp.Api.LoyaltyResponse.MaGiamGia m = maKhaDung.get(i);
            // Hiện điều kiện hóa đơn tối thiểu ngay trong danh sách: khách
            // cần biết TRƯỚC khi chọn, không phải lúc thanh toán mới vỡ lẽ.
            nhan[i] = m.getTen()
                    + (m.getDonToiThieu() > 0
                        ? "\n   Hóa đơn từ " + dt.format(m.getDonToiThieu()) + "đ"
                        : "")
                    + "\n   " + m.getMaCode();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.booking_voucher_tieu_de)
                .setItems(nhan, (d, i) -> {
                    maVoucherDaChon = maKhaDung.get(i).getMaCode();
                    capNhatHienThiVoucher();
                    Toast.makeText(this, R.string.booking_voucher_giu_cho, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void boChonVoucher() {
        maVoucherDaChon = null;
        capNhatHienThiVoucher();
    }

    private void capNhatHienThiVoucher() {
        if (maVoucherDaChon == null) {
            txt_voucher_da_chon.setText(R.string.booking_voucher_chua_chon);
            txt_voucher_da_chon.setTextColor(android.graphics.Color.parseColor("#8D6E63"));
            txt_voucher_thao_tac.setText(R.string.booking_voucher_chon);
            return;
        }

        String ten = maVoucherDaChon;
        for (com.sinhvien.orderdrinkapp.Api.LoyaltyResponse.MaGiamGia m : maKhaDung) {
            if (maVoucherDaChon.equals(m.getMaCode())) { ten = m.getTen(); break; }
        }
        txt_voucher_da_chon.setText(ten + " · " + maVoucherDaChon);
        txt_voucher_da_chon.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
        txt_voucher_thao_tac.setText(R.string.booking_voucher_bo);
    }

    /**
     * Nạp lại khối chọn món.
     *
     * Gọi lúc mở màn hình và mỗi khi nhận sự kiện socket `menu_changed`
     * (quản trị viên vừa sửa thực đơn).
     *
     * Việc hiển thị nay do `taiMon()` lo — nó chỉ lấy ĐÚNG một trang từ
     * máy chủ. Hàm này thêm một việc nữa: kéo toàn bộ thực đơn về nền để
     * làm mới bộ nhớ đệm SQLite, vốn là đường lùi khi mất mạng.
     *
     * Hai việc tách rời nhau có chủ đích: giao diện hiện ra ngay sau một
     * yêu cầu nhỏ (6 món), còn việc đồng bộ 62 món chạy lặng lẽ phía sau
     * và không làm khách phải chờ.
     */
    private void loadDishes() {
        taiMon();
        dongBoBoNhoDemThucDon();
    }

    /**
     * Kéo toàn bộ thực đơn về ghi vào SQLite, không đụng tới giao diện.
     *
     * Vẫn gọi `getDishes(0, 1, 1000, "")` như cũ: `limit` lớn nên máy chủ
     * trả hết, đúng thứ bộ nhớ đệm cần. Khác bản cũ ở chỗ kết quả KHÔNG
     * còn được đổ thẳng lên màn hình.
     */
    private void dongBoBoNhoDemThucDon() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getDishes(0, 1, 1000, "").enqueue(
                new Callback<com.sinhvien.orderdrinkapp.Api.DishPageResponse>() {
            @Override
            public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> call,
                                   Response<com.sinhvien.orderdrinkapp.Api.DishPageResponse> response) {
                if (!response.isSuccessful() || response.body() == null
                        || !"success".equals(response.body().getStatus())) {
                    return;
                }
                List<com.sinhvien.orderdrinkapp.Api.MonResponse> data = response.body().getData();
                if (data == null || data.isEmpty()) return;

                Map<Integer, List<com.sinhvien.orderdrinkapp.Api.MonResponse>> theoLoai = new HashMap<>();
                for (com.sinhvien.orderdrinkapp.Api.MonResponse r : data) {
                    if (!theoLoai.containsKey(r.getMaLoai())) {
                        theoLoai.put(r.getMaLoai(), new ArrayList<>());
                    }
                    theoLoai.get(r.getMaLoai()).add(r);
                }

                LocalDatabaseHelper.getExecutor().execute(() -> {
                    for (Map.Entry<Integer, List<com.sinhvien.orderdrinkapp.Api.MonResponse>> e : theoLoai.entrySet()) {
                        dbHelper.syncDishes(e.getKey(), e.getValue(), true);
                    }
                    Log.d(TAG, "Đã đồng bộ " + data.size() + " món vào bộ nhớ đệm");
                });
            }

            @Override
            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> call, Throwable t) {
                // Không báo cho người dùng: đây là việc chạy nền, màn hình
                // vẫn dùng được bằng dữ liệu từ taiMon() hoặc bộ nhớ đệm cũ.
                Log.w(TAG, "Không đồng bộ được bộ nhớ đệm thực đơn: " + t.getMessage());
            }
        });
    }

    /**
     * Xác thực thông tin biểu mẫu hẹn và gửi yêu cầu đặt bàn lên Server.
     */
    private void submitBooking() {
        if (!daChonDuGio()) {
            Toast.makeText(this, "Vui lòng chọn đầy đủ ngày và giờ hẹn!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tableList.isEmpty() || spinner_tables.getSelectedItemPosition() == -1) {
            Toast.makeText(this, "Giờ này không còn bàn trống, vui lòng chọn giờ khác!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Đảm bảo giờ hẹn phải ở tương lai
        Calendar now = Calendar.getInstance();
        Calendar chosen = Calendar.getInstance();
        chosen.set(selectedYear, selectedMonth - 1, selectedDay, selectedHour, selectedMinute);
        if (chosen.before(now)) {
            Toast.makeText(this, "Giờ hẹn phải sau thời điểm hiện tại!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra giờ hoạt động của nhà hàng (VD: Từ 08:00 sáng đến 22:00 tối)
        if (selectedHour >= 22 || selectedHour < 8) {
            Toast.makeText(this, "Nhà hàng chỉ nhận đặt bàn trong khung giờ từ 08:00 đến 22:00!", Toast.LENGTH_LONG).show();
            return;
        }

        int selectedPos = spinner_tables.getSelectedItemPosition();
        int maban = tableList.get(selectedPos).getMaBan();
        int makh = SessionManager.getMaNV(this);
        String datetime = String.format("%04d-%02d-%02d %02d:%02d:00", selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);

        // Đóng gói danh sách món ăn chọn trước sang định dạng Json
        List<Map<String, Object>> preorderList = new ArrayList<>();
        Map<Integer, Integer> quantities = dishesAdapter.getSelectedQuantities();
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("mamon", entry.getKey());
            item.put("soluong", entry.getValue());
            preorderList.add(item);
        }
        String jsonPreorder = new Gson().toJson(preorderList);

        // Hiển thị loading
        androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(this, "Đang xử lý đặt bàn...");
        progressDialog.show();

        // Gọi API đặt bàn
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        // maVoucherDaChon = null khi không gắn mã — Retrofit bỏ qua @Field null.
        apiService.createBooking(makh, maban, datetime, jsonPreorder, maVoucherDaChon)
                .enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    Log.d(TAG, "Đặt bàn thành công: maban=" + maban + ", datetime=" + datetime);
                    
                    // Phát tín hiệu Socket thông báo máy phục vụ / thu ngân tải lại trạng thái
                    io.socket.client.Socket socket = com.sinhvien.orderdrinkapp.Utils.SocketManager.getInstance().getSocket();
                    if (socket != null && socket.connected()) {
                        socket.emit("booking_status_updated");
                    }
                    Toast.makeText(CustomerBookingActivity.this, "Đặt bàn thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    // 409 (bàn vừa bị khách khác đặt, bàn bảo trì, mã giảm giá
                    // hỏng) hay 400 đều có lý do trong errorBody — trước đây đọc
                    // body() nên lần nào cũng chỉ hiện "Không thể đặt bàn!".
                    String msg = response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage()
                            : ViewUtils.docLoiMayChu(response, "Không thể đặt bàn!");
                    Log.w(TAG, "Đặt bàn thất bại: " + msg);
                    if (isFinishing() || isDestroyed()) return;
                    new androidx.appcompat.app.AlertDialog.Builder(CustomerBookingActivity.this)
                            .setTitle("Chưa đặt được bàn")
                            .setMessage(msg)
                            .setPositiveButton("Đã hiểu", null)
                            .show();
                    // Bàn có thể vừa bị người khác đặt mất — nạp lại danh sách
                    // để khách thấy ngay những bàn còn trống thật.
                    if (response.code() == 409) taiBanTrong();
                }
            }

            @Override
            public void onFailure(Call<BookingResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Lỗi kết nối đặt bàn: " + t.getMessage());
                Toast.makeText(CustomerBookingActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu trữ các trạng thái nhập biểu mẫu khi Activity thay đổi
        outState.putInt("selectedYear", selectedYear);
        outState.putInt("selectedMonth", selectedMonth);
        outState.putInt("selectedDay", selectedDay);
        outState.putInt("selectedHour", selectedHour);
        outState.putInt("selectedMinute", selectedMinute);

        int selectedPos = spinner_tables.getSelectedItemPosition();
        if (selectedPos != -1 && selectedPos < tableList.size()) {
            outState.putInt("selectedTableId", tableList.get(selectedPos).getMaBan());
        }
        if (dishesAdapter != null) {
            String jsonQuantities = new com.google.gson.Gson().toJson(dishesAdapter.getSelectedQuantities());
            outState.putString("selectedQuantities", jsonQuantities);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký lắng nghe sự kiện để tránh rò rỉ bộ nhớ
        if (mSocket != null && onMenuChanged != null) {
            mSocket.off("menu_changed", onMenuChanged);
        }
        if (mSocket != null && onBookingChanged != null) {
            mSocket.off("booking_status_updated", onBookingChanged);
        }
        if (yeuCauBanTrong != null) yeuCauBanTrong.cancel();
    }
}

