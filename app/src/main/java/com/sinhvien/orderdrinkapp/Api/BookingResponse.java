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

    /*
     * ─────────────────── Các trường bổ sung 12/09/2026 ───────────────────
     *
     * `api/get_bookings.php` đã trả về những trường này từ nhiệm vụ E5
     * (11/09), nhưng model không khai báo nên Gson BỎ QUA HOÀN TOÀN —
     * không lỗi, không cảnh báo, dữ liệu chỉ lặng lẽ biến mất.
     *
     * Hậu quả thấy được: màn hình quản lý đặt bàn của nhân viên hiển thị
     * "Mã Khách hàng: #19" thay vì tên và số điện thoại thật. Với phiếu
     * của khách vãng lai (MAKH = 0) thì thành "Mã Khách hàng: #0" — nhân
     * viên không có gì để gọi lại xác nhận.
     *
     * Bài học: thêm trường ở phía máy chủ mà quên khai báo ở model thì
     * không có gì báo cho biết. Sửa endpoint xong phải rà lại model.
     */

    /** Tên người đặt — gộp cả khách có tài khoản lẫn khách vãng lai. */
    @SerializedName("TENNGUOIDAT")
    private String tenNguoiDat;

    /** Số điện thoại người đặt. */
    @SerializedName("SDTNGUOIDAT")
    private String sdtNguoiDat;

    /** true nếu phiếu đặt từ landing page (không có tài khoản). */
    @SerializedName("LA_VANG_LAI")
    private boolean laVangLai;

    @SerializedName("SOKHACH")
    private int soKhach;

    @SerializedName("GHICHU")
    private String ghiChu;

    /** Số LOẠI món đặt trước, để phân biệt "chưa đặt món" với "món giá 0". */
    @SerializedName("SO_MON")
    private int soMon;

    public String getTenNguoiDat() { return tenNguoiDat; }
    public String getSdtNguoiDat() { return sdtNguoiDat; }
    public boolean isLaVangLai()   { return laVangLai; }
    public int getSoKhach()        { return soKhach; }
    public String getGhiChu()      { return ghiChu; }
    public int getSoMon()          { return soMon; }

    /** Mã giảm giá đang được giữ cho phiếu này. */
    @SerializedName("MACODE")
    private String maCode;

    public String getMaCode()      { return maCode; }
    public void setTongTien(String tongTien) { this.tongTien = tongTien; }
}
