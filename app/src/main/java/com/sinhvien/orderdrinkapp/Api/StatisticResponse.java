package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * StatisticResponse - Mô hình dữ liệu phản hồi báo cáo Thống kê doanh thu từ API.
 * Trả về thông tin doanh thu của từng ngày/tháng cụ thể.
 */
public class StatisticResponse {
    // Ngày/tháng thống kê doanh thu (định dạng String YYYY-MM-DD)
    @SerializedName("ngay")
    private String ngay;

    // Tổng doanh thu đạt được trong ngày/tháng tương ứng (đơn vị: VNĐ)
    @SerializedName("doanhthu")
    private long doanhThu;

    public String getNgay() {
        return ngay;
    }

    public long getDoanhThu() {
        return doanhThu;
    }
}
