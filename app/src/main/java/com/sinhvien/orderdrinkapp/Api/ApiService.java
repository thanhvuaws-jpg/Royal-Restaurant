package com.sinhvien.orderdrinkapp.Api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    
    // Đăng nhập
    @FormUrlEncoded
    @POST("api/login.php")
    Call<StaffResponse> login(
            @Field("tendn") String tenDN,
            @Field("matkhau") String matKhau
    );

    // Thêm nhân viên
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

    @GET("api/get_staff.php")
    Call<List<StaffResponse>> getStaff();
    // Thu ngân: Lấy danh sách đơn đang chờ
    @GET("api/get_pending_orders.php")
    Call<List<OrderResponse>> getPendingOrders();

    // Thu ngân: Xác nhận thanh toán
    @FormUrlEncoded
    @POST("api/confirm_payment.php")
    Call<OrderResponse> confirmPayment(
            @Field("madondat") int madondat,
            @Field("phuongthuc") String phuongthuc
    );

    // Nhân viên: Kiểm tra trạng thái đơn
    @GET("api/check_order_status.php")
    Call<OrderResponse> checkOrderStatus(@Query("madondat") int madondat);

    // Lấy danh sách loại món (Bữa sáng, trưa, tối...)
    @GET("api/get_categories.php")
    Call<List<LoaiMonResponse>> getCategories();

    // Lấy danh sách món ăn theo mã loại
    @GET("api/get_dishes.php")
    Call<com.sinhvien.orderdrinkapp.Api.DishPageResponse> getDishes(
            @Query("maloai") int maLoai,
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("search") String search
    );

    // Bàn ăn
    @GET("api/get_tables.php")
    Call<List<TableResponse>> getTables();

    @FormUrlEncoded
    @POST("api/add_table.php")
    Call<TableResponse> addTable(@Field("tenban") String tenBan);

    // Đặt món
    @FormUrlEncoded
    @POST("api/create_order.php")
    Call<OrderResponse> createOrder(
            @Field("manv") int maNV,
            @Field("maban") int maBan
    );

    @FormUrlEncoded
    @POST("api/add_order_detail.php")
    Call<OrderResponse> addOrderDetail(
            @Field("madondat") int maDonDat,
            @Field("mamon") int maMon,
            @Field("soluong") int soLuong
    );

    @GET("api/get_order_by_table.php")
    Call<OrderResponse> getOrderByTable(@Query("maban") int maBan);

    @GET("api/get_order_details.php")
    Call<List<OrderDetailResponse>> getOrderDetails(@Query("madondat") int maDonDat);

    @FormUrlEncoded
    @POST("api/checkout_order.php")
    Call<OrderResponse> checkoutOrder(
            @Field("madondat") int madondat,
            @Field("tongtien") long tongtien,
            @Field("phuongthuc") String phuongthuc
    );

    @FormUrlEncoded
    @POST("api/check_session.php")
    Call<OrderResponse> checkSession(
            @Field("manv") int manv,
            @Field("token") String token
    );

    @FormUrlEncoded
    @POST("api/delete_table.php")
    Call<OrderResponse> deleteTable(@Field("maban") int maban);

    @GET("api/get_statistics.php")
    Call<List<StatisticResponse>> getStatistics();

    @GET("api/get_paid_orders.php")
    Call<List<OrderResponse>> getPaidOrders();

    @FormUrlEncoded
    @POST("api/update_dish_status.php")
    Call<OrderResponse> updateDishStatus(
            @Field("mamon") int maMon,
            @Field("tinhtrang") String tinhTrang
    );

    @FormUrlEncoded
    @POST("api/update_category.php")
    Call<OrderResponse> manageCategory(
            @Field("action") String action,
            @Field("maloai") int maLoai,
            @Field("tenloai") String tenLoai,
            @Field("hinhanh") String hinhAnh
    );

    @GET("api/get_category_by_id.php")
    Call<LoaiMonResponse> getCategoryById(@Query("maloai") int maLoai);

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

    @GET("api/get_dish_by_id.php")
    Call<MonResponse> getDishById(@Query("mamon") int maMon);

    @FormUrlEncoded
    @POST("api/update_table_admin.php")
    Call<OrderResponse> manageTable(
            @Field("action") String action,
            @Field("maban") int maBan,
            @Field("tenban") String tenBan
    );

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

    @GET("api/get_staff_by_id.php")
    Call<StaffResponse> getStaffById(@Query("manv") int maNV);

    // Đặt bàn & Đặt món trước
    @FormUrlEncoded
    @POST("api/create_booking.php")
    Call<BookingResponse> createBooking(
            @Field("makh") int maKH,
            @Field("maban") int maBan,
            @Field("thoigianhen") String thoigianhen,
            @Field("monan") String monanJson
    );

    @GET("api/get_bookings.php")
    Call<List<BookingResponse>> getBookings(@Query("makh") int maKH);

    @GET("api/get_customer_spending.php")
    Call<BookingResponse> getCustomerSpending(@Query("makh") int maKH);

    @FormUrlEncoded
    @POST("api/update_booking_status.php")
    Call<BookingResponse> updateBookingStatus(
            @Field("madatban") int madatban,
            @Field("tinhtrang") String tinhtrang
    );

    @FormUrlEncoded
    @POST("api/confirm_booking.php")
    Call<BookingResponse> confirmBooking(
            @Field("madatban") int madatban,
            @Field("manv") int manv
    );
}
