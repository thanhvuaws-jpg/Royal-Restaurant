package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * CustomerProfileResponse - Mô hình dữ liệu phản hồi thông tin chi tiết của Khách hàng.
 * Bao gồm trạng thái, số điện thoại liên kết, mức chi tiêu và danh sách các đơn đặt bàn đã thực hiện.
 */
public class CustomerProfileResponse {
    // Trạng thái phản hồi từ API ("success", "error")
    @SerializedName("status")
    private String status;

    // Thông điệp phản hồi từ hệ thống
    @SerializedName("message")
    private String message;

    // Số điện thoại của khách hàng
    @SerializedName("sdt")
    private String sdt;

    // Tổng mức chi tiêu tích lũy của khách hàng tại nhà hàng
    @SerializedName("spending")
    private String spending;

    // Danh sách toàn bộ lịch sử đặt bàn của khách hàng
    @SerializedName("bookings")
    private List<BookingResponse> bookings;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }

    public String getSpending() { return spending; }
    public void setSpending(String spending) { this.spending = spending; }

    public List<BookingResponse> getBookings() { return bookings; }
    public void setBookings(List<BookingResponse> bookings) { this.bookings = bookings; }
}
