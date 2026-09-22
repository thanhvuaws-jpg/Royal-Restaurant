package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BestsellerResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("danh_sach")
    private List<BestsellerItem> danhSach;

    public static class BestsellerItem {
        @SerializedName("MAMON")
        private int maMon;

        @SerializedName("TENMON")
        private String tenMon;

        @SerializedName("GIATIEN")
        private double giaTien;

        @SerializedName("HINHANH")
        private String hinhAnh;

        @SerializedName("MALOAI")
        private int maLoai;

        @SerializedName("TENLOAI")
        private String tenLoai;

        @SerializedName("SO_PHAN")
        private int soPhan;

        @SerializedName("TB_MOI_NGAY")
        private double tbMoiNgay;

        @SerializedName("XU_HUONG")
        private String xuHuong;

        public int getMaMon() { return maMon; }
        public String getTenMon() { return tenMon != null ? tenMon : ""; }
        public double getGiaTien() { return giaTien; }
        public String getHinhAnh() { return hinhAnh; }
        public int getMaLoai() { return maLoai; }
        public String getTenLoai() { return tenLoai != null ? tenLoai : ""; }
        public int getSoPhan() { return soPhan; }
        public double getTbMoiNgay() { return tbMoiNgay; }
        public String getXuHuong() { return xuHuong != null ? xuHuong : "on_dinh"; }
    }

    public String getStatus() { return status; }
    public List<BestsellerItem> getDanhSach() { return danhSach; }
}
