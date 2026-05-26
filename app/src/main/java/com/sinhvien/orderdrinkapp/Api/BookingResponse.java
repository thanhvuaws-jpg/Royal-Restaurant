package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

public class BookingResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("MADATBAN")
    private int maDatBan;

    @SerializedName("MAKH")
    private int maKH;

    @SerializedName("MABAN")
    private int maBan;

    @SerializedName("THOIGIANHEN")
    private String thoigianhen;

    @SerializedName("TINHTRANG")
    private String tinhtrang;

    @SerializedName("TENBAN")
    private String tenBan;

    @SerializedName("TONGTIEN")
    private String tongTien;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getMaDatBan() { return maDatBan; }
    public void setMaDatBan(int maDatBan) { this.maDatBan = maDatBan; }

    public int getMaKH() { return maKH; }
    public void setMaKH(int maKH) { this.maKH = maKH; }

    public int getMaBan() { return maBan; }
    public void setMaBan(int maBan) { this.maBan = maBan; }

    public String getThoigianhen() { return thoigianhen; }
    public void setThoigianhen(String thoigianhen) { this.thoigianhen = thoigianhen; }

    public String getTinhtrang() { return tinhtrang; }
    public void setTinhtrang(String tinhtrang) { this.tinhtrang = tinhtrang; }

    public String getTenBan() { return tenBan; }
    public void setTenBan(String tenBan) { this.tenBan = tenBan; }

    public String getTongTien() { return tongTien; }
    public void setTongTien(String tongTien) { this.tongTien = tongTien; }
}
