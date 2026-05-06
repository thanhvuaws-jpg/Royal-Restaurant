package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

public class OrderDetailResponse {
    @SerializedName("MADONDAT")
    private int maDonDat;

    @SerializedName("MAMON")
    private int maMon;

    @SerializedName("TENMON")
    private String tenMon;

    @SerializedName("GIATIEN")
    private long giaTien;

    @SerializedName("SOLUONG")
    private int soLuong;

    @SerializedName("HINHANH")
    private String hinhAnh;

    public int getMaDonDat() { return maDonDat; }
    public int getMaMon() { return maMon; }
    public String getTenMon() { return tenMon; }
    public long getGiaTien() { return giaTien; }
    public int getSoLuong() { return soLuong; }
    public String getHinhAnh() { return hinhAnh; }
}
