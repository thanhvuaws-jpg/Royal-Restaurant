package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

/**
 * Một tin nhắn trong phòng chat chăm sóc khách hàng.
 *
 * Dùng chung cho cả tin lấy qua REST (`chat_lay.php`) lẫn tin nhận qua
 * Socket.IO (sự kiện `chat_moi`). Máy chủ cố ý phát ra CÙNG MỘT hình dạng ở
 * hai đường — xem `chat_dinh_dang_tin()` bên PHP và `dinhDangGio()` bên
 * `telegram.js` — nên chỉ cần một lớp này.
 */
public class TinNhan {

    /** 'khach' — chính người dùng gửi. */
    public static final String KHACH     = "khach";
    /** 'nhanvien' — người thật của quán trả lời. */
    public static final String NHAN_VIEN = "nhanvien";
    /** 'bot' — trợ lý tự động. */
    public static final String BOT       = "bot";

    @SerializedName("MATINNHAN")   private long   maTinNhan;
    @SerializedName("MAHOITHOAI")  private int    maHoiThoai;
    @SerializedName("NGUOIGUI")    private String nguoiGui;
    @SerializedName("TEN_HIENTHI") private String tenHienThi;
    @SerializedName("NOIDUNG")     private String noiDung;
    @SerializedName("NGAYTAO")     private String ngayTao;

    public TinNhan() { }

    /**
     * Tạo một tin "đang gửi" để hiện ngay trên màn hình trước khi máy chủ
     * trả lời. Mã âm để phân biệt với tin thật và để thay thế lại sau.
     */
    public static TinNhan tamThoi(String noiDung) {
        TinNhan t = new TinNhan();
        t.maTinNhan = -System.currentTimeMillis();
        t.nguoiGui  = KHACH;
        t.noiDung   = noiDung;
        t.ngayTao   = null;
        return t;
    }

    public long   getMaTinNhan()  { return maTinNhan; }
    public int    getMaHoiThoai() { return maHoiThoai; }
    public String getNguoiGui()   { return nguoiGui != null ? nguoiGui : ""; }
    public String getTenHienThi() { return tenHienThi != null ? tenHienThi : ""; }
    public String getNoiDung()    { return noiDung != null ? noiDung : ""; }
    public String getNgayTao()    { return ngayTao; }

    public boolean laCuaKhach() { return KHACH.equals(getNguoiGui()); }
    public boolean laCuaBot()   { return BOT.equals(getNguoiGui()); }
    /** Tin chưa được máy chủ xác nhận (mã âm do tamThoi() cấp). */
    public boolean dangGui()    { return maTinNhan < 0; }
}
