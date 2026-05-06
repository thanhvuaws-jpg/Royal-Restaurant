package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

public class StatisticResponse {
    @SerializedName("ngay")
    private String ngay;

    @SerializedName("doanhthu")
    private long doanhThu;

    public String getNgay() {
        return ngay;
    }

    public long getDoanhThu() {
        return doanhThu;
    }
}
