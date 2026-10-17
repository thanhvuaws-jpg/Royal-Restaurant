package com.sinhvien.orderdrinkapp.Utils;

/**
 * Utility class để chống bấm double click (spam click) vào các nút quan trọng.
 * Cách dùng:
 *   btn.setOnClickListener(v -> {
 *       if (ViewUtils.isFastDoubleClick()) return;
 *       // ... xử lý logic
 *   });
 */
public class ViewUtils {

    private static final long CLICK_COOLDOWN_MS = 800; // Thời gian chờ giữa 2 lần bấm (ms)
    private static long lastClickTime = 0;

    /**
     * Kiểm tra xem có phải đang bấm double click không.
     * @return true nếu bấm quá nhanh (cần bỏ qua), false nếu bấm hợp lệ
     */
    public static boolean isFastDoubleClick() {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_COOLDOWN_MS) {
            return true; // Bấm quá nhanh → bỏ qua
        }
        lastClickTime = now;
        return false;
    }

    /**
     * Chuyển đổi đường dẫn hình ảnh thô (raw path) từ Database hoặc API thành URL đầy đủ để tải ảnh.
     * Hỗ trợ tự động phân tích:
     * - Nếu đường dẫn trống hoặc null: Trả về chuỗi rỗng.
     * - Nếu đường dẫn bắt đầu bằng http:// hoặc https://: Trả về chính đường dẫn đó.
     * - Nếu là đường dẫn tương đối (relative path): Ghép nối với URL gốc của API Client (ApiClient.getBaseUrl()) để tạo URL hoàn chỉnh.
     *
     * @param rawPath Đường dẫn hình ảnh thô từ máy chủ hoặc dữ liệu cục bộ.
     * @return URL hình ảnh đầy đủ để hiển thị thông qua thư viện tải ảnh (Glide, Picasso,...).
     */
    public static String getImageUrl(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return "";
        }
        if (rawPath.startsWith("http://") || rawPath.startsWith("https://")) {
            return rawPath;
        }
        return com.sinhvien.orderdrinkapp.Api.ApiClient.getBaseUrl() + rawPath;
    }

    /**
     * Chữ thường, bỏ dấu tiếng Việt: "Phở Bò" -> "pho bo". Dùng để tìm kiếm
     * trong máy mà gõ có dấu hay không dấu đều ra — giống cách MySQL
     * (collation _ai_ci) so khi tìm trên máy chủ.
     */
    public static String boDau(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(java.util.Locale.ROOT)
                .trim();
    }

    /**
     * Lý do máy chủ đưa ra khi từ chối yêu cầu (HTTP 4xx/5xx).
     *
     * Retrofit KHÔNG đọc thân phản hồi lỗi vào response.body() — với mã
     * 409 thì body() luôn null và lý do thật ("Bàn đã có khách đặt từ 18:00
     * đến 20:00…") nằm trong errorBody(). Đọc body() như trước là lần nào
     * cũng hiện câu chung chung, người dùng không biết phải làm gì tiếp.
     *
     * errorBody() chỉ đọc được MỘT lần, nên gọi hàm này đúng một lần cho
     * mỗi phản hồi.
     */
    public static String docLoiMayChu(retrofit2.Response<?> response, String macDinh) {
        if (response != null && response.errorBody() != null) {
            try {
                org.json.JSONObject o = new org.json.JSONObject(response.errorBody().string());
                String m = o.optString("message", "");
                if (!m.isEmpty()) return m;
            } catch (Exception ignored) { }
        }
        return macDinh;
    }

    /**
     * Xóa thông báo lỗi của ô nhập ngay khi người dùng gõ lại (H22).
     *
     * Các form kiểm tra khi bấm Lưu và đặt setError(). Trước đây lỗi chỉ được
     * xóa ở lần bấm Lưu kế tiếp, nên đã nhập đúng rồi mà dòng đỏ "Không được
     * để trống!" vẫn nằm đó, như thể ô vẫn còn sai.
     */
    public static void xoaLoiKhiGo(com.google.android.material.textfield.TextInputLayout... cacO) {
        for (com.google.android.material.textfield.TextInputLayout o : cacO) {
            if (o == null || o.getEditText() == null) continue;
            o.getEditText().addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(android.text.Editable e) {
                    if (o.getError() != null) {
                        o.setError(null);
                        o.setErrorEnabled(false);
                    }
                }
            });
        }
    }

    /** Ba kiểu nhãn trạng thái của bộ Sơn Son (Figma .huy-hieu). */
    public static final int NHAN_CHO = 0, NHAN_XONG = 1, NHAN_TRE = 2;

    /**
     * Tô một TextView thành nhãn trạng thái bộ Sơn Son: viên nền nhạt, viền và chữ cùng màu,
     * chấm tròn phía trước. CHỜ = hổ phách, XONG = ngọc, TRỄ/HỦY = sơn son.
     * Dùng chung cho đơn, lịch hẹn, bàn, kho… để mọi màn cùng một kiểu nhãn.
     */
    public static void datNhanTrangThai(android.widget.TextView tv, int kieu) {
        if (tv == null) return;
        android.content.Context c = tv.getContext();
        int nen, mau;
        switch (kieu) {
            case NHAN_XONG: nen = com.sinhvien.orderdrinkapp.R.drawable.nen_hh_xong; mau = 0xFF2E6B57; break;
            case NHAN_TRE:  nen = com.sinhvien.orderdrinkapp.R.drawable.nen_hh_tre;  mau = 0xFF8E1B17; break;
            default:        nen = com.sinhvien.orderdrinkapp.R.drawable.nen_hh_cho;  mau = 0xFFA86A12; break;
        }
        tv.setBackgroundResource(nen);
        tv.setTextColor(mau);
        tv.setAllCaps(false);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        android.graphics.drawable.Drawable cham = androidx.core.content.ContextCompat.getDrawable(
                c, com.sinhvien.orderdrinkapp.R.drawable.cham_hh);
        if (cham != null) {
            cham = androidx.core.graphics.drawable.DrawableCompat.wrap(cham.mutate());
            androidx.core.graphics.drawable.DrawableCompat.setTint(cham, mau);
        }
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(cham, null, null, null);
        float d = c.getResources().getDisplayMetrics().density;
        tv.setCompoundDrawablePadding(Math.round(5 * d));
        tv.setPaddingRelative(Math.round(9 * d), Math.round(3 * d), Math.round(10 * d), Math.round(3 * d));
    }

    /**
     * Chữ viết tắt cho huy hiệu ảnh đại diện đầu menu bên: chữ đầu của HAI từ đầu,
     * như mẫu Figma ("Quản lý sân thử" → "QL", "Khách Thử" → "KT"). Tên một từ thì
     * lấy một chữ; tên rỗng thì "R" (Royal).
     */
    public static String vietTat(String hoTen) {
        if (hoTen == null) return "R";
        String[] tu = hoTen.trim().split("\\s+");
        StringBuilder kq = new StringBuilder();
        for (String t : tu) {
            if (t.isEmpty()) continue;
            kq.appendCodePoint(t.codePointAt(0));
            if (kq.codePointCount(0, kq.length()) == 2) break;
        }
        return kq.length() == 0 ? "R" : kq.toString().toUpperCase(new java.util.Locale("vi"));
    }
}
