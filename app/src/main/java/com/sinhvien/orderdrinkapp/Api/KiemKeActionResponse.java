package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class KiemKeActionResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("so_lech")
    private int soLech;

    @SerializedName("chi_tiet")
    private List<DongKiemKeLech> chiTiet;

    public static class DongKiemKeLech {
        @SerializedName("manl")
        private int maNL;

        @SerializedName("tennl")
        private String tenNL;

        @SerializedName("donvi")
        private String donVi;

        @SerializedName("ton_so")
        private double tonSo;

        @SerializedName("ton_thucte")
        private double tonThucTe;

        @SerializedName("chenh_lech")
        private double chenhLech;

        public int getMaNL() { return maNL; }
        public String getTenNL() { return tenNL != null ? tenNL : ""; }
        public String getDonVi() { return donVi != null ? donVi : ""; }
        public double getTonSo() { return tonSo; }
        public double getTonThucTe() { return tonThucTe; }
        public double getChenhLech() { return chenhLech; }
    }

    public String getStatus() { return status; }
    public String getMessage() { return message != null ? message : ""; }
    public int getSoLech() { return soLech; }
    public List<DongKiemKeLech> getChiTiet() { return chiTiet; }
}
