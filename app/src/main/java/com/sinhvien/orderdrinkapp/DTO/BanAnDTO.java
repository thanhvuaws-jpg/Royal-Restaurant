package com.sinhvien.orderdrinkapp.DTO;

/**
 * BanAnDTO - Lớp truyền tải dữ liệu (Data Transfer Object) cho Bàn ăn.
 * Dùng để ánh xạ dữ liệu bàn ăn giữa SQLite cục bộ, API và tầng hiển thị UI.
 */
public class BanAnDTO {
    // Mã số bàn ăn (MABAN)
    int MaBan;
    // Tên bàn ăn (TENBAN)
    String TenBan;
    // Trạng thái bàn ăn (TINHTRANG): "true" (Đang hoạt động/Có khách), "false" (Trống)
    String TinhTrang;

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
