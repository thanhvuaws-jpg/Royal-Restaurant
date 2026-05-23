package com.sinhvien.orderdrinkapp.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.sinhvien.orderdrinkapp.Activities.DetailStatisticActivity;
import com.sinhvien.orderdrinkapp.Activities.HomeActivity;
import com.sinhvien.orderdrinkapp.CustomAdapter.AdapterDisplayStatistic;
import com.sinhvien.orderdrinkapp.DTO.DonDatDTO;
import com.sinhvien.orderdrinkapp.Api.ApiClient;
import com.sinhvien.orderdrinkapp.Api.ApiService;
import com.sinhvien.orderdrinkapp.Api.OrderResponse;
import com.sinhvien.orderdrinkapp.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DisplayStatisticFragment extends Fragment {

    // Views
    RecyclerView rv_statistic_OrderList;
    BarChart chart_statistic_Revenue;
    TextView txt_statistic_TotalRevenue, txt_statistic_OrderCount, txt_statistic_AvgOrder;
    ChipGroup chipGroup_statistic_Filter;

    // Data
    List<DonDatDTO> tatCaDonDat; // Toàn bộ đơn đã hoàn thành
    AdapterDisplayStatistic adapterDisplayStatistic;
    View view;

    private static final int FILTER_TODAY   = 0;
    private static final int FILTER_7DAYS   = 7;
    private static final int FILTER_30DAYS  = 30;
    private static final int FILTER_ALL     = -1;
    private int currentFilter = FILTER_7DAYS;

    private static final SimpleDateFormat DB_FORMAT =
            new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.displaystatistic_layout, container, false);

        if (getActivity() != null &&
                ((HomeActivity) getActivity()).getSupportActionBar() != null) {
            ((HomeActivity) getActivity()).getSupportActionBar()
                    .setTitle(R.string.nav_statistic);
        }

        // Bind views
        rv_statistic_OrderList      = view.findViewById(R.id.rv_statistic_OrderList);
        rv_statistic_OrderList.setLayoutManager(new LinearLayoutManager(getActivity()));
        chart_statistic_Revenue     = view.findViewById(R.id.chart_statistic_Revenue);
        txt_statistic_TotalRevenue  = view.findViewById(R.id.txt_statistic_TotalRevenue);
        txt_statistic_OrderCount    = view.findViewById(R.id.txt_statistic_OrderCount);
        txt_statistic_AvgOrder      = view.findViewById(R.id.txt_statistic_AvgOrder);
        chipGroup_statistic_Filter  = view.findViewById(R.id.chipGroup_statistic_Filter);


        tatCaDonDat = new ArrayList<>(); // Khởi tạo trống để tránh crash khi chưa có dữ liệu
        HienThiDSThongKe();

        // Chip filter listener
        chipGroup_statistic_Filter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_filter_Today) {
                currentFilter = FILTER_TODAY;
            } else if (checkedId == R.id.chip_filter_7Days) {
                currentFilter = FILTER_7DAYS;
            } else if (checkedId == R.id.chip_filter_30Days) {
                currentFilter = FILTER_30DAYS;
            } else if (checkedId == R.id.chip_filter_All) {
                currentFilter = FILTER_ALL;
            }
            capNhatDashboard();
        });

        // Mặc định chọn "7 ngày"
        ((Chip) view.findViewById(R.id.chip_filter_7Days)).setChecked(true);
        capNhatDashboard();

        // Click vào đơn → xem chi tiết (sẽ được gán trong capNhatDashboard)

        return view;
    }

    private void HienThiDSThongKe(){
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getPaidOrders().enqueue(new Callback<List<OrderResponse>>() {
            @Override
            public void onResponse(Call<List<OrderResponse>> call, Response<List<OrderResponse>> response) {
                if (!isAdded() || getActivity() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    tatCaDonDat = new ArrayList<>();
                    SimpleDateFormat cloudFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    for (OrderResponse res : response.body()) {
                        DonDatDTO dto = new DonDatDTO();
                        dto.setMaDonDat(res.getMaDonDat());
                        dto.setMaNV(res.getMaNV());
                        dto.setMaBan(res.getMaBan());
                        dto.setTongTien(String.valueOf(res.getTongTien()));
                        dto.setTinhTrang("true");
                        dto.setTenNV(res.getHoTenNV());
                        dto.setTenBan(res.getTenBan());

                        // Chuyển ngày từ Cloud (yyyy-MM-dd) sang App (dd-MM-yyyy)
                        try {
                            Date d = cloudFormat.parse(res.getNgayDat());
                            dto.setNgayDat(DB_FORMAT.format(d));
                        } catch (Exception e) {
                            dto.setNgayDat(res.getNgayDat());
                        }
                        tatCaDonDat.add(dto);
                    }
                    capNhatDashboard();
                }
            }

            @Override
            public void onFailure(Call<List<OrderResponse>> call, Throwable t) {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi thống kê Cloud: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private List<DonDatDTO> layDanhSachDaLoc() {
        if (currentFilter == FILTER_ALL) return tatCaDonDat;

        List<DonDatDTO> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        if (currentFilter == FILTER_TODAY) {
            String today = DB_FORMAT.format(cal.getTime());
            for (DonDatDTO don : tatCaDonDat) {
                if (today.equals(don.getNgayDat())) result.add(don);
            }
        } else {
            // FILTER_7DAYS hoặc FILTER_30DAYS
            cal.add(Calendar.DAY_OF_YEAR, -currentFilter);
            Date cutoff = cal.getTime();
            for (DonDatDTO don : tatCaDonDat) {
                try {
                    String sNgay = don.getNgayDat();
                    if(sNgay != null){
                        Date ngay = DB_FORMAT.parse(sNgay);
                        if (ngay != null && !ngay.before(cutoff)) result.add(don);
                    }
                } catch (ParseException ignored) {}
            }
        }
        return result;
    }

    /** Cập nhật toàn bộ dashboard: cards + chart + list */
    private void capNhatDashboard() {
        List<DonDatDTO> filtered = layDanhSachDaLoc();

        // === Summary cards ===
        long tongDoanhThu = 0;
        for (DonDatDTO don : filtered) {
            try {
                String sTien = don.getTongTien();
                if(sTien != null && !sTien.isEmpty() && !sTien.equals("null")){
                    tongDoanhThu += (long) Double.parseDouble(sTien);
                }
            }
            catch (Exception ignored) {}
        }
        int soDon = filtered.size();
        long trungBinh = soDon > 0 ? tongDoanhThu / soDon : 0;

        txt_statistic_TotalRevenue.setText(
                String.format("%,d", tongDoanhThu) + " " +
                        getString(R.string.currency_vnd));
        txt_statistic_OrderCount.setText(String.valueOf(soDon));
        txt_statistic_AvgOrder.setText(
                String.format("%,d", trungBinh) + " " +
                        getString(R.string.currency_vnd));

        // === RecyclerView ===
        adapterDisplayStatistic = new AdapterDisplayStatistic(
                getActivity(), filtered);
        rv_statistic_OrderList.setAdapter(adapterDisplayStatistic);
        
        adapterDisplayStatistic.setOnItemClickListener(position -> {
            if (position >= filtered.size()) return;
            DonDatDTO don = filtered.get(position);
            Intent intent = new Intent(getActivity(), DetailStatisticActivity.class);
            intent.putExtra("madon", don.getMaDonDat());
            intent.putExtra("manv", don.getMaNV());
            intent.putExtra("maban", don.getMaBan());
            intent.putExtra("ngaydat", don.getNgayDat());
            intent.putExtra("tongtien", don.getTongTien());
            intent.putExtra("tennv", don.getTenNV());
            intent.putExtra("tenban", don.getTenBan());
            startActivity(intent);
        });

        // === Bar chart ===
        setupBarChart(filtered);
    }

    private void setupBarChart(List<DonDatDTO> filtered) {
        if (filtered.isEmpty()) {
            chart_statistic_Revenue.clear();
            chart_statistic_Revenue.setNoDataText(getString(R.string.stat_no_data));
            chart_statistic_Revenue.setNoDataTextColor(
                    getResources().getColor(R.color.grey));
            chart_statistic_Revenue.invalidate();
            return;
        }

        // Gom doanh thu theo ngày (giữ thứ tự)
        Map<String, Long> doanhThuTheoNgay = new LinkedHashMap<>();
        // Lấy tối đa 10 ngày gần nhất để chart gọn
        int maxDays = (currentFilter == FILTER_ALL || currentFilter == FILTER_30DAYS) ? 10 : 7;

        // Tạo danh sách ngày (từ xa đến gần)
        Calendar cal = Calendar.getInstance();
        List<String> ngayList = new ArrayList<>();
        if (currentFilter == FILTER_ALL) {
            // Lấy 10 ngày có đơn gần nhất
            for (DonDatDTO don : filtered) {
                if (!doanhThuTheoNgay.containsKey(don.getNgayDat())) {
                    doanhThuTheoNgay.put(don.getNgayDat(), 0L);
                }
            }
            // Giới hạn 10 key cuối
            List<String> keys = new ArrayList<>(doanhThuTheoNgay.keySet());
            int start = Math.max(0, keys.size() - maxDays);
            Map<String, Long> limited = new LinkedHashMap<>();
            for (int i = start; i < keys.size(); i++) limited.put(keys.get(i), 0L);
            doanhThuTheoNgay = limited;
        } else {
            // Tạo đủ ngày trong khoảng (kể cả ngày chưa có đơn → giá trị 0)
            int days = (currentFilter == FILTER_TODAY) ? 1 :
                       (currentFilter == FILTER_30DAYS) ? maxDays : maxDays;
            for (int i = days - 1; i >= 0; i--) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, -i);
                doanhThuTheoNgay.put(DB_FORMAT.format(c.getTime()), 0L);
            }
        }

        // Cộng doanh thu vào từng ngày
        for (DonDatDTO don : filtered) {
            if (doanhThuTheoNgay.containsKey(don.getNgayDat())) {
                try {
                    String sTien = don.getTongTien();
                    if(sTien != null && !sTien.isEmpty()){
                        long val = (long) Double.parseDouble(sTien);
                        doanhThuTheoNgay.put(don.getNgayDat(),
                                doanhThuTheoNgay.get(don.getNgayDat()) + val);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Tạo entries + nhãn ngày rút gọn (dd/MM)
        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] labels = new String[doanhThuTheoNgay.size()];
        int idx = 0;
        for (Map.Entry<String, Long> e : doanhThuTheoNgay.entrySet()) {
            entries.add(new BarEntry(idx, e.getValue()));
            // Chuyển "dd-MM-yyyy" → "dd/MM"
            String[] parts = e.getKey().split("-");
            labels[idx] = parts.length >= 2 ? parts[0] + "/" + parts[1] : e.getKey();
            idx++;
        }

        // Dataset
        int primaryColor = getResources().getColor(R.color.colorPrimary);
        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.stat_chart_title));
        dataSet.setColor(primaryColor);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value <= 0) return "";
                if (value >= 1000000) {
                    return String.format(Locale.US, "%.1ftr", value / 1000000f);
                } else if (value >= 1000) {
                    return String.format(Locale.US, "%.0fk", value / 1000f);
                }
                return String.format(Locale.US, "%.0f", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        // Cấu hình chart
        chart_statistic_Revenue.setData(barData);
        chart_statistic_Revenue.setDrawGridBackground(false);
        chart_statistic_Revenue.setDrawBorders(false);
        chart_statistic_Revenue.getDescription().setEnabled(false);
        chart_statistic_Revenue.getLegend().setEnabled(false);
        chart_statistic_Revenue.setTouchEnabled(false);
        chart_statistic_Revenue.setExtraBottomOffset(8f);

        // X-axis (trục ngang)
        XAxis xAxis = chart_statistic_Revenue.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setTextSize(9f);
        xAxis.setLabelRotationAngle(-45f); // Xoay nghiêng 45 độ cho dễ đọc
        xAxis.setLabelCount(Math.min(labels.length, 7), false); // Chỉ hiện tối đa 7 nhãn để tránh dính nhau

        // Y-axis trái (ẩn)
        YAxis leftAxis = chart_statistic_Revenue.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setTextSize(9f);

        // Y-axis phải (ẩn)
        chart_statistic_Revenue.getAxisRight().setEnabled(false);

        chart_statistic_Revenue.animateY(800);
        chart_statistic_Revenue.invalidate();
    }
}
