package com.sinhvien.orderdrinkapp.DTO;

public class ChiTietDonDatDTO {

    int MaDonDat, MaMon, SoLuong;
    String GhiChu;

    public int getMaDonDat() {
        return MaDonDat;
    }

    public void setMaDonDat(int maDonDat) {
        MaDonDat = maDonDat;
    }

    public int getMaMon() {
        return MaMon;
    }

    public void setMaMon(int maMon) {
        MaMon = maMon;
    }

    public int getSoLuong() {
        return SoLuong;
    }

    public void setSoLuong(int soLuong) {
        SoLuong = soLuong;
    }

    public String getGhiChu() {
        return GhiChu;
    }

    public void setGhiChu(String ghiChu) {
        GhiChu = ghiChu;
    }
}
