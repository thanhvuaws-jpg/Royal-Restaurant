package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * TableResponse - Mô hình dữ liệu phản hồi thông tin Bàn ăn từ API.
 * Chứa mã bàn, tên bàn, tình trạng sử dụng, trạng thái API và thời gian hẹn nếu có đặt bàn trước.
 */
public class TableResponse {
    // Mã bàn ăn (MABAN)
    @SerializedName("MABAN")
    private int maBan;

    // Tên của bàn ăn (TENBAN), ví dụ: "Bàn số 5"
    @SerializedName("TENBAN")
    private String tenBan;

    // Tình trạng bàn ăn (TINHTRANG), ví dụ: "true" (bàn đang có người ngồi), "false" (bàn trống)
    @SerializedName("TINHTRANG")
    private String tinhTrang;

    // Trạng thái phản hồi của yêu cầu cập nhật bàn ("success", "error")
    @SerializedName("status")
    private String status;

    // Tin nhắn mô tả chi tiết lỗi hoặc thành công
    @SerializedName("message")
    private String message;

    // Thời gian hẹn tới nhận bàn (THOIGIANHEN) trong trường hợp bàn được đặt trước
    @SerializedName("THOIGIANHEN")
    private String thoigianhen;

    public int getMaBan() { return maBan; }
    public void setMaBan(int maBan) { this.maBan = maBan; }
    
    public String getTenBan() { return tenBan; }
    public void setTenBan(String tenBan) { this.tenBan = tenBan; }

    public String getTinhTrang() { return tinhTrang; }
    public void setTinhTrang(String tinhTrang) { this.tinhTrang = tinhTrang; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getThoigianhen() { return thoigianhen; }
    public void setThoigianhen(String thoigianhen) { this.thoigianhen = thoigianhen; }
}
