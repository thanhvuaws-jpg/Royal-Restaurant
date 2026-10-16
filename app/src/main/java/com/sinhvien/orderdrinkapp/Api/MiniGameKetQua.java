package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * Kết quả POST `api/minigame.php` (action = quay | mo_ruong).
 *
 * Máy chủ đã chọn ô trúng và ghi điểm/mã TRƯỚC khi trả về; app chỉ diễn lại.
 */
public class MiniGameKetQua {

    @SerializedName("status")      private String status;
    @SerializedName("message")     private String message;
    @SerializedName("ma")          private String ma;
    /** Ô trúng 0..11 (vòng quay). */
    @SerializedName("o")           private int o;
    /** diem | voucher | luot | trong */
    @SerializedName("loai")        private String loai;
    @SerializedName("diem_nhan")   private int diemNhan;
    @SerializedName("macode")      private String maCode;
    /** Tên giải, vd "+50 điểm", "Mã Giảm 10.000đ", "Thêm 1 lượt quay". */
    @SerializedName("ten")         private String ten;
    /** Rương: tên voucher kèm theo (nếu có). */
    @SerializedName("ten_voucher") private String tenVoucher;
    /** Số điểm sau khi cộng. */
    @SerializedName("diem")        private int diem;
    @SerializedName("luot_con")    private int luotCon;

    public String getStatus()     { return status; }
    public String getMessage()    { return message; }
    public String getMa()         { return ma; }
    public int getO()             { return o; }
    public String getLoai()       { return loai; }
    public int getDiemNhan()      { return diemNhan; }
    public String getMaCode()     { return maCode; }
    public String getTen()        { return ten; }
    public String getTenVoucher() { return tenVoucher; }
    public int getDiem()          { return diem; }
    public int getLuotCon()       { return luotCon; }
}
