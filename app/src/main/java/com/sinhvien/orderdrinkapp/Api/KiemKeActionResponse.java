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

    /** Số nguyên liệu đã đếm (chỉ có ở bước xem trước). */
    @SerializedName("so_dem")
    private int soDem;

    /** Có dòng lệch từ một nửa tồn sổ trở lên — gần như chắc là gõ nhầm. */
    @SerializedName("co_lech_lon")
    private boolean coLechLon;

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

        @SerializedName("lech_lon")
        private boolean lechLon;

        @SerializedName("goi_y")
        private String goiY;

        public int getMaNL() { return maNL; }
        public String getTenNL() { return tenNL != null ? tenNL : ""; }
        public String getDonVi() { return donVi != null ? donVi : ""; }
        public double getTonSo() { return tonSo; }
        public double getTonThucTe() { return tonThucTe; }
        public double getChenhLech() { return chenhLech; }
        public boolean isLechLon() { return lechLon; }
        public String getGoiY() { return goiY != null ? goiY : ""; }
    }

    public String getStatus() { return status; }
    public String getMessage() { return message != null ? message : ""; }
    public int getSoLech() { return soLech; }
    public int getSoDem() { return soDem; }
    public boolean isCoLechLon() { return coLechLon; }
    public List<DongKiemKeLech> getChiTiet() { return chiTiet; }
}
