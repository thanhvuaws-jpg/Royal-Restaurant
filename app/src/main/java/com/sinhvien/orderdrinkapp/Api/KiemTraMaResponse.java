package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * Kết quả nhân viên tra một mã giảm giá trước khi thu tiền.
 *
 * TIENGIAM và CON_LAI chỉ có giá trị khi lời gọi kèm mã đơn — không có đơn
 * thì không biết giảm bao nhiêu, nhất là với mã giảm theo phần trăm.
 */
public class KiemTraMaResponse {

    @SerializedName("status")       private String status;
    @SerializedName("message")      private String message;
    @SerializedName("MACODE")       private String maCode;
    @SerializedName("TEN")          private String ten;
    @SerializedName("MOTA")         private String moTa;
    @SerializedName("DON_TOITHIEU") private int donToiThieu;
    @SerializedName("NGAYHETHAN")   private String ngayHetHan;
    /** Tên chủ mã, để nhân viên đối chiếu với khách đang đứng trước mặt. */
    @SerializedName("CHU_MA")       private String chuMa;
    @SerializedName("TONGTIEN")     private Long tongTien;
    @SerializedName("TIENGIAM")     private Long tienGiam;
    @SerializedName("CON_LAI")      private Long conLai;

    public String getStatus()     { return status; }
    public String getMessage()    { return message; }
    public String getMaCode()     { return maCode; }
    public String getTen()        { return ten; }
    public String getMoTa()       { return moTa; }
    public int getDonToiThieu()   { return donToiThieu; }
    public String getNgayHetHan() { return ngayHetHan; }
    public String getChuMa()      { return chuMa; }
    public Long getTongTien()     { return tongTien; }
    public Long getTienGiam()     { return tienGiam; }
    public Long getConLai()       { return conLai; }
}
