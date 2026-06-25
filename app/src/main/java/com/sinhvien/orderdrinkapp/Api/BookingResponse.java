package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * BookingResponse - Mô hình dữ liệu phản hồi (Response Model) của yêu cầu Đặt bàn.
 * Nhận và phân tích dữ liệu JSON phản hồi từ các API đặt bàn hoặc cập nhật trạng thái đặt bàn.
 */
public class BookingResponse {
    // Trạng thái phản hồi từ API (ví dụ: "success", "error")
    @SerializedName("status")
    private String status;

    // Tin nhắn mô tả chi tiết phản hồi từ Server
    @SerializedName("message")
    private String message;

    // Mã định danh của bản ghi đặt bàn (MADATBAN)
    @SerializedName("MADATBAN")
    private int maDatBan;

    // Mã số khách hàng thực hiện đặt bàn (MAKH)
    @SerializedName("MAKH")
    private int maKH;

    // Mã số bàn ăn được đặt trước (MABAN)
    @SerializedName("MABAN")
    private int maBan;

    // Thời gian khách hàng hẹn tới nhà hàng (định dạng String)
    @SerializedName("THOIGIANHEN")
    private String thoigianhen;

    // Trạng thái đặt bàn (ví dụ: "Đã xác nhận", "Đã hủy", "Đã nhận bàn")
    @SerializedName("TINHTRANG")
    private String tinhtrang;

    // Tên của bàn ăn được đặt trước (ví dụ: "Bàn số 1")
    @SerializedName("TENBAN")
    private String tenBan;

    // Tổng số tiền hoặc chi tiêu liên quan (ví dụ nếu có đặt món trước)
    @SerializedName("TONGTIEN")
    private String tongTien;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getMaDatBan() { return maDatBan; }
    public void setMaDatBan(int maDatBan) { this.maDatBan = maDatBan; }

    public int getMaKH() { return maKH; }
    public void setMaKH(int maKH) { this.maKH = maKH; }

    public int getMaBan() { return maBan; }
    public void setMaBan(int maBan) { this.maBan = maBan; }

    public String getThoigianhen() { return thoigianhen; }
    public void setThoigianhen(String thoigianhen) { this.thoigianhen = thoigianhen; }

    public String getTinhtrang() { return tinhtrang; }
    public void setTinhtrang(String tinhtrang) { this.tinhtrang = tinhtrang; }

    public String getTenBan() { return tenBan; }
    public void setTenBan(String tenBan) { this.tenBan = tenBan; }

    public String getTongTien() { return tongTien; }
    public void setTongTien(String tongTien) { this.tongTien = tongTien; }
}
