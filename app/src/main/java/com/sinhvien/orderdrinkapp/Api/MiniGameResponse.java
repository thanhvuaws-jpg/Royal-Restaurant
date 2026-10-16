package com.sinhvien.orderdrinkapp.Api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Phản hồi GET `api/minigame.php` — trạng thái Sân chơi Royal: điểm, 12 ô vòng
 * quay (nhãn do máy chủ đặt), lượt còn lại hôm nay, và rương 7 ngày.
 */
public class MiniGameResponse {

    @SerializedName("status")  private String status;
    @SerializedName("message") private String message;
    /** "chua_san_sang" khi máy chủ chưa chạy cập nhật CSDL sql/38. */
    @SerializedName("ma")      private String ma;
    @SerializedName("diem")    private int diem;
    @SerializedName("vong_quay") private VongQuay vongQuay;
    @SerializedName("ruong")   private Ruong ruong;

    public String getStatus()    { return status; }
    public String getMessage()   { return message; }
    public String getMa()        { return ma; }
    public int getDiem()         { return diem; }
    public VongQuay getVongQuay() { return vongQuay; }
    public Ruong getRuong()      { return ruong; }

    public static class VongQuay {
        @SerializedName("luot_con")     private int luotCon;
        @SerializedName("da_diem_danh") private boolean daDiemDanh;
        @SerializedName("o")            private List<O> o;

        public int getLuotCon()       { return luotCon; }
        /** Cập nhật ngay theo phản hồi lượt quay, trước khi tải lại cả trạng thái. */
        public void setLuotCon(int luotCon) { this.luotCon = luotCon; }
        public boolean isDaDiemDanh() { return daDiemDanh; }
        public List<O> getO()         { return o; }
    }

    /** Một ô trên vòng quay: chữ lớn ("50", "10K", "+1") và chữ nhỏ ("điểm", "giảm"). */
    public static class O {
        @SerializedName("to")   private String to;
        @SerializedName("nho")  private String nho;
        @SerializedName("loai") private String loai;

        public String getTo()   { return to; }
        public String getNho()  { return nho; }
        public String getLoai() { return loai; }
    }

    public static class Ruong {
        @SerializedName("mo_duoc") private boolean moDuoc;
        /** Hạn mở rương dạng dd/MM, null khi chưa có rương. */
        @SerializedName("han")     private String han;
        @SerializedName("chuoi")   private int chuoi;
        @SerializedName("can")     private int can;

        public boolean isMoDuoc() { return moDuoc; }
        public String getHan()    { return han; }
        public int getChuoi()     { return chuoi; }
        public int getCan()       { return can; }
    }
}
