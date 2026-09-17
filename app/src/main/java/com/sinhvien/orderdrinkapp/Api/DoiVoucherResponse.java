package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/** Kết quả đổi điểm lấy voucher. */
public class DoiVoucherResponse {

    @SerializedName("status")   private String status;
    @SerializedName("message")  private String message;
    @SerializedName("MACODE")   private String maCode;
    @SerializedName("TEN")      private String ten;
    @SerializedName("MOTA")     private String moTa;
    @SerializedName("diem_tru") private int diemTru;
    /** Số dư còn lại sau khi trừ. */
    @SerializedName("diem")     private int diem;

    public String getStatus()  { return status; }
    public String getMessage() { return message; }
    public String getMaCode()  { return maCode; }
    public String getTen()     { return ten; }
    public String getMoTa()    { return moTa; }
    public int getDiemTru()    { return diemTru; }
    public int getDiem()       { return diem; }
}
