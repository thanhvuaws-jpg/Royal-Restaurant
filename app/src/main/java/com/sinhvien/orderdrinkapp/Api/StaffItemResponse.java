package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

public class StaffItemResponse {
    @SerializedName("MANV")
    private int maNV;

    @SerializedName("HOTENNV")
    private String hoTen;

    @SerializedName("TENDN")
    private String tenDN;

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

    @SerializedName("TENQUYEN")
    private String tenQuyen;

    public int getMaNV() { return maNV; }
    public String getHoTen() { return hoTen; }
    public String getTenDN() { return tenDN; }
    public String getEmail() { return email; }
    public String getSdt() { return sdt; }
    public String getGioiTinh() { return gioiTinh; }
    public String getNgaySinh() { return ngaySinh; }
    public int getMaQuyen() { return maQuyen; }
    public String getTenQuyen() { return tenQuyen; }
}
