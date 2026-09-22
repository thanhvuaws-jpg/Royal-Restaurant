package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

public class NguyenLieuResponse {
    @SerializedName("MANL")
    private int maNL;

    @SerializedName("TENNL")
    private String tenNL;

    @SerializedName("MANHOM")
    private int maNhom;

    @SerializedName("TENNHOM")
    private String tenNhom;

    @SerializedName("DONVI")
    private String donVi;

    @SerializedName("TON_HIENTAI")
    private double tonHienTai;

    @SerializedName("TON_TOITHIEU")
    private double tonToiThieu;

    @SerializedName("GIA_NHAP_GANNHAT")
    private double giaNhapGanNhat;

    @SerializedName("HINHANH")
    private String hinhAnh;

    @SerializedName("URL_ANH")
    private String urlAnh;

    @SerializedName("URL_ANH_NHO")
    private String urlAnhNho;

    @SerializedName("LOAI_ANH")
    private String loaiAnh;

    @SerializedName("TINHTRANG_TON")
    private String tinhTrangTon;

    @SerializedName("HOATDONG")
    private String hoatDong;

    public int getMaNL() { return maNL; }
    public String getTenNL() { return tenNL != null ? tenNL : ""; }
    public int getMaNhom() { return maNhom; }
    public String getTenNhom() { return tenNhom != null ? tenNhom : "Chưa phân nhóm"; }
    public String getDonVi() { return donVi != null ? donVi : ""; }
    public double getTonHienTai() { return tonHienTai; }
    public double getTonToiThieu() { return tonToiThieu; }
    public double getGiaNhapGanNhat() { return giaNhapGanNhat; }
    public String getHinhAnh() { return hinhAnh; }
    public String getUrlAnh() { return urlAnh; }
    public String getUrlAnhNho() { return (urlAnhNho != null && !urlAnhNho.isEmpty()) ? urlAnhNho : urlAnh; }
    public String getLoaiAnh() { return loaiAnh; }
    public String getTinhTrangTon() { return tinhTrangTon != null ? tinhTrangTon : "du"; }
    public boolean isHoatDong() { return "true".equalsIgnoreCase(hoatDong); }
}
