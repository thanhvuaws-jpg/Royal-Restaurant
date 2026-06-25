package com.sinhvien.orderdrinkapp.DTO;

/**
 * MonDTO - Lớp truyền tải dữ liệu món ăn/thức uống (Dish).
 * Ánh xạ dữ liệu món ăn từ SQLite cục bộ, API và các Adapter danh sách hiển thị món ăn của nhà hàng.
 */
public class MonDTO {

    // Mã món ăn (MAMON), Mã danh mục loại món ăn (MALOAI) liên kết
    int MaMon, MaLoai;
    // Tên món, đơn giá (lưu dạng String), tình trạng món ("true" - còn món, "false" - hết món)
    String TenMon, GiaTien, TinhTrang;
    // Mảng Byte chứa dữ liệu hình ảnh (nếu lưu SQLite dạng BLOB)
    byte[] HinhAnh;
    // URL/Đường dẫn hình ảnh của món ăn lưu trữ trên server VPS
    String hinhAnhUrl;

    public int getMaMon() {
        return MaMon;
    }

    public void setMaMon(int maMon) {
        MaMon = maMon;
    }

    public int getMaLoai() {
        return MaLoai;
    }

    public void setMaLoai(int maLoai) {
        MaLoai = maLoai;
    }

    public String getTenMon() {
        return TenMon;
    }

    public void setTenMon(String tenMon) {
        TenMon = tenMon;
    }

    public String getGiaTien() {
        return GiaTien;
    }

    public void setGiaTien(String giaTien) {
        GiaTien = giaTien;
    }
    public String getTinhTrang() {
        return TinhTrang;
    }
    public void setTinhTrang(String tinhTrang) {
        TinhTrang = tinhTrang;
    }

    public byte[] getHinhAnh() {
        return HinhAnh;
    }

    public void setHinhAnh(byte[] hinhAnh) {
        HinhAnh = hinhAnh;
    }

    public String getHinhAnhUrl() {
        return hinhAnhUrl;
    }
    public void setHinhAnhUrl(String hinhAnhUrl) {
        this.hinhAnhUrl = hinhAnhUrl;
    }
}
