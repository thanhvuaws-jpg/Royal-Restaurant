package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

public class PhieuKhoActionResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("phieu")
    private PhieuData phieu;

    public static class PhieuData {
        @SerializedName("so_phieu")
        private String soPhieu;

        @SerializedName("maphieunhap")
        private Integer maPhieuNhap;

        @SerializedName("maphieuxuat")
        private Integer maPhieuXuat;

        @SerializedName("maphieuhuy")
        private Integer maPhieuHuy;

        @SerializedName("hinhanh")
        private String hinhAnh;

        public String getSoPhieu() { return soPhieu; }
        public Integer getMaPhieuNhap() { return maPhieuNhap; }
        public Integer getMaPhieuXuat() { return maPhieuXuat; }
        public Integer getMaPhieuHuy() { return maPhieuHuy; }
        public String getHinhAnh() { return hinhAnh; }
    }

    public String getStatus() { return status; }
    public String getMessage() { return message != null ? message : ""; }
    public PhieuData getPhieu() { return phieu; }
}
