package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * OrderDetailResponse - Mô hình dữ liệu đại diện cho Chi tiết dòng món ăn trong Đơn đặt (OrderDetail).
 * Lưu thông tin mã đơn đặt, mã món, tên món, đơn giá, số lượng và ảnh minh họa để dựng danh sách món trong đơn hàng.
 */
public class OrderDetailResponse {
    // Mã đơn đặt món (MADONDAT) sở hữu món này
    @SerializedName("MADONDAT")
    private int maDonDat;

    // Mã món ăn (MAMON)
    @SerializedName("MAMON")
    private int maMon;

    // Tên món ăn (TENMON) để hiển thị lên hóa đơn
    @SerializedName("TENMON")
    private String tenMon;

    // Đơn giá món ăn (GIATIEN) tại thời điểm gọi món
    @SerializedName("GIATIEN")
    private long giaTien;

    // Số lượng đĩa/ly món ăn (SOLUONG) được gọi
    @SerializedName("SOLUONG")
    private int soLuong;

    // Hình ảnh minh họa (HINHANH) của món ăn
    @SerializedName("HINHANH")
    private String hinhAnh;

    public int getMaDonDat() { return maDonDat; }
    public int getMaMon() { return maMon; }
    public String getTenMon() { return tenMon; }
    public long getGiaTien() { return giaTien; }
    public int getSoLuong() { return soLuong; }
    public String getHinhAnh() { return hinhAnh; }
}
