package com.sinhvien.orderdrinkapp.Activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.ChatGuiResponse;
import com.sinhvien.orderdrinkapp.Api.ChatResponse;
import com.sinhvien.orderdrinkapp.Api.TinNhan;
import com.sinhvien.orderdrinkapp.CustomAdapter.TinNhanAdapter;
import com.sinhvien.orderdrinkapp.R;
import com.sinhvien.orderdrinkapp.Utils.SessionManager;
import com.sinhvien.orderdrinkapp.Utils.SocketManager;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ChatActivity — phòng chat giữa khách hàng và bộ phận chăm sóc.
 *
 *
 * VÌ SAO LÀ ACTIVITY CHỨ KHÔNG PHẢI MỘT TAB NỮA
 * ==============================================
 * Thanh điều hướng dưới đã kín 5 mục, thêm mục thứ sáu là bắt đầu chen chúc.
 * Nhưng lý do chính không phải chỗ trống: màn chat cần TOÀN BỘ chiều cao khi
 * bàn phím bật lên, cần vòng đời riêng để biết lúc nào nên nghe tin mới, và
 * cần nút quay lại rõ ràng. Nhét vào một tab thì cả ba thứ đều phải làm thủ
 * công và làm sai.
 *
 *
 * HAI ĐƯỜNG NHẬN TIN, CÓ CHỦ Ý
 * ============================
 * 1. Socket.IO — tức thời, là đường chính.
 * 2. Hỏi lại máy chủ mỗi 8 giây — lưới an toàn.
 *
 * Nghe có vẻ thừa, nhưng socket đứt là chuyện thường trên mạng di động, và
 * nó đứt IM LẶNG: máy khách vẫn tưởng mình đang kết nối. Không có lưới thứ
 * hai thì khách ngồi nhìn màn hình trống trong khi nhân viên đã trả lời từ
 * lâu — hỏng đúng thứ tính năng này sinh ra để làm.
 *
 * Vòng hỏi lại rất rẻ vì dùng `tu_tin`: máy chủ chỉ trả phần MỚI HƠN mã tin
 * cuối đang có, thường là một mảng rỗng.
 *
 * Cả hai đường đều đổ vào `themTin()`, và hàm đó lọc trùng theo mã tin — nên
 * tin tới hai lần cũng chỉ hiện một lần.
 */
public class ChatActivity extends AppCompatActivity {

    /** Chu kỳ hỏi lại máy chủ khi màn hình đang mở. */
    private static final long CHU_KY_HOI_LAI_MS = 8000;

    /** Câu gợi ý cho khách chưa biết bắt đầu từ đâu. */
    private static final String[] GOI_Y = {
            "Mấy giờ mở cửa?",
            "Đặt bàn thế nào?",
            "Có chỗ đậu xe không?",
            "Cách đổi điểm lấy voucher"
    };

    private RecyclerView   dsTinNhan;
    private EditText       oNhap;
    private ImageButton    nutGui;
    private LinearLayout   khungChao;
    private LinearLayout   khungXacNhan;
    private Button         nutVanCan;
    private Button         nutKetThuc;
    private ProgressBar    vongQuay;
    private TextView       txtTrangThai;

    /** Trạng thái hội thoại lần cuối máy chủ báo về. */
    private String tinhTrang    = "";
    private boolean botConTruc  = true;

    private TinNhanAdapter adapter;
    private ApiService     api;

    private final Handler  tay = new Handler(Looper.getMainLooper());
    private Runnable       viecHoiLai;
    private Emitter.Listener ngheTinMoi;

    private boolean dangGui = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar thanh = findViewById(R.id.thanh_chat);
        setSupportActionBar(thanh);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        thanh.setNavigationOnClickListener(v -> finish());

        dsTinNhan    = findViewById(R.id.ds_tin_nhan);
        oNhap        = findViewById(R.id.o_nhap_tin);
        nutGui       = findViewById(R.id.nut_gui);
        khungChao    = findViewById(R.id.khung_chao);
        khungXacNhan = findViewById(R.id.khung_xac_nhan);
        nutVanCan    = findViewById(R.id.nut_van_can);
        nutKetThuc   = findViewById(R.id.nut_ket_thuc);
        vongQuay     = findViewById(R.id.vong_quay_chat);
        txtTrangThai = findViewById(R.id.txt_trang_thai_chat);

        nutVanCan.setOnClickListener(v -> thaoTac("tieptuc", "Đã báo lại cho nhân viên."));
        nutKetThuc.setOnClickListener(v -> thaoTac("ketthuc", "Cảm ơn bạn đã liên hệ!"));

        adapter = new TinNhanAdapter();

        // stackFromEnd: danh sách bám ĐÁY. Đây là hành vi đúng của mọi khung
        // chat — tin mới nhất phải nằm trong tầm mắt, không phải tin cũ nhất.
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        dsTinNhan.setLayoutManager(llm);
        dsTinNhan.setAdapter(adapter);

        api = ApiClient.getClient().create(ApiService.class);

        dungGoiY();

        nutGui.setOnClickListener(v -> gui(oNhap.getText().toString()));
        oNhap.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_SEND) {
                gui(oNhap.getText().toString());
                return true;
            }
            return false;
        });

        napLanDau();
        ngheSocket();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Ẩn "Gặp nhân viên" khi bot đã tắt: bấm lần nữa không làm gì thêm,
        // để đó chỉ khiến khách bấm rồi tự hỏi vì sao không thấy gì xảy ra.
        MenuItem gap = menu.findItem(R.id.menu_gap_nhan_vien);
        if (gap != null) gap.setVisible(botConTruc);

        MenuItem ket = menu.findItem(R.id.menu_ket_thuc);
        if (ket != null) ket.setVisible(!adapter.rong());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_gap_nhan_vien) {
            thaoTac("gapnhanvien", null);
            return true;
        }
        if (id == R.id.menu_ket_thuc) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Kết thúc trò chuyện?")
                    .setMessage("Bạn vẫn có thể nhắn lại bất cứ lúc nào — hệ thống sẽ mở một cuộc trò chuyện mới.")
                    .setPositiveButton("Kết thúc",
                            (d, w) -> thaoTac("ketthuc", "Cảm ơn bạn đã liên hệ!"))
                    .setNegativeButton("Để sau", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Gửi một thao tác vòng đời rồi nạp lại phần mới.
     *
     * Nạp lại thay vì tự vẽ kết quả: máy chủ mới là nơi biết trạng thái sau
     * thao tác, và mỗi thao tác đều kèm một tin nhắn hệ thống mà chỉ có nạp
     * lại mới thấy.
     */
    private void thaoTac(String action, String thongBao) {
        api.chatThaoTac(action).enqueue(new Callback<ChatGuiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatGuiResponse> c, @NonNull Response<ChatGuiResponse> r) {
                if (!r.isSuccessful() || r.body() == null || !r.body().thanhCong()) {
                    baoLoi(r.body() != null && r.body().getMessage() != null
                            ? r.body().getMessage() : "Không thực hiện được. Bạn thử lại nhé.");
                    return;
                }
                if (thongBao != null) Toast.makeText(ChatActivity.this, thongBao, Toast.LENGTH_SHORT).show();

                if ("ketthuc".equals(action)) {
                    // Hội thoại đã đóng: nạp lại sẽ ra rỗng, nên đóng luôn màn
                    // hình thay vì để khách nhìn một khung chat trắng.
                    finish();
                    return;
                }
                hoiTinMoi();
                napLaiTrangThai();
            }

            @Override
            public void onFailure(@NonNull Call<ChatGuiResponse> c, @NonNull Throwable t) {
                baoLoi("Mất kết nối. Bạn thử lại nhé.");
            }
        });
    }

    /** Hỏi lại riêng phần trạng thái, không kéo theo tin nhắn. */
    private void napLaiTrangThai() {
        api.layHoiThoai(adapter.maCuoi()).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> c, @NonNull Response<ChatResponse> r) {
                if (r.isSuccessful() && r.body() != null) {
                    for (TinNhan t : r.body().getTinNhan()) themTin(t);
                    capNhatTrangThai(r.body().getHoiThoai());
                    cuonXuongCuoi();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> c, @NonNull Throwable t) { }
        });
    }

    /* ══════════════════════ Vòng đời ══════════════════════ */

    @Override
    protected void onResume() {
        super.onResume();
        batHoiLai();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Dừng hẳn vòng hỏi lại khi màn hình khuất. Để nó chạy nền là gọi
        // mạng liên tục cho một màn hình không ai nhìn — tốn pin và tốn 3G
        // của khách mà không đem lại gì.
        tay.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tay.removeCallbacksAndMessages(null);
        Socket socket = SocketManager.getInstance().getSocket();
        if (socket != null && ngheTinMoi != null) {
            socket.off("chat_moi", ngheTinMoi);
        }
    }

    /* ══════════════════════ Nạp dữ liệu ══════════════════════ */

    private void napLanDau() {
        vongQuay.setVisibility(View.VISIBLE);
        api.layHoiThoai(0).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> c, @NonNull Response<ChatResponse> r) {
                vongQuay.setVisibility(View.GONE);
                if (!r.isSuccessful() || r.body() == null) {
                    baoLoi("Không tải được cuộc trò chuyện.");
                    return;
                }
                ChatResponse kq = r.body();
                adapter.datLai(kq.getTinNhan());
                capNhatKhungChao();
                capNhatTrangThai(kq.getHoiThoai());
                cuonXuongCuoi();
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> c, @NonNull Throwable t) {
                vongQuay.setVisibility(View.GONE);
                baoLoi("Mất kết nối. Kiểm tra mạng rồi thử lại nhé.");
            }
        });
    }

    /** Hỏi máy chủ phần MỚI HƠN mã tin cuối đang có. */
    private void hoiTinMoi() {
        api.layHoiThoai(adapter.maCuoi()).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> c, @NonNull Response<ChatResponse> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                for (TinNhan t : r.body().getTinNhan()) themTin(t);
                capNhatTrangThai(r.body().getHoiThoai());
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> c, @NonNull Throwable t) {
                // Im lặng. Đây là lưới an toàn chạy nền; báo lỗi mỗi lần mạng
                // chớp sẽ phủ kín màn hình bằng thông báo mà khách không làm
                // gì được.
            }
        });
    }

    private void batHoiLai() {
        tay.removeCallbacksAndMessages(null);
        viecHoiLai = new Runnable() {
            @Override
            public void run() {
                hoiTinMoi();
                tay.postDelayed(this, CHU_KY_HOI_LAI_MS);
            }
        };
        tay.postDelayed(viecHoiLai, CHU_KY_HOI_LAI_MS);
    }

    /* ══════════════════════ Gửi ══════════════════════ */

    private void gui(String noiDung) {
        final String tin = noiDung == null ? "" : noiDung.trim();
        if (TextUtils.isEmpty(tin)) return;
        if (dangGui) return;

        if (tin.length() > 2000) {
            Toast.makeText(this, "Tin nhắn quá dài. Bạn chia thành nhiều tin giúp mình nhé.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Hiện ngay tin của khách trước khi máy chủ trả lời (optimistic).
        //
        // Chờ máy chủ xong mới vẽ thì trên mạng chậm khách thấy ô nhập trống
        // trơn cả giây — cảm giác y hệt như bấm gửi mà không ăn thua, và họ
        // sẽ bấm lại.
        final TinNhan tam = TinNhan.tamThoi(tin);
        adapter.them(tam);
        capNhatKhungChao();
        cuonXuongCuoi();

        oNhap.setText("");
        datTrangThaiGui(true);

        api.guiTinNhan(tin).enqueue(new Callback<ChatGuiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatGuiResponse> c, @NonNull Response<ChatGuiResponse> r) {
                datTrangThaiGui(false);
                adapter.goTinTam(tam.getMaTinNhan());

                if (!r.isSuccessful() || r.body() == null || !r.body().thanhCong()) {
                    String loi = r.body() != null && r.body().getMessage() != null
                            ? r.body().getMessage() : "Không gửi được tin nhắn.";
                    // Trả chữ về ô nhập để khách khỏi phải gõ lại.
                    oNhap.setText(tin);
                    oNhap.setSelection(tin.length());
                    baoLoi(loi);
                    return;
                }

                for (TinNhan t : r.body().getTinMoi()) themTin(t);
                cuonXuongCuoi();
            }

            @Override
            public void onFailure(@NonNull Call<ChatGuiResponse> c, @NonNull Throwable t) {
                datTrangThaiGui(false);
                adapter.goTinTam(tam.getMaTinNhan());
                oNhap.setText(tin);
                oNhap.setSelection(tin.length());
                baoLoi("Không gửi được. Kiểm tra mạng rồi thử lại nhé.");
            }
        });
    }

    private void datTrangThaiGui(boolean dang) {
        dangGui = dang;
        nutGui.setEnabled(!dang);
        nutGui.setAlpha(dang ? 0.5f : 1f);
    }

    /* ══════════════════════ Socket.IO ══════════════════════ */

    private void ngheSocket() {
        Socket socket = SocketManager.getInstance().getSocket();
        if (socket == null) return;

        // Vào lại room phòng khi socket vừa nối lại sau khi đứt: server chỉ
        // nhớ room theo từng kết nối, kết nối mới là mất sạch.
        socket.emit("join_customer", SessionManager.getMaNV(this));

        final Gson gson = new Gson();
        ngheTinMoi = args -> {
            if (args == null || args.length == 0) return;
            try {
                TinNhan t = gson.fromJson(args[0].toString(), TinNhan.class);
                runOnUiThread(() -> {
                    themTin(t);
                    cuonXuongCuoi();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        socket.on("chat_moi", ngheTinMoi);
    }

    /* ══════════════════════ Giao diện ══════════════════════ */

    private void themTin(TinNhan t) {
        if (t == null) return;
        adapter.them(t);
        capNhatKhungChao();
    }

    private void capNhatKhungChao() {
        khungChao.setVisibility(adapter.rong() ? View.VISIBLE : View.GONE);
        invalidateOptionsMenu();
    }

    private void capNhatTrangThai(ChatResponse.HoiThoai ht) {
        if (ht == null) {
            tinhTrang  = "";
            botConTruc = true;
            txtTrangThai.setText("Royal Restaurant · 08:00 – 22:00");
            khungXacNhan.setVisibility(View.GONE);
            invalidateOptionsMenu();
            return;
        }

        tinhTrang  = ht.getTinhTrang();
        botConTruc = ht.botConTruc();

        // Phụ đề nói đúng chuyện đang xảy ra, không nói chung chung.
        //
        // "Đang chờ nhân viên" khác hẳn "Nhân viên đang hỗ trợ bạn": cái đầu
        // nghĩa là chưa ai đọc, cái sau nghĩa là đã có người. Gộp thành một
        // câu thì khách không biết mình còn phải chờ hay không.
        if ("dangxuly".equals(tinhTrang)) {
            txtTrangThai.setText("Nhân viên đang hỗ trợ bạn");
        } else if ("chodong".equals(tinhTrang)) {
            txtTrangThai.setText("Chờ bạn xác nhận hoàn tất");
        } else if ("daxong".equals(tinhTrang)) {
            txtTrangThai.setText("Đã kết thúc");
        } else if (!botConTruc) {
            txtTrangThai.setText("Đang chờ nhân viên…");
        } else {
            txtTrangThai.setText("Royal Restaurant · 08:00 – 22:00");
        }

        khungXacNhan.setVisibility("chodong".equals(tinhTrang) ? View.VISIBLE : View.GONE);
        invalidateOptionsMenu();
    }

    private void cuonXuongCuoi() {
        // post() chứ không gọi thẳng: lúc này RecyclerView chưa kịp đo lại
        // sau khi thêm dòng, nên scrollToPosition sẽ nhắm vào bố cục CŨ và
        // dừng lệch một dòng.
        dsTinNhan.post(() -> {
            int n = adapter.getItemCount();
            if (n > 0) dsTinNhan.scrollToPosition(n - 1);
        });
    }

    private void dungGoiY() {
        ChipGroup nhom = findViewById(R.id.nhom_goi_y);
        for (String cau : GOI_Y) {
            Chip chip = new Chip(this);
            chip.setText(cau);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> gui(cau));
            nhom.addView(chip);
        }
    }

    private void baoLoi(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
