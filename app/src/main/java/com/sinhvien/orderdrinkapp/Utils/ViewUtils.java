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
}
