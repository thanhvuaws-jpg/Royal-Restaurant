package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;

public class StaffResponse {
    @SerializedName("MANV")
    private int maNV;

    @SerializedName("HOTENNV")
    private String hoTenNV;

    @SerializedName("TENDN")
    private String tenDN;

    @SerializedName("MATKHAU")
    private String matKhau;

    @SerializedName("EMAIL")
    private String email;

    @SerializedName("SDT")
    private String sdt;

    @SerializedName("GIOITINH")
    private String gioiTinh;

    @SerializedName("NGAYSINH")
    private String ngaySinh;

    @SerializedName("MAQUYEN")
    private int maQuyen;

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("TOKEN")
    private String token;

    public int getMaNV() { return maNV; }
    public String getHoTenNV() { return hoTenNV; }
    public String getTenDN() { return tenDN; }
    public String getMatKhau() { return matKhau; }
    public String getEmail() { return email; }
    public String getSdt() { return sdt; }
    public String getGioiTinh() { return gioiTinh; }
    public String getNgaySinh() { return ngaySinh; }
    public int getMaQuyen() { return maQuyen; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
}
