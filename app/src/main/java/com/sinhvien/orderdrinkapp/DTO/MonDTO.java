package com.sinhvien.orderdrinkapp.DTO;

public class MonDTO {

    int MaMon, MaLoai;
    String TenMon,GiaTien,TinhTrang;
    byte[] HinhAnh;

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

    String hinhAnhUrl;
    public String getHinhAnhUrl() {
        return hinhAnhUrl;
    }
    public void setHinhAnhUrl(String hinhAnhUrl) {
        this.hinhAnhUrl = hinhAnhUrl;
    }

}
