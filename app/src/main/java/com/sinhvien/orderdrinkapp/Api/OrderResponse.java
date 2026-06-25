package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * OrderResponse - Mô hình dữ liệu phản hồi đại diện cho Đơn đặt (Order) từ API.
 * Chứa thông tin trạng thái đơn, phương thức thanh toán, mã đơn, nhân viên phục vụ, bàn ăn, ngày đặt và tổng tiền.
 */
public class OrderResponse {
    // Trạng thái của API response ("success", "error")
    @SerializedName("status")
    private String status;

    // Thông báo chi tiết trả về từ API
    @SerializedName("message")
    private String message;

    // Tình trạng đơn hàng (ví dụ: "false" - chưa thanh toán, "true" - đã thanh toán/chờ xác nhận)
    @SerializedName("tinhtrang")
    private String tinhTrang;

    // Phương thức thanh toán (ví dụ: "Tiền mặt", "VietQR")
    @SerializedName(value = "phuongthuc", alternate = {"PHUONGTHUCTT"})
    private String phuongThuc;

    // Mã đơn đặt món (MADONDAT)
    @SerializedName("MADONDAT")
    private int maDonDat;

    // Mã nhân viên phục vụ hoặc tạo hóa đơn (MANV)
    @SerializedName("MANV")
    private int maNV;

    // Mã số bàn ăn được gán cho đơn hàng này (MABAN)
    @SerializedName("MABAN")
    private int maBan;

    // Thời gian tạo đơn hàng (NGAYDAT)
    @SerializedName("NGAYDAT")
    private String ngayDat;

    // Tổng số tiền hóa đơn cần trả (TONGTIEN) dưới dạng chuỗi
    @SerializedName("TONGTIEN")
    private String tongTien;

    // Tên nhân viên phục vụ để hiển thị
    @SerializedName("HOTENNV")
    private String hoTenNV;

    // Tên bàn ăn tương ứng để hiển thị trên hóa đơn
    @SerializedName("TENBAN")
    private String tenBan;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMaDonDat() {
        return maDonDat;
    }

    public void setMaDonDat(int maDonDat) {
        this.maDonDat = maDonDat;
    }

    public int getMaNV() { return maNV; }
    public int getMaBan() { return maBan; }
    public String getNgayDat() { return ngayDat; }
    public String getTongTien() { return tongTien; }
    public String getHoTenNV() { return hoTenNV; }
    public String getTenBan() { return tenBan; }
    public String getTinhTrang() { return tinhTrang; }
    public String getPhuongThuc() { return phuongThuc; }
}
