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
import androidx.lifecycle.ViewModelProvider;
import com.sinhvien.orderdrinkapp.ViewModel.StatisticViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DisplayStatisticFragment - Màn hình Báo cáo Thống kê doanh thu (Statistic Dashboard).
 * Dành riêng cho Admin/Quản lý để theo dõi tình hình kinh doanh của nhà hàng.
 * - Hỗ trợ các bộ lọc nhanh (ChipGroup):
 *   + Hôm nay
 *   + 7 ngày gần đây
 *   + 30 ngày gần đây
 *   + Tất cả thời gian
 * - Tích hợp thư viện MPAndroidChart vẽ biểu đồ cột (BarChart) doanh thu trực quan,
 *   tự động quy đổi đơn vị hiển thị (ví dụ: tr cho triệu đồng, k cho nghìn đồng).
 * - Hiển thị 3 thẻ tóm tắt doanh số: Tổng doanh thu, Số lượng đơn hàng, Giá trị trung bình/đơn.
 * - Danh sách RecyclerView liệt kê chi tiết các hóa đơn thỏa mãn điều kiện lọc.
 * - Sử dụng StatisticViewModel để nạp và cache dữ liệu từ API VPS/SQLite nội bộ.
 */
public class DisplayStatisticFragment extends Fragment {

    // Các thành phần giao diện hiển thị báo cáo
    RecyclerView rv_statistic_OrderList;
    BarChart chart_statistic_Revenue;
    TextView txt_statistic_TotalRevenue, txt_statistic_OrderCount, txt_statistic_AvgOrder;
    ChipGroup chipGroup_statistic_Filter;

    // Danh sách lưu trữ hóa đơn thu thập được
    List<DonDatDTO> tatCaDonDat; 
    AdapterDisplayStatistic adapterDisplayStatistic;
    View view;
    // ViewModel xử lý số liệu thống kê
    private StatisticViewModel statisticViewModel;
    // Dialog loading dữ liệu
    private androidx.appcompat.app.AlertDialog progressDialog;

    // Các mốc bộ lọc thống kê thời gian
    private static final int FILTER_TODAY   = 0;
    private static final int FILTER_7DAYS   = 7;
    private static final int FILTER_30DAYS  = 30;
    private static final int FILTER_ALL     = -1;
    private int currentFilter = FILTER_7DAYS;

    // Định dạng ngày lưu trữ trong Database
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

        // Ánh xạ các view từ XML layout
        rv_statistic_OrderList      = view.findViewById(R.id.rv_statistic_OrderList);
        rv_statistic_OrderList.setLayoutManager(new LinearLayoutManager(getActivity()));
        chart_statistic_Revenue     = view.findViewById(R.id.chart_statistic_Revenue);
        txt_statistic_TotalRevenue  = view.findViewById(R.id.txt_statistic_TotalRevenue);
        txt_statistic_OrderCount    = view.findViewById(R.id.txt_statistic_OrderCount);
        txt_statistic_AvgOrder      = view.findViewById(R.id.txt_statistic_AvgOrder);
        chipGroup_statistic_Filter  = view.findViewById(R.id.chipGroup_statistic_Filter);

        tatCaDonDat = new ArrayList<>(); 

        // Khởi tạo ViewModel và đăng ký lắng nghe cập nhật LiveData
        statisticViewModel = new ViewModelProvider(this).get(StatisticViewModel.class);
        statisticViewModel.getStatisticOrders().observe(getViewLifecycleOwner(), list -> {
            tatCaDonDat.clear();
            tatCaDonDat.addAll(list);
            capNhatDashboard();
        });
        statisticViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                if (progressDialog == null && getActivity() != null) {
                    progressDialog = com.sinhvien.orderdrinkapp.Utils.DialogHelper.getLoadingDialog(getActivity(), "Đang tải dữ liệu...");
                }
                if (progressDialog != null && !progressDialog.isShowing()) {
                    progressDialog.show();
                }
            } else {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        });
        statisticViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getActivity(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý sự kiện chuyển mốc thời gian lọc (Chips)
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
            statisticViewModel.setCurrentFilter(currentFilter);
            HienThiDSThongKe(true);
        });

        // Mặc định chọn mốc lọc "7 ngày" lúc khởi chạy
        ((Chip) view.findViewById(R.id.chip_filter_7Days)).setChecked(true);
        
        HienThiDSThongKe(false);

        return view;
    }

    public static void clearCache() {
        // ViewModel quản lý vòng đời dữ liệu nên không cần cache tĩnh
    }

    private void HienThiDSThongKe(){
        HienThiDSThongKe(false);
    }

    /**
     * Gọi ViewModel lấy danh sách hóa đơn theo mốc thời gian lọc.
     */
    private void HienThiDSThongKe(boolean forceRefresh) {
        statisticViewModel.fetchPaidOrders(forceRefresh);
    }

    /** 
     * Tính toán tổng doanh thu, số đơn hàng, đơn giá trung bình và gán dữ liệu lên biểu đồ cột + RecyclerView.
     */
    private void capNhatDashboard() {
        List<DonDatDTO> filtered = tatCaDonDat; 

        // Tính toán các thông số thống kê cơ bản
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

        // Gán văn bản hiển thị lên các CardView tổng quan
        txt_statistic_TotalRevenue.setText(
                String.format("%,d", tongDoanhThu) + " " +
                        getString(R.string.currency_vnd));
        txt_statistic_OrderCount.setText(String.valueOf(soDon));
        txt_statistic_AvgOrder.setText(
                String.format("%,d", trungBinh) + " " +
                        getString(R.string.currency_vnd));

        // Cập nhật RecyclerView danh sách hóa đơn
        if (adapterDisplayStatistic == null) {
            adapterDisplayStatistic = new AdapterDisplayStatistic(getActivity(), filtered);
            rv_statistic_OrderList.setAdapter(adapterDisplayStatistic);
            
            // Click chọn hóa đơn mở rộng xem thông tin chi tiết các món ăn đã gọi
            adapterDisplayStatistic.setOnItemClickListener(position -> {
                List<DonDatDTO> currentFiltered = tatCaDonDat;
                if (position >= currentFiltered.size()) return;
                DonDatDTO don = currentFiltered.get(position);
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
        } else {
            adapterDisplayStatistic.updateData(filtered);
        }

        // Cập nhật dữ liệu và hiển thị BarChart
        setupBarChart(filtered);
    }

    /**
     * Cấu hình và vẽ biểu đồ cột doanh thu theo ngày bằng thư viện MPAndroidChart.
     */
    private void setupBarChart(List<DonDatDTO> filtered) {
        if (filtered.isEmpty()) {
            chart_statistic_Revenue.clear();
            chart_statistic_Revenue.setNoDataText(getString(R.string.stat_no_data));
            chart_statistic_Revenue.setNoDataTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.grey));
            chart_statistic_Revenue.invalidate();
            return;
        }

        // Gom nhóm doanh thu theo ngày
        Map<String, Long> doanhThuTheoNgay = new LinkedHashMap<>();
        int maxDays = (currentFilter == FILTER_ALL || currentFilter == FILTER_30DAYS) ? 10 : 7;

        // Khởi tạo trục thời gian từ xa đến gần
        Calendar cal = Calendar.getInstance();
        if (currentFilter == FILTER_ALL) {
            for (DonDatDTO don : filtered) {
                if (!doanhThuTheoNgay.containsKey(don.getNgayDat())) {
                    doanhThuTheoNgay.put(don.getNgayDat(), 0L);
                }
            }
            List<String> keys = new ArrayList<>(doanhThuTheoNgay.keySet());
            int start = Math.max(0, keys.size() - maxDays);
            Map<String, Long> limited = new LinkedHashMap<>();
            for (int i = start; i < keys.size(); i++) limited.put(keys.get(i), 0L);
            doanhThuTheoNgay = limited;
        } else {
            int days = (currentFilter == FILTER_TODAY) ? 1 :
                       (currentFilter == FILTER_30DAYS) ? maxDays : maxDays;
            for (int i = days - 1; i >= 0; i--) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, -i);
                doanhThuTheoNgay.put(DB_FORMAT.format(c.getTime()), 0L);
            }
        }

        // Tính tổng tiền hóa đơn cho mỗi ngày tương ứng
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

        // Chuẩn bị dữ liệu BarEntry và nhãn hiển thị trục X (Rút gọn về dd/MM)
        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] labels = new String[doanhThuTheoNgay.size()];
        int idx = 0;
        for (Map.Entry<String, Long> e : doanhThuTheoNgay.entrySet()) {
            entries.add(new BarEntry(idx, e.getValue()));
            String[] parts = e.getKey().split("-");
            labels[idx] = parts.length >= 2 ? parts[0] + "/" + parts[1] : e.getKey();
            idx++;
        }

        // Định dạng màu sắc cột, màu chữ và nhãn cột biểu đồ
        int primaryColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.stat_chart_title));
        dataSet.setColor(primaryColor);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(9f);
        
        // Quy đổi giá trị hiển thị trên đỉnh cột gọn gàng (tr cho triệu, k cho nghìn)
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

        // Thiết lập cấu hình tổng quan biểu đồ BarChart
        chart_statistic_Revenue.setData(barData);
        chart_statistic_Revenue.setDrawGridBackground(false);
        chart_statistic_Revenue.setDrawBorders(false);
        chart_statistic_Revenue.getDescription().setEnabled(false);
        chart_statistic_Revenue.getLegend().setEnabled(false);
        chart_statistic_Revenue.setTouchEnabled(false);
        chart_statistic_Revenue.setExtraBottomOffset(8f);

        // Định dạng trục X (Trục Hoành)
        XAxis xAxis = chart_statistic_Revenue.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setTextSize(9f);
        xAxis.setLabelRotationAngle(-45f); // Xoay nghiêng 45 độ tránh đè nhãn lên nhau
        xAxis.setLabelCount(Math.min(labels.length, 7), false); 

        // Định dạng trục Y bên trái
        YAxis leftAxis = chart_statistic_Revenue.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setTextSize(9f);

        // Ẩn trục Y bên phải để biểu đồ gọn gàng hơn
        chart_statistic_Revenue.getAxisRight().setEnabled(false);

        // Hiệu ứng hoạt hình vẽ biểu đồ chạy từ dưới lên trong 800ms
        chart_statistic_Revenue.animateY(800);
        chart_statistic_Revenue.invalidate();
    }
}
