package com.sinhvien.orderdrinkapp.Api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * ApiService — Định nghĩa danh sách các Endpoint HTTP API kết nối tới Server VPS.
 * Sử dụng thư viện Retrofit để thực hiện các yêu cầu GET và POST.
 */
public interface ApiService {
    
    /**
     * Đăng nhập hệ thống (Staff / Customer).
     * @param tenDN Tên đăng nhập.
     * @param matKhau Mật khẩu (chưa mã hóa).
     */
    @FormUrlEncoded
    @POST("api/login.php")
    Call<StaffResponse> login(
            @Field("tendn") String tenDN,
            @Field("matkhau") String matKhau
    );

    /**
     * Đăng ký tài khoản mới hoặc Thêm nhân viên.
     * @param hoTen Họ và tên.
     * @param tenDN Tên đăng nhập mong muốn.
     * @param matKhau Mật khẩu.
     * @param email Địa chỉ email.
     * @param sdt Số điện thoại.
     * @param gioiTinh Giới tính ("Nam", "Nữ", "Khác").
     * @param ngaySinh Ngày sinh định dạng "YYYY-MM-DD".
     * @param maQuyen Quyền hạn (1: Admin, 4: Khách hàng).
     */
    @FormUrlEncoded
    @POST("api/add_staff.php")
    Call<StaffResponse> addStaff(
            @Field("hoten") String hoTen,
            @Field("tendn") String tenDN,
            @Field("matkhau") String matKhau,
            @Field("email") String email,
            @Field("sdt") String sdt,
            @Field("gioitinh") String gioiTinh,
            @Field("ngaysinh") String ngaySinh,
            @Field("maquyen") int maQuyen
    );

    /**
     * Lấy danh sách toàn bộ nhân viên trong hệ thống (chỉ dành cho Quản lý).
     */
    @GET("api/get_staff.php")
    Call<List<StaffResponse>> getStaff();

    /**
     * Thu ngân: Lấy danh sách các đơn đặt món đang ở trạng thái chờ thanh toán.
     */
    @GET("api/get_pending_orders.php")
    Call<List<OrderResponse>> getPendingOrders();

    /**
     * Thu ngân: Xác nhận đơn đặt hàng đã thanh toán thành công.
     * @param madondat Mã số đơn đặt.
     * @param phuongthuc Phương thức thanh toán ("Tiền mặt", "Chuyển khoản").
     */
    @FormUrlEncoded
    @POST("api/confirm_payment.php")
    Call<OrderResponse> confirmPayment(
            @Field("madondat") int madondat,
            @Field("phuongthuc") String phuongthuc
    );

    /**
     * Nhân viên: Kiểm tra trạng thái hiện tại của một đơn hàng cụ thể.
     */
    @GET("api/check_order_status.php")
    Call<OrderResponse> checkOrderStatus(@Query("madondat") int madondat);

    /**
     * Lấy danh sách toàn bộ danh mục nhóm món ăn (Bữa sáng, Giải khát, Lẩu...).
     */
    @GET("api/get_categories.php")
    Call<List<LoaiMonResponse>> getCategories();

    /**
     * Lấy danh sách món ăn phân trang có hỗ trợ tìm kiếm theo tên và lọc theo danh mục.
     * @param maLoai Mã loại danh mục.
     * @param page Số trang hiện tại.
     * @param limit Số lượng bản ghi mỗi trang.
     * @param search Từ khóa tìm kiếm tên món.
     */
    @GET("api/get_dishes.php")
    Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> getDishes(
            @Query("maloai") int maLoai,
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("search") String search
    );

    /**
     * Lấy danh sách toàn bộ Bàn ăn của nhà hàng.
     */
    @GET("api/get_tables.php")
    Call<List<TableResponse>> getTables();

    /**
     * Thêm mới một bàn ăn (chỉ dành cho Admin).
     */
    @FormUrlEncoded
    @POST("api/add_table.php")
    Call<TableResponse> addTable(@Field("tenban") String tenBan);

    /**
     * Khởi tạo một đơn đặt món (Order) mới cho một bàn ăn cụ thể.
     * @param maNV ID nhân viên tạo đơn.
     * @param maBan ID bàn ăn được phục vụ.
     */
    @FormUrlEncoded
    @POST("api/create_order.php")
    Call<OrderResponse> createOrder(
            @Field("manv") int maNV,
            @Field("maban") int maBan
    );

    /**
     * Thêm món ăn và số lượng tương ứng vào chi tiết đơn đặt món đang mở.
     * @param maDonDat ID đơn hàng hiện tại.
     * @param maMon ID món ăn muốn thêm.
     * @param soLuong Số lượng phần ăn.
     */
    @FormUrlEncoded
    @POST("api/add_order_detail.php")
    Call<OrderResponse> addOrderDetail(
            @Field("madondat") int maDonDat,
            @Field("mamon") int maMon,
            @Field("soluong") int soLuong
    );

    /**
     * Lấy đơn đặt món đang hoạt động (chưa thanh toán) của một bàn ăn cụ thể.
     */
    @GET("api/get_order_by_table.php")
    Call<OrderResponse> getOrderByTable(@Query("maban") int maBan);

    /**
     * Lấy danh sách chi tiết các món ăn trong đơn đặt món.
     */
    @GET("api/get_order_details.php")
    Call<List<OrderDetailResponse>> getOrderDetails(@Query("madondat") int maDonDat);

    /**
     * Gửi yêu cầu thanh toán đơn hàng (Chuyển trạng thái từ hoạt động sang chờ thanh toán).
     */
    @FormUrlEncoded
    @POST("api/checkout_order.php")
    Call<OrderResponse> checkoutOrder(
            @Field("madondat") int madondat,
            @Field("tongtien") long tongtien,
            @Field("phuongthuc") String phuongthuc
    );

    /**
     * Kiểm tra tính hợp lệ của token đăng nhập.
     */
    @FormUrlEncoded
    @POST("api/check_session.php")
    Call<OrderResponse> checkSession(
            @Field("manv") int manv,
            @Field("token") String token
    );

    /**
     * Xóa bỏ bàn ăn (Admin).
     */
    @FormUrlEncoded
    @POST("api/delete_table.php")
    Call<OrderResponse> deleteTable(@Field("maban") int maban);

    /**
     * Lấy báo cáo thống kê doanh thu tổng quát theo ngày/tháng/năm.
     */
    @GET("api/get_statistics.php")
    Call<List<StatisticResponse>> getStatistics();

    /**
     * Lấy danh sách các đơn hàng đã thanh toán thành công.
     */
    @GET("api/get_paid_orders.php")
    Call<List<OrderResponse>> getPaidOrders();

    /**
     * Cập nhật trạng thái còn món hay hết món của món ăn.
     * @param maMon ID món ăn.
     * @param tinhTrang Trạng thái ("Còn hàng", "Hết hàng").
     */
    @FormUrlEncoded
    @POST("api/update_dish_status.php")
    Call<OrderResponse> updateDishStatus(
            @Field("mamon") int maMon,
            @Field("tinhtrang") String tinhTrang
    );

    /**
     * Thêm, Sửa hoặc Xóa danh mục món ăn (Admin).
     * @param action Hành động ("add", "edit", "delete").
     */
    @FormUrlEncoded
    @POST("api/update_category.php")
    Call<OrderResponse> manageCategory(
            @Field("action") String action,
            @Field("maloai") int maLoai,
            @Field("tenloai") String tenLoai,
            @Field("hinhanh") String hinhAnh
    );

    /**
     * Lấy thông tin chi tiết một danh mục theo ID.
     */
    @GET("api/get_category_by_id.php")
    Call<LoaiMonResponse> getCategoryById(@Query("maloai") int maLoai);

    /**
     * Thêm, Sửa hoặc Xóa món ăn (Admin).
     * @param action Hành động ("add", "edit", "delete").
     */
    @FormUrlEncoded
    @POST("api/update_dish.php")
    Call<OrderResponse> manageDish(
            @Field("action") String action,
            @Field("mamon") int maMon,
            @Field("tenmon") String tenMon,
            @Field("giatien") String giaTien,
            @Field("maloai") int maLoai,
            @Field("tinhtrang") String tinhTrang,
            @Field("hinhanh") String hinhAnh
    );

    /**
     * Lấy thông tin chi tiết món ăn theo ID.
     */
    @GET("api/get_dish_by_id.php")
    Call<MonResponse> getDishById(@Query("mamon") int maMon);

    /**
     * Chỉnh sửa thông tin tên bàn ăn (Admin).
     */
    @FormUrlEncoded
    @POST("api/update_table_admin.php")
    Call<OrderResponse> manageTable(
            @Field("action") String action,
            @Field("maban") int maBan,
            @Field("tenban") String tenBan
    );

    /**
     * Quản lý thông tin Nhân viên: Thêm, sửa, xóa thông tin (Admin).
     * @param action Hành động ("add", "edit", "delete").
     */
    @FormUrlEncoded
    @POST("api/update_staff.php")
    Call<OrderResponse> manageStaff(
            @Field("action") String action,
            @Field("manv") int maNV,
            @Field("hoten") String hoTen,
            @Field("tendn") String tenDN,
            @Field("matkhau") String matKhau,
            @Field("email") String email,
            @Field("sdt") String sdt,
            @Field("gioitinh") String gioiTinh,
            @Field("ngaysinh") String ngaySinh,
            @Field("maquyen") int maQuyen
    );

    /**
     * Lấy thông tin chi tiết nhân viên theo ID.
     */
    @GET("api/get_staff_by_id.php")
    Call<StaffResponse> getStaffById(@Query("manv") int maNV);

    /**
     * Khách hàng: Gửi yêu cầu đặt bàn trước và kèm theo danh sách món ăn chọn trước (Pre-order).
     * @param maKH ID khách hàng thành viên.
     * @param maBan ID bàn ăn muốn đặt trước.
     * @param thoigianhen Thời gian hẹn tới quán dạng định dạng chuỗi.
     * @param monanJson Chuỗi JSON danh sách món ăn chọn trước.
     */
    @FormUrlEncoded
    @POST("api/create_booking.php")
    Call<BookingResponse> createBooking(
            @Field("makh") int maKH,
            @Field("maban") int maBan,
            @Field("thoigianhen") String thoigianhen,
            @Field("monan") String monanJson
    );

    /**
     * Khách hàng: Lấy danh sách lịch sử đặt bàn của bản thân.
     */
    @GET("api/get_bookings.php")
    Call<List<BookingResponse>> getBookings(@Query("makh") int maKH);

    /**
     * Lấy tổng chi tiêu của Khách hàng.
     */
    @GET("api/get_customer_spending.php")
    Call<BookingResponse> getCustomerSpending(@Query("makh") int maKH);

    /**
     * Cập nhật trạng thái đặt bàn (Hủy bàn, Xác nhận bàn...).
     */
    @FormUrlEncoded
    @POST("api/update_booking_status.php")
    Call<BookingResponse> updateBookingStatus(
            @Field("madatban") int madatban,
            @Field("tinhtrang") String tinhtrang
    );

    /**
     * Nhân viên: Xác nhận yêu cầu đặt bàn của Khách hàng.
     */
    @FormUrlEncoded
    @POST("api/confirm_booking.php")
    Call<BookingResponse> confirmBooking(
            @Field("madatban") int madatban,
            @Field("manv") int manv
    );

    /**
     * Nhân viên: Xác nhận khách đã đến nhận bàn đã đặt trước (Check-in).
     */
    @FormUrlEncoded
    @POST("api/checkin_booking.php")
    Call<BookingResponse> checkinBooking(
            @Field("madatban") int madatban,
            @Field("manv") int manv
    );

    /**
     * Lấy tình trạng đặt bàn của các bàn hiện tại trong ngày.
     */
    @GET("api/get_table_booking_status.php")
    Call<List<TableResponse>> getTableBookingStatus();

    /**
     * Lấy thông tin hồ sơ cá nhân và hạng thành viên của khách hàng.
     */
    @GET("api/get_customer_profile.php")
    Call<CustomerProfileResponse> getCustomerProfile(@Query("makh") int maKH);

    /**
     * Hủy hoặc tự động cập nhật hàng loạt trạng thái các bàn đặt quá giờ (quá hẹn) mà không đến.
     */
    @FormUrlEncoded
    @POST("api/batch_update_booking_status.php")
    Call<BookingResponse> batchUpdateBookingStatus(
            @Field("madatbans") String madatbans,
            @Field("tinhtrang") String tinhtrang
    );

    /**
     * Lấy danh sách đơn hàng đã thanh toán thành công lọc theo ngày cụ thể.
     */
    @GET("api/get_paid_orders.php")
    Call<List<OrderResponse>> getPaidOrders(@Query("date") String date);

    /**
     * Thống kê báo cáo các đơn hàng đã thanh toán thành công lọc theo khoảng thời gian tùy chọn.
     * @param fromDate Ngày bắt đầu lọc (YYYY-MM-DD).
     * @param toDate Ngày kết thúc lọc (YYYY-MM-DD).
     */
    @GET("api/get_paid_orders.php")
    Call<List<OrderResponse>> getPaidOrders(
            @Query("from_date") String fromDate,
            @Query("to_date") String toDate
    );
}

