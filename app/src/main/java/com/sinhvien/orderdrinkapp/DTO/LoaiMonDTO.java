package com.sinhvien.orderdrinkapp.DTO;
 
public class LoaiMonDTO {
 
    int MaLoai;
    String TenLoai;
    byte[] HinhAnh;
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
