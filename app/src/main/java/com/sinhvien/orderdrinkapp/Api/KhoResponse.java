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

    /**
     * Số lô nhập đã / sắp hết hạn (kho_dem_canh_bao_han). Bản đầu không khai
     * trường này, nên thẻ "Cảnh báo hạn" trên màn Kho luôn hiện 0.
     */
    @SerializedName("canh_bao_han")
    private CanhBaoHan canhBaoHan;

    public static class CanhBaoHan {
        @SerializedName("het_han")
        private int hetHan;

        @SerializedName("sap_het_han")
        private int sapHetHan;

        public int getHetHan() { return hetHan; }
        public int getSapHetHan() { return sapHetHan; }
    }

    public CanhBaoHan getCanhBaoHan() { return canhBaoHan; }

    public List<NguyenLieuResponse> getDanhSach() { return danhSach; }
    public int getTongSo() { return tongSo; }
    public int getTrang() { return trang; }
    public int getMoiTrang() { return moiTrang; }
    public boolean isConNua() { return conNua; }
    public int getSoNguyenLieu() { return soNguyenLieu; }
    public double getGiaTriTon() { return giaTriTon; }
    public DemTon getDemTon() { return demTon; }
}
