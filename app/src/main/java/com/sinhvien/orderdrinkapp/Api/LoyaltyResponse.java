package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Phản hồi của `api/loyalty_home.php` — toàn bộ dữ liệu màn hình
 * "Điểm danh & Quà tặng" trong MỘT lời gọi.
 *
 * Gom vào một lớp lồng nhau thay vì bốn lời gọi riêng: màn hình hiển thị
 * mọi phần cùng lúc, và bốn phản hồi về rải rác sẽ làm giao diện nhảy
 * từng mảng trên mạng chậm.
 */
public class LoyaltyResponse {

    @SerializedName("status")   private String status;
    @SerializedName("message")  private String message;

    /** Điểm còn dùng được. */
    @SerializedName("diem")     private int diem;

    /** Điểm cộng dồn từ trước tới nay, không giảm khi tiêu. */
    @SerializedName("tong_diem") private int tongDiem;

    /** Số ngày điểm danh liên tiếp. */
    @SerializedName("chuoi")    private int chuoi;

    @SerializedName("da_diem_danh") private boolean daDiemDanh;
    @SerializedName("diem_hom_nay") private int diemHomNay;

    @SerializedName("hang")     private String hang;
    @SerializedName("hang_ten") private String hangTen;
    @SerializedName("chi_tieu") private long chiTieu;

    @SerializedName("o_ngay")      private List<ONgay> oNgay;
    @SerializedName("voucher")     private List<Voucher> voucher;
    @SerializedName("ma_cua_toi")  private List<MaGiamGia> maCuaToi;

    /** Khác null khi máy chủ vừa cấp quà tri ân tuần này. */
    @SerializedName("qua_tuan_moi") private QuaTuan quaTuanMoi;

    public String getStatus()          { return status; }
    public String getMessage()         { return message; }
    public int getDiem()               { return diem; }
    public int getTongDiem()           { return tongDiem; }
    public int getChuoi()              { return chuoi; }
    public boolean isDaDiemDanh()      { return daDiemDanh; }
    public int getDiemHomNay()         { return diemHomNay; }
    public String getHang()            { return hang; }
    public String getHangTen()         { return hangTen; }
    public long getChiTieu()           { return chiTieu; }
    public List<ONgay> getONgay()      { return oNgay; }
    public List<Voucher> getVoucher()  { return voucher; }
    public List<MaGiamGia> getMaCuaToi() { return maCuaToi; }
    public QuaTuan getQuaTuanMoi()     { return quaTuanMoi; }

    /** Một ô trong bảng điểm danh 7 ngày. */
    public static class ONgay {
        @SerializedName("ngay")       private int ngay;
        @SerializedName("diem")       private int diem;
        @SerializedName("la_moc")     private boolean laMoc;
        /** 'xong' | 'homnay' | 'chua' */
        @SerializedName("trang_thai") private String trangThai;

        public int getNgay()          { return ngay; }
        public int getDiem()          { return diem; }
        public boolean isLaMoc()      { return laMoc; }
        public String getTrangThai()  { return trangThai; }
    }

    /** Một phiếu có thể đổi bằng điểm. */
    public static class Voucher {
        @SerializedName("MAVOUCHER")     private int maVoucher;
        @SerializedName("TEN")           private String ten;
        @SerializedName("MOTA")          private String moTa;
        @SerializedName("LOAI_GIAM")     private String loaiGiam;
        @SerializedName("GIATRI")        private int giaTri;
        @SerializedName("GIAM_TOIDA")    private int giamToiDa;
        @SerializedName("DON_TOITHIEU")  private int donToiThieu;
        @SerializedName("DIEM_DOI")      private int diemDoi;
        @SerializedName("HANG_TOITHIEU") private String hangToiThieu;
        @SerializedName("DU_DIEM")       private boolean duDiem;
        @SerializedName("DU_HANG")       private boolean duHang;

        public int getMaVoucher()     { return maVoucher; }
        public String getTen()        { return ten; }
        public String getMoTa()       { return moTa; }
        public int getDiemDoi()       { return diemDoi; }
        public int getDonToiThieu()   { return donToiThieu; }
        public String getHangToiThieu() { return hangToiThieu; }
        public boolean isDuDiem()     { return duDiem; }
        public boolean isDuHang()     { return duHang; }
    }

    /** Một mã đã phát cho khách. */
    public static class MaGiamGia {
        @SerializedName("MACODE")       private String maCode;
        @SerializedName("NGUON")        private String nguon;
        @SerializedName("NGAYTAO")      private String ngayTao;
        @SerializedName("NGAYHETHAN")   private String ngayHetHan;
        @SerializedName("TINHTRANG")    private String tinhTrang;
        @SerializedName("TEN")          private String ten;
        @SerializedName("MOTA")         private String moTa;
        @SerializedName("DON_TOITHIEU") private int donToiThieu;

        public String getMaCode()     { return maCode; }
        public String getNguon()      { return nguon; }
        public String getNgayHetHan() { return ngayHetHan; }
        public String getTinhTrang()  { return tinhTrang; }
        public String getTen()        { return ten; }
        public String getMoTa()       { return moTa; }
        public int getDonToiThieu()   { return donToiThieu; }
    }

    /** Quà tri ân tuần vừa được cấp. */
    public static class QuaTuan {
        @SerializedName("MACODE") private String maCode;
        @SerializedName("TEN")    private String ten;
        @SerializedName("MOTA")   private String moTa;

        public String getMaCode() { return maCode; }
        public String getTen()    { return ten; }
        public String getMoTa()   { return moTa; }
    }
}
