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

    /*
     * ─────────────── Các trường bổ sung 12/09/2026 ───────────────
     *
     * get_customer_profile.php nay trả thêm họ tên, email và bốn con số
     * thống kê, để màn hình Lịch sử & chi tiêu hiển thị được mà không phải
     * tự đếm từ danh sách — danh sách nay đã phân trang nên đếm tại client
     * sẽ ra con số của riêng trang đang xem, không phải của toàn bộ.
     *
     * Trường `bookings` vẫn còn nhưng máy chủ luôn trả rỗng; danh sách thật
     * lấy từ get_bookings.php?phan_trang=1.
     */

    @SerializedName("hoten")
    private String hoTen;

    @SerializedName("email")
    private String email;

    @SerializedName("so_don")
    private int soDon;

    @SerializedName("so_lan_dat")
    private int soLanDat;

    @SerializedName("so_hoan_thanh")
    private int soHoanThanh;

    @SerializedName("so_da_huy")
    private int soDaHuy;

    @SerializedName("so_sap_toi")
    private int soSapToi;

    @SerializedName("lan_gan_nhat")
    private String lanGanNhat;

    public String getHoTen()      { return hoTen; }
    public String getEmail()      { return email; }
    public int getSoDon()         { return soDon; }
    public int getSoLanDat()      { return soLanDat; }
    public int getSoHoanThanh()   { return soHoanThanh; }
    public int getSoDaHuy()       { return soDaHuy; }
    public int getSoSapToi()      { return soSapToi; }
    public String getLanGanNhat() { return lanGanNhat; }
    public void setBookings(List<BookingResponse> bookings) { this.bookings = bookings; }
}
