package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class KhoResponse {
    @SerializedName("danh_sach")
    private List<NguyenLieuResponse> danhSach;

    @SerializedName("tong_so")
    private int tongSo;

    @SerializedName("trang")
    private int trang;

    @SerializedName("moi_trang")
    private int moiTrang;

    @SerializedName("con_nua")
    private boolean conNua;

    @SerializedName("so_nguyen_lieu")
    private int soNguyenLieu;

    @SerializedName("gia_tri_ton")
    private double giaTriTon;

    @SerializedName("dem")
    private DemTon demTon;

    public static class DemTon {
        @SerializedName("am")
        private int am;

        @SerializedName("het")
        private int het;

        @SerializedName("sap_het")
        private int sapHet;

        @SerializedName("du")
        private int du;

        public int getAm() { return am; }
        public int getHet() { return het; }
        public int getSapHet() { return sapHet; }
        public int getDu() { return du; }
    }

    public List<NguyenLieuResponse> getDanhSach() { return danhSach; }
    public int getTongSo() { return tongSo; }
    public int getTrang() { return trang; }
    public int getMoiTrang() { return moiTrang; }
    public boolean isConNua() { return conNua; }
    public int getSoNguyenLieu() { return soNguyenLieu; }
    public double getGiaTriTon() { return giaTriTon; }
    public DemTon getDemTon() { return demTon; }
}
