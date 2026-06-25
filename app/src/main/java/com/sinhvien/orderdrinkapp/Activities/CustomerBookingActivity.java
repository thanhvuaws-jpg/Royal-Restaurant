package com.sinhvien.orderdrinkapp.Activities;

import android.app.DatePickerDialog;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
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
import com.sinhvien.orderdrinkapp.Api.BookingResponse;
import com.sinhvien.orderdrinkapp.Api.TableResponse;
import com.sinhvien.orderdrinkapp.CustomAdapter.PreorderDishesAdapter;
import com.sinhvien.orderdrinkapp.DTO.MonDTO;
import com.sinhvien.orderdrinkapp.DTO.BanAnDTO;
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
    Button btn_select_date, btn_select_time, btn_confirm_booking;
    TextView txt_selected_datetime, txt_total_preorder;
    RecyclerView rv_booking_dishes;

    // Danh sách bàn ăn & adapter hiển thị trên Spinner
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

    private int savedTableId = -1;
    private Map<Integer, Integer> savedQuantities = new java.util.HashMap<>();

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
        btn_select_date = findViewById(R.id.btn_select_date);
        btn_select_time = findViewById(R.id.btn_select_time);
        btn_confirm_booking = findViewById(R.id.btn_confirm_booking);
        txt_selected_datetime = findViewById(R.id.txt_selected_datetime);
        txt_total_preorder = findViewById(R.id.txt_total_preorder);
        rv_booking_dishes = findViewById(R.id.rv_booking_dishes);

        dbHelper = LocalDatabaseHelper.getInstance(this);

        // Khởi tạo Spinner danh sách bàn ăn
        tableAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tableNames);
        tableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_tables.setAdapter(tableAdapter);

        // Nạp danh sách bàn ăn, đăng ký sự kiện picker và tải món ăn
        loadTables();
        setupDateTimePickers();
        loadDishes();

        if (savedInstanceState != null) {
            updateDateTimeDisplay();
        }

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
        if (mSocket != null) {
            mSocket.on("menu_changed", onMenuChanged);
        }
    }

    /**
     * Tải danh sách bàn ăn (Ưu tiên nạp bộ nhớ cache từ SQLite trước để tối ưu tốc độ, sau đó gọi mạng và đồng bộ).
     */
    private void loadTables() {
        // 1. Tải từ SQLite cache
        LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(this);
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<BanAnDTO> cachedList = dbHelper.getTables();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                updateSpinnerWithTables(cachedList);
            });
        });

        // 2. Đồng bộ mới từ API Server
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getTables().enqueue(new Callback<List<TableResponse>>() {
            @Override
            public void onResponse(Call<List<TableResponse>> call, Response<List<TableResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncTables(response.body()); // Lưu vào SQLite
                        List<BanAnDTO> updatedList = dbHelper.getTables();
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            updateSpinnerWithTables(updatedList);
                        });
                    });
                }
            }

            @Override
            public void onFailure(Call<List<TableResponse>> call, Throwable t) {
                if (tableList.isEmpty()) {
                    Toast.makeText(CustomerBookingActivity.this, "Lỗi tải bàn: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Cập nhật hiển thị danh sách bàn lên Spinner (chỉ lấy những bàn có tình trạng trống).
     */
    private void updateSpinnerWithTables(List<BanAnDTO> list) {
        tableList.clear();
        tableNames.clear();
        for (BanAnDTO ban : list) {
            // Chỉ hiển thị các bàn trống (tinhTrang = false)
            if ("false".equalsIgnoreCase(ban.getTinhTrang())) {
                TableResponse t = new TableResponse();
                t.setMaBan(ban.getMaBan());
                t.setTenBan(ban.getTenBan());
                t.setTinhTrang(ban.getTinhTrang());
                tableList.add(t);
                tableNames.add(ban.getTenBan());
            }
        }
        if (tableList.isEmpty()) {
            tableNames.add("Hiện tại không có bàn trống");
        }
        tableAdapter.notifyDataSetChanged();

        // Khôi phục lại bàn được chọn trước đó
        if (savedTableId != -1) {
            for (int i = 0; i < tableList.size(); i++) {
                if (tableList.get(i).getMaBan() == savedTableId) {
                    spinner_tables.setSelection(i);
                    break;
                }
            }
        }
    }

    /**
     * Khởi tạo và thiết lập các hộp thoại chọn Ngày (DatePickerDialog) và Giờ (TimePickerDialog).
     */
    private void setupDateTimePickers() {
        Calendar calendar = Calendar.getInstance();

        // Click để hiển thị hộp chọn Ngày
        btn_select_date.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedYear = year;
                selectedMonth = month + 1;
                selectedDay = dayOfMonth;
                updateDateTimeDisplay();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        // Click để hiển thị hộp chọn Giờ
        btn_select_time.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                selectedHour = hourOfDay;
                selectedMinute = minute;
                updateDateTimeDisplay();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        });
    }

    /**
     * Cập nhật chuỗi hiển thị ngày giờ đã chọn lên giao diện người dùng.
     */
    private void updateDateTimeDisplay() {
        if (selectedYear != -1 && selectedHour != -1) {
            String datetime = String.format("%04d-%02d-%02d %02d:%02d:00", selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);
            txt_selected_datetime.setText("Thời gian hẹn: " + datetime);
        } else {
            String display = "";
            if (selectedYear != -1) {
                display += String.format("%02d/%02d/%04d", selectedDay, selectedMonth, selectedYear);
            }
            if (selectedHour != -1) {
                if (!display.isEmpty()) display += " lúc ";
                display += String.format("%02d:%02d", selectedHour, selectedMinute);
            }
            txt_selected_datetime.setText(display.isEmpty() ? "Chưa chọn thời gian" : display);
        }
    }

    /**
     * Tính toán tổng giá trị của các món ăn đặt trước và hiển thị lên giao diện.
     */
    private void updateTotalPriceDisplay(Map<Integer, Integer> quantities) {
        totalPreorderPrice = 0;
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            int mamon = entry.getKey();
            int qty = entry.getValue();
            for (MonDTO dish : dishList) {
                if (dish.getMaMon() == mamon) {
                    try {
                        totalPreorderPrice += qty * Long.parseLong(dish.getGiaTien());
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }
        }
        DecimalFormat formatter = new DecimalFormat("#,###");
        txt_total_preorder.setText("Tổng: " + formatter.format(totalPreorderPrice) + " đ");
    }

    /**
     * Tải danh sách món ăn từ SQLite cache lên giao diện, song song đồng bộ từ Server.
     */
    private void loadDishes() {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<MonDTO> allCached = dbHelper.getAllDishes();
            List<MonDTO> cachedList = new ArrayList<>();
            for (MonDTO dish : allCached) {
                // Chỉ lấy món ăn ở trạng thái đang phục vụ (true)
                if ("true".equalsIgnoreCase(dish.getTinhTrang())) {
                    cachedList.add(dish);
                }
            }
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                Map<Integer, Integer> currentSelected = dishesAdapter != null ? dishesAdapter.getSelectedQuantities() : new HashMap<>();
                dishList.clear();
                dishList.addAll(cachedList);
                if (dishesAdapter == null) {
                    dishesAdapter = new PreorderDishesAdapter(CustomerBookingActivity.this, dishList, quantities -> {
                        updateTotalPriceDisplay(quantities);
                    });
                    if (savedQuantities != null && !savedQuantities.isEmpty()) {
                        dishesAdapter.getSelectedQuantities().putAll(savedQuantities);
                        savedQuantities.clear();
                    }
                    rv_booking_dishes.setLayoutManager(new LinearLayoutManager(CustomerBookingActivity.this));
                    rv_booking_dishes.setAdapter(dishesAdapter);
                } else {
                    Map<Integer, Integer> validSelected = new HashMap<>();
                    for (Map.Entry<Integer, Integer> entry : currentSelected.entrySet()) {
                        for (MonDTO dish : dishList) {
                            if (dish.getMaMon() == entry.getKey()) {
                                validSelected.put(entry.getKey(), entry.getValue());
                                break;
                            }
                        }
                    }
                    dishesAdapter.getSelectedQuantities().clear();
                    dishesAdapter.getSelectedQuantities().putAll(validSelected);
                    dishesAdapter.notifyDataSetChanged();
                }
                updateTotalPriceDisplay(dishesAdapter.getSelectedQuantities());
            });
        });

        // Gọi API tải danh sách món ăn từ Server Cloud
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getDishes(0, 1, 1000, "").enqueue(new Callback<com.sinhvien.orderdrinkapp.Api.DishPageResponse>() {
            @Override
            public void onResponse(Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> call, Response<com.sinhvien.orderdrinkapp.Api.DishPageResponse> response) {
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    List<com.sinhvien.orderdrinkapp.Api.MonResponse> data = response.body().getData();
                    if (data != null && !data.isEmpty()) {
                        Map<Integer, List<com.sinhvien.orderdrinkapp.Api.MonResponse>> grouped = new HashMap<>();
                        for (com.sinhvien.orderdrinkapp.Api.MonResponse r : data) {
                            int catId = r.getMaLoai();
                            if (!grouped.containsKey(catId)) {
                                grouped.put(catId, new ArrayList<>());
                            }
                            grouped.get(catId).add(r);
                        }
                        LocalDatabaseHelper.getExecutor().execute(() -> {
                            for (Map.Entry<Integer, List<com.sinhvien.orderdrinkapp.Api.MonResponse>> entry : grouped.entrySet()) {
                                dbHelper.syncDishes(entry.getKey(), entry.getValue(), true);
                            }
                            List<MonDTO> allUpdated = dbHelper.getAllDishes();
                            List<MonDTO> updatedList = new ArrayList<>();
                            for (MonDTO dish : allUpdated) {
                                if ("true".equalsIgnoreCase(dish.getTinhTrang())) {
                                    updatedList.add(dish);
                                }
                            }
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                Map<Integer, Integer> currentSelected = dishesAdapter != null ? dishesAdapter.getSelectedQuantities() : new HashMap<>();
                                dishList.clear();
                                dishList.addAll(updatedList);
                                if (dishesAdapter != null) {
                                    Map<Integer, Integer> validSelected = new HashMap<>();
                                    for (Map.Entry<Integer, Integer> entry : currentSelected.entrySet()) {
                                        for (MonDTO dish : dishList) {
                                            if (dish.getMaMon() == entry.getKey()) {
                                                validSelected.put(entry.getKey(), entry.getValue());
                                                break;
                                            }
                                        }
                                    }
                                    dishesAdapter.getSelectedQuantities().clear();
                                    dishesAdapter.getSelectedQuantities().putAll(validSelected);
                                    dishesAdapter.notifyDataSetChanged();
                                    updateTotalPriceDisplay(dishesAdapter.getSelectedQuantities());
                                }
                            });
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> call, Throwable t) {}
        });
    }

    /**
     * Xác thực thông tin biểu mẫu hẹn và gửi yêu cầu đặt bàn lên Server.
     */
    private void submitBooking() {
        if (tableList.isEmpty() || spinner_tables.getSelectedItemPosition() == -1) {
            Toast.makeText(this, "Không có bàn trống nào để đặt!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedYear == -1 || selectedHour == -1) {
            Toast.makeText(this, "Vui lòng chọn đầy đủ ngày và giờ hẹn!", Toast.LENGTH_SHORT).show();
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
        apiService.createBooking(makh, maban, datetime, jsonPreorder).enqueue(new Callback<BookingResponse>() {
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
                    String msg = response.body() != null ? response.body().getMessage() : "Không thể đặt bàn!";
                    Log.w(TAG, "Đặt bàn thất bại: " + msg);
                    Toast.makeText(CustomerBookingActivity.this, msg, Toast.LENGTH_SHORT).show();
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
    }
}

