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
    public String getTenBan() { return tenBan; }
    public String getTinhTrang() { return tinhTrang; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
