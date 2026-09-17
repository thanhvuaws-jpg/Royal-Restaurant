package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả của `chat_lay.php` — hội thoại đang mở và các tin trong đó.
 *
 * `maHoiThoai` bằng null nghĩa là khách CHƯA từng nhắn. Đó là trạng thái
 * bình thường, không phải lỗi: máy chủ cố ý không tạo hội thoại rỗng chỉ
 * vì khách mở màn hình ra xem.
 */
public class ChatResponse {

    public static class HoiThoai {
        @SerializedName("MAHOITHOAI")     private int    maHoiThoai;
        @SerializedName("CHUDE")          private String chuDe;
        @SerializedName("TINHTRANG")      private String tinhTrang;
        @SerializedName("SO_CHUA_DOC_KH") private int    soChuaDocKH;
        @SerializedName("BOT_HOATDONG")   private String botHoatDong;

        public int    getMaHoiThoai() { return maHoiThoai; }
        public String getChuDe()      { return chuDe != null ? chuDe : ""; }
        /** 'mo' | 'dangxuly' | 'daxong' */
        public String getTinhTrang()  { return tinhTrang != null ? tinhTrang : ""; }
        /** Số tin của nhà hàng mà khách chưa xem. */
        public int    getSoChuaDocKH() { return soChuaDocKH; }

        /**
         * Trợ lý tự động còn được phép trả lời không.
         *
         * Mặc định TRUE khi máy chủ không gửi trường này — bản cũ của API
         * chưa có nó, và coi như bot còn trực là hành vi giống hệt trước đây.
         */
        public boolean botConTruc() { return !"false".equals(botHoatDong); }
    }

    @SerializedName("status")     private String        status;
    @SerializedName("message")    private String        message;
    @SerializedName("MAHOITHOAI") private Integer       maHoiThoai;
    @SerializedName("hoi_thoai")  private HoiThoai      hoiThoai;
    @SerializedName("tin_nhan")   private List<TinNhan> tinNhan;

    public String        getStatus()     { return status; }
    public String        getMessage()    { return message; }
    public Integer       getMaHoiThoai() { return maHoiThoai; }
    public HoiThoai      getHoiThoai()   { return hoiThoai; }
    public List<TinNhan> getTinNhan()    { return tinNhan != null ? tinNhan : new ArrayList<>(); }

    public boolean thanhCong() { return "success".equals(status); }
}
