package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CustomerProfileResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("sdt")
    private String sdt;

    @SerializedName("spending")
    private String spending;

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
