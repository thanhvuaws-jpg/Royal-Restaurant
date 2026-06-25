package com.sinhvien.orderdrinkapp.DTO;

/**
 * ThanhToanDTO - Lớp truyền tải dữ liệu phục vụ màn hình Thanh toán (Billing/Checkout).
 * Lưu thông tin dòng hóa đơn bao gồm tên món, số lượng, đơn giá và hình ảnh của món ăn để hiển thị chi tiết hóa đơn.
 */
public class ThanhToanDTO {
    // Tên món ăn cần thanh toán
    String TenMon;
    // Số lượng món và Đơn giá (đơn vị: VNĐ)
    int SoLuong, GiaTien;
    // Mảng Byte chứa dữ liệu hình ảnh (nếu tải từ SQLite)
    byte[] HinhAnh;
    // Đường dẫn/URL hình ảnh món ăn trên Server VPS (dùng hiển thị online)
    String HinhAnhPath;

    public String getHinhAnhPath() {
        return HinhAnhPath;
    }

    public void setHinhAnhPath(String hinhAnhPath) {
        HinhAnhPath = hinhAnhPath;
    }

    public String getTenMon() {
        return TenMon;
    }

    public void setTenMon(String tenMon) {
        TenMon = tenMon;
    }

    public int getSoLuong() {
        return SoLuong;
    }

    public void setSoLuong(int soLuong) {
        SoLuong = soLuong;
    }

    public int getGiaTien() {
        return GiaTien;
    }

    public void setGiaTien(int giaTien) {
        GiaTien = giaTien;
    }

    public byte[] getHinhAnh() {
        return HinhAnh;
    }

    public void setHinhAnh(byte[] hinhAnh) {
        HinhAnh = hinhAnh;
    }

    public void setHinhAnhUrl(String url) {
        this.HinhAnhPath = url;
    }
}
