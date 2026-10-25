package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * Kết quả gửi đơn cho thu ngân, có kèm thông tin mã giảm giá đã áp.
 *
 * Khác OrderResponse ở ba trường cuối. Giữ OrderResponse nguyên vẹn cho
 * luồng thanh toán không dùng mã, để không phải sửa chỗ gọi cũ.
 */
public class CheckoutResponse {

    @SerializedName("status")   private String status;
    @SerializedName("message")  private String message;
    @SerializedName("MADONDAT") private int maDonDat;
    /** Số tiền THỰC THU, đã trừ phần giảm. */
    @SerializedName("TONGTIEN") private long tongTien;
    @SerializedName("TIENGIAM") private long tienGiam;
    @SerializedName("MACODE")   private String maCode;
    /** Mã khách gắn lúc đặt bàn mà không áp được: máy chủ vẫn nhận đơn (tính đủ
     *  tiền, trả mã về ví) và gửi lý do ở đây để nhân viên báo khách. */
    @SerializedName("CANH_BAO_MA") private String canhBaoMa;

    public String getStatus()  { return status; }
    public String getCanhBaoMa() { return canhBaoMa; }
    public String getMessage() { return message; }
    public int getMaDonDat()   { return maDonDat; }
    public long getTongTien()  { return tongTien; }
    public long getTienGiam()  { return tienGiam; }
    public String getMaCode()  { return maCode; }
}
