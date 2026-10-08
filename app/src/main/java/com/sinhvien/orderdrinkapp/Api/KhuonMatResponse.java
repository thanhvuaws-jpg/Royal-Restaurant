package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/** Phản hồi của api/khuon_mat.php khi bật đăng nhập bằng khuôn mặt (QĐ-103). */
public class KhuonMatResponse {
    @SerializedName("status")
    private String status;
    @SerializedName("message")
    private String message;
    @SerializedName("MAKHOA")
    private int maKhoa;
    /** Khóa thiết bị — máy chủ chỉ trả ĐÚNG MỘT LẦN, lúc bật. */
    @SerializedName("KHOA")
    private String khoa;
    @SerializedName("MANV")
    private int maNV;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public int getMaKhoa() { return maKhoa; }
    public String getKhoa() { return khoa; }
    public int getMaNV() { return maNV; }
}
