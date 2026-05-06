package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

public class MonResponse {
    @SerializedName("MAMON")
    private int maMon;

    @SerializedName("TENMON")
    private String tenMon;

    @SerializedName("GIATIEN")
    private String giaTien;

    @SerializedName("MALOAI")
    private int maLoai;

    @SerializedName("TINHTRANG")
    private String tinhTrang;

    @SerializedName("HINHANH")
    private String hinhAnh;

    public int getMaMon() { return maMon; }
    public String getTenMon() { return tenMon; }
    public String getGiaTien() { return giaTien; }
    public int getMaLoai() { return maLoai; }
    public String getTinhTrang() { return tinhTrang; }
    public String getHinhAnh() { return hinhAnh; }
}
