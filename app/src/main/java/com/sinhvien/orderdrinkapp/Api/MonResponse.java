package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * MonResponse - Mô hình dữ liệu đại diện cho một Món ăn/Thức uống nhận từ API Server.
 * Chứa mã món, tên món, giá tiền, mã loại danh mục, tình trạng phục vụ và đường dẫn ảnh.
 */
public class MonResponse {
    // Mã định danh món ăn (MAMON)
    @SerializedName("MAMON")
    private int maMon;

    // Tên món ăn (TENMON), ví dụ: "Bạc xỉu đá"
    @SerializedName("TENMON")
    private String tenMon;

    // Đơn giá của món ăn (GIATIEN) lưu dưới dạng chuỗi
    @SerializedName("GIATIEN")
    private String giaTien;

    // Mã danh mục loại món ăn (MALOAI) liên kết
    @SerializedName("MALOAI")
    private int maLoai;

    // Tình trạng món ăn (TINHTRANG), ví dụ: "true" (còn món), "false" (hết món)
    @SerializedName("TINHTRANG")
    private String tinhTrang;

    // Liên kết/tên file ảnh (HINHANH) hiển thị minh họa cho món ăn
    @SerializedName("HINHANH")
    private String hinhAnh;

    public int getMaMon() { return maMon; }
    public String getTenMon() { return tenMon; }
    public String getGiaTien() { return giaTien; }
    public int getMaLoai() { return maLoai; }
    public String getTinhTrang() { return tinhTrang; }
    public String getHinhAnh() { return hinhAnh; }
}
