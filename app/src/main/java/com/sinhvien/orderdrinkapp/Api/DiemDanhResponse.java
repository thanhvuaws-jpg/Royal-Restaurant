package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/** Kết quả một lần bấm điểm danh. */
public class DiemDanhResponse {

    @SerializedName("status")    private String status;
    @SerializedName("message")   private String message;
    @SerializedName("diem_nhan") private int diemNhan;
    /** Số dư sau khi cộng. */
    @SerializedName("diem")      private int diem;
    @SerializedName("chuoi")     private int chuoi;
    @SerializedName("vi_tri")    private int viTri;
    /** true khi đây là ngày thứ 7 — có thưởng thêm. */
    @SerializedName("la_moc")    private boolean laMoc;

    public String getStatus()  { return status; }
    public String getMessage() { return message; }
    public int getDiemNhan()   { return diemNhan; }
    public int getDiem()       { return diem; }
    public int getChuoi()      { return chuoi; }
    public int getViTri()      { return viTri; }
    public boolean isLaMoc()   { return laMoc; }
}
