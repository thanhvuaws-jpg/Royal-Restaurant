package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

public class TableResponse {
    @SerializedName("MABAN")
    private int maBan;

    @SerializedName("TENBAN")
    private String tenBan;

    @SerializedName("TINHTRANG")
    private String tinhTrang;

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    public int getMaBan() { return maBan; }
    public void setMaBan(int maBan) { this.maBan = maBan; }
    
    public String getTenBan() { return tenBan; }
    public void setTenBan(String tenBan) { this.tenBan = tenBan; }

    public String getTinhTrang() { return tinhTrang; }
    public void setTinhTrang(String tinhTrang) { this.tinhTrang = tinhTrang; }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
