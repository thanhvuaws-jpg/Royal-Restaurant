package com.sinhvien.orderdrinkapp.Utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * TienTe — MỘT cách hiển thị tiền cho cả app: "110.000 đ".
 *
 * VÌ SAO (lỗi H16, 25/09/2026)
 * ----------------------------
 * App định dạng tiền ở khoảng 20 chỗ theo bốn kiểu: `String.format("%,d")`,
 * `new DecimalFormat("#,###")`, `NumberFormat(Locale.GERMANY)` và in số trần.
 * Ba kiểu đầu phụ thuộc NGÔN NGỮ CỦA MÁY: máy đặt tiếng Anh thì ra dấu phẩy.
 * Kết quả trên cùng một màn: dòng món "55.000", tổng "110,000", thống kê
 * "56,250,000 VNĐ" cạnh "240000 VNĐ". Ở quầy thu ngân, dấu phẩy còn dễ đọc
 * nhầm thành phần thập phân.
 *
 * Ở đây ghi cứng dấu nhóm là dấu chấm theo cách viết của người Việt, không
 * theo ngôn ngữ máy, và dùng một đơn vị duy nhất là "đ".
 */
public final class TienTe {

    private TienTe() { }

    /** Bộ định dạng "110.000" — dùng thay cho `new DecimalFormat("#,###")`. */
    public static DecimalFormat dinhDang() {
        DecimalFormatSymbols kyHieu = new DecimalFormatSymbols(Locale.ROOT);
        kyHieu.setGroupingSeparator('.');
        kyHieu.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", kyHieu);
    }

    /** "110.000" */
    public static String so(long soTien) {
        return dinhDang().format(soTien);
    }

    /** "110.000" từ chuỗi máy chủ trả ("110000", "110000.00"); chuỗi lạ thì trả nguyên. */
    public static String so(String soTien) {
        if (soTien == null) return "0";
        try {
            return so(Math.round(Double.parseDouble(soTien.trim())));
        } catch (NumberFormatException e) {
            return soTien;
        }
    }

    /** "110.000 đ" */
    public static String dong(long soTien) {
        return so(soTien) + " đ";
    }

    /** "110.000 đ" từ chuỗi máy chủ trả. */
    public static String dong(String soTien) {
        return so(soTien) + " đ";
    }
}
