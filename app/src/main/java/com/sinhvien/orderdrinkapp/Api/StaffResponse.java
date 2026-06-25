package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;
import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;

/**
 * StaffResponse - Mô hình dữ liệu phản hồi thông tin Nhân viên (hoặc Khách hàng đăng ký) từ API.
 * Hỗ trợ nhận thông tin đăng nhập, hồ sơ cá nhân và JWT token/session token từ server.
 */
public class StaffResponse {
    // Mã nhân viên/khách hàng (MANV)
    @SerializedName("MANV")
    private int maNV;

    // Họ và tên nhân viên (HOTENNV)
    @SerializedName("HOTENNV")
    private String hoTenNV;

    // Tên đăng nhập (TENDN)
    @SerializedName("TENDN")
    private String tenDN;

    // Mật khẩu (MATKHAU) dưới dạng mã hóa hoặc gốc phục vụ đồng bộ SQLite
    @SerializedName("MATKHAU")
    private String matKhau;

    // Địa chỉ email (EMAIL)
    @SerializedName("EMAIL")
    private String email;

    // Số điện thoại (SDT)
    @SerializedName("SDT")
    private String sdt;

    // Giới tính (GIOITINH), ví dụ: "Nam", "Nữ", "Khác"
    @SerializedName("GIOITINH")
    private String gioiTinh;

    // Ngày sinh (NGAYSINH) dưới dạng String "YYYY-MM-DD"
    @SerializedName("NGAYSINH")
    private String ngaySinh;

    // Mã quyền hạn (MAQUYEN): 1 - Admin, 2 - Thu ngân, 3 - Phục vụ, 4 - Khách hàng
    @SerializedName("MAQUYEN")
    private int maQuyen;

    // Trạng thái phản hồi của yêu cầu đăng nhập/đăng ký
    @SerializedName("status")
    private String status;

    // Thông báo lỗi hoặc thành công từ API
    @SerializedName("message")
    private String message;

    // Token xác thực phiên đăng nhập (TOKEN) được cấp từ server
    @SerializedName("TOKEN")
    private String token;

    public int getMaNV() { return maNV; }
    public String getHoTenNV() { return hoTenNV; }
    public String getTenDN() { return tenDN; }
    public String getMatKhau() { return matKhau; }
    public String getEmail() { return email; }
    public String getSdt() { return sdt; }
    public String getGioiTinh() { return gioiTinh; }
    public String getNgaySinh() { return ngaySinh; }
    public int getMaQuyen() { return maQuyen; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
}
