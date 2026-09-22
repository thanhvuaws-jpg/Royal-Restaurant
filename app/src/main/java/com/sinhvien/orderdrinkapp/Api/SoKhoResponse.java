package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SoKhoResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("nguyen_lieu")
    private NguyenLieuResponse nguyenLieu;

    @SerializedName("lich_su")
    private List<DongSoKho> lichSu;

    public static class DongSoKho {
        @SerializedName("MASO")
        private int maSo;

        @SerializedName("LOAI")
        private String loai;

        @SerializedName("THAYDOI")
        private double thayDoi;

        @SerializedName("TON_SAU")
        private double tonSau;

        @SerializedName("NGAYTAO")
        private String ngayTao;

        @SerializedName("GHICHU")
        private String ghiChu;

        @SerializedName("MAPHIEU")
        private String maPhieu;

        @SerializedName("LOAI_PHIEU")
        private String loaiPhieu;

        public int getMaSo() { return maSo; }
        public String getLoai() { return loai != null ? loai : ""; }
        public double getThayDoi() { return thayDoi; }
        public double getTonSau() { return tonSau; }
        public String getNgayTao() { return ngayTao != null ? ngayTao : ""; }
        public String getGhiChu() { return ghiChu != null ? ghiChu : ""; }
        public String getMaPhieu() { return maPhieu != null ? maPhieu : ""; }
        public String getLoaiPhieu() { return loaiPhieu != null ? loaiPhieu : ""; }
    }

    public String getStatus() { return status; }
    public NguyenLieuResponse getNguyenLieu() { return nguyenLieu; }
    public List<DongSoKho> getLichSu() { return lichSu; }
}
