package com.sinhvien.orderdrinkapp.DTO;

public class BanAnDTO {
    int MaBan;
    String TenBan;
    String TinhTrang; // Thêm biến này để lưu trạng thái Trống/Có người

    public int getMaBan() {
        return MaBan;
    }

    public void setMaBan(int maBan) {
        MaBan = maBan;
    }

    public String getTenBan() {
        return TenBan;
    }

    public void setTenBan(String tenBan) {
        TenBan = tenBan;
    }

    public String getTinhTrang() {
        return TinhTrang;
    }

    public void setTinhTrang(String tinhTrang) {
        TinhTrang = tinhTrang;
    }
}
