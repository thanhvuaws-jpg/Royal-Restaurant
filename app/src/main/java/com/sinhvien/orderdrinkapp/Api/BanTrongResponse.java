package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Phản hồi của ban_trong.php — những bàn còn đặt được vào một giờ cụ thể.
 *
 * Chỉ có bàn trống và SỐ bàn đã kín, không có lịch của khách khác: khách
 * không cần biết ai đặt bàn nào lúc nào.
 */
public class BanTrongResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    /** Số phút một lượt đặt giữ bàn (hiện là 120). */
    @SerializedName("THOI_LUONG_PHUT")
    private int thoiLuongPhut;

    @SerializedName("SO_BAN_KIN")
    private int soBanKin;

    /** Mỗi phần tử có MABAN, TENBAN, LOAI_ANH, URL_ANH, URL_ANH_NHO. */
    @SerializedName("BAN")
    private List<TableResponse> ban;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public int getThoiLuongPhut() { return thoiLuongPhut; }
    public int getSoBanKin() { return soBanKin; }
    public List<TableResponse> getBan() { return ban != null ? ban : new ArrayList<>(); }
}
