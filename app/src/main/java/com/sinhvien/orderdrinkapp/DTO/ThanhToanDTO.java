package com.sinhvien.orderdrinkapp.DTO;

public class ThanhToanDTO {
    String TenMon;
    int SoLuong, GiaTien;
    byte[] HinhAnh;
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
