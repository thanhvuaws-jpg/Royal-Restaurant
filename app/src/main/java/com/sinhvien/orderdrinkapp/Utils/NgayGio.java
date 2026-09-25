package com.sinhvien.orderdrinkapp.Utils;

/**
 * NgayGio — đổi chuỗi ngày giờ của máy chủ ("yyyy-MM-dd HH:mm:ss") sang dạng
 * app hiển thị, bằng CẮT CHUỖI.
 *
 * VÌ SAO KHÔNG DÙNG SimpleDateFormat (tối ưu hiệu năng, 25/09/2026)
 * -----------------------------------------------------------------
 * Các ViewModel đổi ngày cho TỪNG đơn bằng SimpleDateFormat.parse() rồi
 * format(), ngay trong onResponse của Retrofit — tức là trên luồng giao diện.
 * Màn Thống kê "30 ngày" có ~2.700 đơn, "Tất cả" còn nhiều hơn: đo trên máy ảo
 * thấy khựng ~0,5 giây (30 khung hình bị bỏ) mỗi lần tải. Chuỗi máy chủ luôn
 * đúng một định dạng cố định, nên cắt chuỗi cho cùng kết quả mà nhanh hơn
 * hàng trăm lần, không tạo đối tượng Date/Calendar nào.
 */
public final class NgayGio {

    private NgayGio() { }

    private static boolean dungDang(String s) {
        return s != null && s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-';
    }

    /** "2026-09-24 13:05:00" -> "24-09-2026"; chuỗi lạ thì trả nguyên. */
    public static String sangNgay(String s) {
        if (!dungDang(s)) return s;
        return s.substring(8, 10) + "-" + s.substring(5, 7) + "-" + s.substring(0, 4);
    }

    /** "2026-09-27 16:00:00" -> "16:00 · 27/09/2026" (giờ trước, như cách nói lịch hẹn). */
    public static String gioHen(String s) {
        if (!dungDang(s) || s.length() < 16) return s;
        return s.substring(11, 16) + " · " + s.substring(8, 10) + "/" + s.substring(5, 7) + "/" + s.substring(0, 4);
    }

    /** "2026-09-24 13:05:00" -> "24-09-2026 13:05"; chuỗi lạ thì trả nguyên. */
    public static String sangNgayGio(String s) {
        if (!dungDang(s)) return s;
        String ngay = sangNgay(s);
        return s.length() >= 16 ? ngay + " " + s.substring(11, 16) : ngay;
    }
}
