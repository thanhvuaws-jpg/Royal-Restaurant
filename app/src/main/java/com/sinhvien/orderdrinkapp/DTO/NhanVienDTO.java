package com.sinhvien.orderdrinkapp.DTO;

/**
 * NhanVienDTO - Lớp truyền tải dữ liệu Nhân viên hoặc Khách hàng thành viên (Staff/Customer).
 * Chứa các thông tin cơ bản: Mã, Tên, Tên đăng nhập, Mật khẩu, Email, SĐT, Giới tính, Ngày sinh và Quyền hạn.
 */
public class NhanVienDTO {

    // Các trường thông tin cá nhân của nhân viên
    String HOTENNV, TENDN, MATKHAU, EMAIL, SDT, GIOITINH, NGAYSINH;
    // Mã nhân viên (MANV) và mã quyền hạn (MAQUYEN)
    int MANV, MAQUYEN;

    public int getMAQUYEN() {
        return MAQUYEN;
    }

    public void setMAQUYEN(int MAQUYEN) {
        this.MAQUYEN = MAQUYEN;
    }

    public int getMANV() {
        return MANV;
    }

    public void setMANV(int MANV) {
        this.MANV = MANV;
    }

    public String getHOTENNV() {
        return HOTENNV;
    }

    public void setHOTENNV(String HOTENNV) {
        this.HOTENNV = HOTENNV;
    }

    public String getTENDN() {
        return TENDN;
    }

    public void setTENDN(String TENDN) {
        this.TENDN = TENDN;
    }

    public String getMATKHAU() {
        return MATKHAU;
    }

    public void setMATKHAU(String MATKHAU) {
        this.MATKHAU = MATKHAU;
    }

    public String getEMAIL() {
        return EMAIL;
    }

    public void setEMAIL(String EMAIL) {
        this.EMAIL = EMAIL;
    }

    public String getSDT() {
        return SDT;
    }

    public void setSDT(String SDT) {
        this.SDT = SDT;
    }

    public String getGIOITINH() {
        return GIOITINH;
    }

    public void setGIOITINH(String GIOITINH) {
        this.GIOITINH = GIOITINH;
    }

    public String getNGAYSINH() {
        return NGAYSINH;
    }

    public void setNGAYSINH(String NGAYSINH) {
        this.NGAYSINH = NGAYSINH;
    }
}
