package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

public class LoaiMonResponse {
    @SerializedName("MALOAI")
    private int maLoai;

    @SerializedName("TENLOAI")
    private String tenLoai;

    @SerializedName("HINHANH")
    private String hinhAnh;

    public int getMaLoai() { return maLoai; }
    public String getTenLoai() { return tenLoai; }
    public String getHinhAnh() { return hinhAnh; }
}
