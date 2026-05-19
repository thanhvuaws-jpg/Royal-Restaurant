package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

public class OrderResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("tinhtrang")
    private String tinhTrang;

    @SerializedName("phuongthuc")
    private String phuongThuc;

    @SerializedName("MADONDAT")
    private int maDonDat;

    @SerializedName("MANV")
    private int maNV;

    @SerializedName("MABAN")
    private int maBan;

    @SerializedName("NGAYDAT")
    private String ngayDat;

    @SerializedName("TONGTIEN")
    private String tongTien;

    @SerializedName("HOTENNV")
    private String hoTenNV;

    @SerializedName("TENBAN")
    private String tenBan;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMaDonDat() {
        return maDonDat;
    }

    public void setMaDonDat(int maDonDat) {
        this.maDonDat = maDonDat;
    }

    public int getMaNV() { return maNV; }
    public int getMaBan() { return maBan; }
    public String getNgayDat() { return ngayDat; }
    public String getTongTien() { return tongTien; }
    public String getHoTenNV() { return hoTenNV; }
    public String getTenBan() { return tenBan; }
    public String getTinhTrang() { return tinhTrang; }
    public String getPhuongThuc() { return phuongThuc; }
}
