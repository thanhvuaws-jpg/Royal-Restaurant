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

public class CustomerBookingActivity extends AppCompatActivity {

    private static final String TAG = "CustomerBookingActivity";

    Spinner spinner_tables;
    Button btn_select_date, btn_select_time, btn_confirm_booking;
    TextView txt_selected_datetime, txt_total_preorder;
    RecyclerView rv_booking_dishes;

    List<TableResponse> tableList = new ArrayList<>();
    List<String> tableNames = new ArrayList<>();
    ArrayAdapter<String> tableAdapter;

    LocalDatabaseHelper dbHelper;
    PreorderDishesAdapter dishesAdapter;
    List<MonDTO> dishList = new ArrayList<>();

    int selectedYear = -1, selectedMonth = -1, selectedDay = -1;
    int selectedHour = -1, selectedMinute = -1;

    long totalPreorderPrice = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_booking);

        Toolbar toolbar = findViewById(R.id.booking_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        spinner_tables = findViewById(R.id.spinner_tables);
        btn_select_date = findViewById(R.id.btn_select_date);
        btn_select_time = findViewById(R.id.btn_select_time);
        btn_confirm_booking = findViewById(R.id.btn_confirm_booking);
        txt_selected_datetime = findViewById(R.id.txt_selected_datetime);
        txt_total_preorder = findViewById(R.id.txt_total_preorder);
        rv_booking_dishes = findViewById(R.id.rv_booking_dishes);

        dbHelper = LocalDatabaseHelper.getInstance(this);

        // Khởi tạo spinner bàn ăn
        tableAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tableNames);
        tableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_tables.setAdapter(tableAdapter);

        loadTables();
        setupDateTimePickers();
        loadDishes();

        btn_confirm_booking.setOnClickListener(v -> {
            if (ViewUtils.isFastDoubleClick()) return; // Chống double click
            submitBooking();
        });
    }

    private void loadTables() {
        // 1. Tải từ SQLite trước
        LocalDatabaseHelper dbHelper = LocalDatabaseHelper.getInstance(this);
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<BanAnDTO> cachedList = dbHelper.getTables();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                updateSpinnerWithTables(cachedList);
            });
        });

        // 2. Đồng bộ từ network
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getTables().enqueue(new Callback<List<TableResponse>>() {
            @Override
            public void onResponse(Call<List<TableResponse>> call, Response<List<TableResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LocalDatabaseHelper.getExecutor().execute(() -> {
                        dbHelper.syncTables(response.body());
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
    }

    private void setupDateTimePickers() {
        Calendar calendar = Calendar.getInstance();

        btn_select_date.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedYear = year;
                selectedMonth = month + 1;
                selectedDay = dayOfMonth;
                updateDateTimeDisplay();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        btn_select_time.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                selectedHour = hourOfDay;
                selectedMinute = minute;
                updateDateTimeDisplay();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        });
    }

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

    private void loadDishes() {
        LocalDatabaseHelper.getExecutor().execute(() -> {
            List<MonDTO> cachedList = dbHelper.getAllDishes();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                dishList.clear();
                dishList.addAll(cachedList);
                if (dishesAdapter != null) dishesAdapter.notifyDataSetChanged();
                dishesAdapter = new PreorderDishesAdapter(CustomerBookingActivity.this, dishList, quantities -> {
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
                });
                rv_booking_dishes.setLayoutManager(new LinearLayoutManager(CustomerBookingActivity.this));
                rv_booking_dishes.setAdapter(dishesAdapter);
            });
        });

        // Đồng bộ toàn bộ món ăn từ Server về SQLite
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
                            List<MonDTO> updatedList = dbHelper.getAllDishes();
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                dishList.clear();
                                dishList.addAll(updatedList);
                                if (dishesAdapter != null) dishesAdapter.notifyDataSetChanged();
                            });
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> call, Throwable t) {}
        });
    }

    private void submitBooking() {
        if (tableList.isEmpty() || spinner_tables.getSelectedItemPosition() == -1) {
            Toast.makeText(this, "Không có bàn trống nào để đặt!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedYear == -1 || selectedHour == -1) {
            Toast.makeText(this, "Vui lòng chọn đầy đủ ngày và giờ hẹn!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra lịch hẹn phải ở tương lai
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

        // Chuẩn bị danh sách món ăn đặt trước thành chuỗi JSON
        List<Map<String, Object>> preorderList = new ArrayList<>();
        Map<Integer, Integer> quantities = dishesAdapter.getSelectedQuantities();
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("mamon", entry.getKey());
            item.put("soluong", entry.getValue());
            preorderList.add(item);
        }
        String jsonPreorder = new Gson().toJson(preorderList);

        androidx.appcompat.app.AlertDialog progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(this, "Đang xử lý đặt bàn...");
        progressDialog.show();

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.createBooking(makh, maban, datetime, jsonPreorder).enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                    Log.d(TAG, "Đặt bàn thành công: maban=" + maban + ", datetime=" + datetime);
                    // Emit socket để cashier web biết có booking mới
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
}
