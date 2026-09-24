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

    /**
     * Trạng thái HÀNH CHÍNH của bàn: "true" = đang sử dụng, "false" = bảo trì.
     *
     * Khác hẳn TINHTRANG ở trên — TINHTRANG là trạng thái VẬN HÀNH, đổi liên
     * tục trong ngày khi khách vào/ra. Còn HOATDONG do quản lý đặt, hiếm khi
     * đổi, và dùng để đánh dấu bàn hỏng hoặc tạm ngưng.
     *
     * Lý do tồn tại: bàn đã có lịch sử đơn hàng thì cơ sở dữ liệu KHÔNG cho
     * xóa (khóa ngoại DONDAT.MABAN dùng NO ACTION, để bảo toàn số liệu doanh
     * thu). Chế độ bảo trì là lối thoát cho ràng buộc đó.
     */
    @SerializedName("HOATDONG")
    private String hoatDong;

    // Trạng thái phản hồi của yêu cầu cập nhật bàn ("success", "error")
    @SerializedName("status")
    private String status;

    // Tin nhắn mô tả chi tiết lỗi hoặc thành công
    @SerializedName("message")
    private String message;

    // Thời gian hẹn tới nhận bàn (THOIGIANHEN) trong trường hợp bàn được đặt trước
    @SerializedName("THOIGIANHEN")
    private String thoigianhen;

    /*
     * Ảnh bàn, máy chủ đã phân loại sẵn (kho_doc_hinh trong api/kho.php):
     * LOAI_ANH là 'anh_noi_bo' | 'anh_ngoai' | 'khong_co'. URL_ANH_NHO là bản
     * rộng 480px cho thẻ bàn; URL_ANH là ảnh gốc cho ảnh xem trước cỡ lớn.
     * Đường dẫn nội bộ là tương đối — ghép bằng ViewUtils.getImageUrl().
     */
    @SerializedName("LOAI_ANH")
    private String loaiAnh;

    @SerializedName("URL_ANH")
    private String urlAnh;

    @SerializedName("URL_ANH_NHO")
    private String urlAnhNho;

    public String getLoaiAnh() { return loaiAnh; }
    public String getUrlAnh() { return urlAnh; }
    public void setUrlAnh(String urlAnh) { this.urlAnh = urlAnh; }
    public String getUrlAnhNho() { return urlAnhNho; }
    public void setUrlAnhNho(String urlAnhNho) { this.urlAnhNho = urlAnhNho; }

    public int getMaBan() { return maBan; }
    public void setMaBan(int maBan) { this.maBan = maBan; }
    
    public String getTenBan() { return tenBan; }
    public void setTenBan(String tenBan) { this.tenBan = tenBan; }

    public String getTinhTrang() { return tinhTrang; }
    public void setTinhTrang(String tinhTrang) { this.tinhTrang = tinhTrang; }

    /** Trả về "true" nếu bàn đang hoạt động. Mặc định "true" khi máy chủ
     *  chưa gửi trường này (tương thích ngược với bản API cũ). */
    public String getHoatDong() { return hoatDong == null ? "true" : hoatDong; }
    public void setHoatDong(String hoatDong) { this.hoatDong = hoatDong; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getThoigianhen() { return thoigianhen; }
    public void setThoigianhen(String thoigianhen) { this.thoigianhen = thoigianhen; }
}
