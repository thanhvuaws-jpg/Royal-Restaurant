package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NhomNguyenLieuResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("danh_sach")
    private List<NhomItem> danhSach;

    public static class NhomItem {
        @SerializedName("MANHOM")
        private int maNhom;

        @SerializedName("TENNHOM")
        private String tenNhom;

        @SerializedName("THUTU")
        private int thuTu;

        @SerializedName("SO_NL")
        private int soNl;

        public NhomItem() {}

        public NhomItem(int maNhom, String tenNhom) {
            this.maNhom = maNhom;
            this.tenNhom = tenNhom;
        }

        public int getMaNhom() { return maNhom; }
        public String getTenNhom() { return tenNhom != null ? tenNhom : ""; }
        public int getThuTu() { return thuTu; }
        public int getSoNl() { return soNl; }

        @Override
        public String toString() {
            return getTenNhom();
        }
    }

    public String getStatus() { return status; }
    public List<NhomItem> getDanhSach() { return danhSach; }
}
