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
