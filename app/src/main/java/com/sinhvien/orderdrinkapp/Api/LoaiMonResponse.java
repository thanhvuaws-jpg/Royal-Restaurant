package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * LoaiMonResponse - Mô hình dữ liệu đại diện cho một Danh mục món ăn nhận từ API Server.
 * Chứa mã loại, tên loại và đường dẫn hình ảnh của danh mục.
 */
public class LoaiMonResponse {
    // Mã định danh danh mục (MALOAI) trên Server
    @SerializedName("MALOAI")
    private int maLoai;

    // Tên của danh mục (TENLOAI), ví dụ: "Cà phê", "Trà sữa"
    @SerializedName("TENLOAI")
    private String tenLoai;

    // Tên tệp ảnh hoặc liên kết ảnh (HINHANH) đại diện cho danh mục
    @SerializedName("HINHANH")
    private String hinhAnh;

    public int getMaLoai() { return maLoai; }
    public String getTenLoai() { return tenLoai; }
    public String getHinhAnh() { return hinhAnh; }
}
