package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Phản hồi CÓ PHÂN TRANG của `api/get_bookings.php`.
 *
 * VÌ SAO CÓ LỚP NÀY THAY VÌ SỬA getBookings()
 * --------------------------------------------
 * `getBookings()` khai báo `Call<List<BookingResponse>>` — nghĩa là nó
 * mong nhận một MẢNG trần. Nếu đổi endpoint để luôn trả về đối tượng
 * {status, data, page, ...} thì Gson sẽ ném lỗi phân giải ở cả ba nơi
 * đang gọi:
 *
 *   - ManageBookingsFragment  (màn hình nhân viên)
 *   - CustomerBookingFragment (màn hình khách)
 *   - BookingAlertManager     (quét phiếu quá giờ mỗi 60 giây)
 *
 * Riêng chỗ thứ ba nguy hiểm nhất: nó từng có callback rỗng và nuốt lỗi
 * hoàn toàn (xem QĐ-044), nên sẽ hỏng mà không để lại dấu vết nào.
 *
 * Nên máy chủ giữ nguyên mảng trần theo mặc định, và chỉ trả về phong bì
 * này khi client gửi `phan_trang=1`. Cùng khuôn với DishPageResponse.
 */
public class BookingPageResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private List<BookingResponse> data;

    @SerializedName("page")
    private int page;

    @SerializedName("limit")
    private int limit;

    /** Tổng số phiếu khớp bộ lọc, KHÔNG phải số phiếu của trang này. */
    @SerializedName("total")
    private int total;

    @SerializedName("total_pages")
    private int totalPages;

    @SerializedName("has_more")
    private boolean hasMore;

    public String getStatus()               { return status; }
    public List<BookingResponse> getData()  { return data; }
    public int getPage()                    { return page; }
    public int getLimit()                   { return limit; }
    public int getTotal()                   { return total; }
    public int getTotalPages()              { return totalPages < 1 ? 1 : totalPages; }
    public boolean isHasMore()              { return hasMore; }
}
