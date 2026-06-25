package com.sinhvien.orderdrinkapp.DTO;

/**
 * DonDatDTO - Lớp truyền tải dữ liệu (Data Transfer Object) cho Đơn đặt món (Order).
 * Chứa thông tin về đơn đặt, bao gồm bàn ăn, nhân viên thực hiện, tình trạng, ngày tạo, tổng tiền và phương thức thanh toán.
 */
public class DonDatDTO {

    // Mã số đơn đặt món (MADONDAT), Mã bàn (MABAN), Mã nhân viên (MANV)
    int MaDonDat, MaBan, MaNV;
    // Tình trạng đơn, Ngày đặt, Tổng tiền, Tên nhân viên, Tên bàn, Phương thức thanh toán (phuongThucTT)
    String TinhTrang, NgayDat, TongTien, TenNV, TenBan, phuongThucTT;

    public int getMaDonDat() {
        return MaDonDat;
    }

    public void setMaDonDat(int maDonDat) {
        MaDonDat = maDonDat;
    }

    public int getMaBan() {
        return MaBan;
    }

    public void setMaBan(int maBan) {
        MaBan = maBan;
    }

    public int getMaNV() {
        return MaNV;
    }

    public void setMaNV(int maNV) {
        MaNV = maNV;
    }

    public String getTinhTrang() {
        return TinhTrang;
    }

    public void setTinhTrang(String tinhTrang) {
        TinhTrang = tinhTrang;
    }

    public String getNgayDat() {
        return NgayDat;
    }

    public void setNgayDat(String ngayDat) {
        NgayDat = ngayDat;
    }

    public String getTongTien() {
        return TongTien;
    }

    public void setTongTien(String tongTien) {
        TongTien = tongTien;
    }

    public String getTenNV() { return TenNV; }
    public void setTenNV(String tenNV) { TenNV = tenNV; }
    public String getTenBan() { return TenBan; }
    public void setTenBan(String tenBan) { TenBan = tenBan; }
    public String getPhuongThucTT() { return phuongThucTT; }
    public void setPhuongThucTT(String phuongThucTT) { this.phuongThucTT = phuongThucTT; }
}
