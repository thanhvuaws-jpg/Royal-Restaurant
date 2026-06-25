package com.sinhvien.orderdrinkapp.DTO;
 
/**
 * LoaiMonDTO - Lớp truyền tải dữ liệu cho Danh mục món ăn (Category).
 * Ánh xạ thông tin mã loại, tên loại và đường dẫn hình ảnh giữa SQLite, API và Adapter hiển thị.
 */
public class LoaiMonDTO {
 
    // Mã loại danh mục (MALOAI)
    int MaLoai;
    // Tên của danh mục (TENLOAI), ví dụ: "Món khai vị"
    String TenLoai;
    // Mảng Byte chứa dữ liệu hình ảnh (nếu lưu dạng BLOB cục bộ)
    byte[] HinhAnh;
    // Đường dẫn ảnh (HINHANH) lưu dạng chuỗi liên kết/tên file ảnh trên VPS
    String hinhAnhUrl; 
 
    public int getMaLoai() {
        return MaLoai;
    }
 
    public void setMaLoai(int maLoai) {
        MaLoai = maLoai;
    }
 
    public String getTenLoai() {
        return TenLoai;
    }
 
    public void setTenLoai(String tenLoai) {
        TenLoai = tenLoai;
    }
 
    public byte[] getHinhAnh() {
        return HinhAnh;
    }
 
    public void setHinhAnh(byte[] hinhAnh) {
        HinhAnh = hinhAnh;
    }
 
    public String getHinhAnhPath() {
        return hinhAnhUrl;
    }
 
    public void setHinhAnhPath(String hinhAnhPath) {
        this.hinhAnhUrl = hinhAnhPath;
    }
}
