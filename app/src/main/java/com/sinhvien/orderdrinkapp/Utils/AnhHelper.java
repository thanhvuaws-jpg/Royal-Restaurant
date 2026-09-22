package com.sinhvien.orderdrinkapp.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * AnhHelper - Tiện ích nén ảnh và chuyển đổi Base64 cho chứng từ kho.
 * Đáp ứng yêu cầu 4.2 và 12.5:
 * "Nén ảnh xuống dung lượng tối đa 1 mê-ga-byte trước khi gửi lên máy chủ."
 */
public class AnhHelper {

    public static final int MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1 MB
    public static final int MAX_DIMENSION = 1600; // Chiều dài/rộng tối đa trước khi nén chất lượng

    /**
     * Nén Bitmap xuống dung lượng dưới 1MB (1,048,576 bytes) và trả về mảng bytes.
     */
    public static byte[] nenAnhToiDa1MB(Bitmap source) {
        if (source == null) return new byte[0];

        Bitmap workingBitmap = source;

        // Thu nhỏ kích thước ảnh nếu vượt quá MAX_DIMENSION (tránh tốn bộ nhớ)
        int width = workingBitmap.getWidth();
        int height = workingBitmap.getHeight();
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            float ratio = (float) width / (float) height;
            int newWidth, newHeight;
            if (ratio > 1) {
                newWidth = MAX_DIMENSION;
                newHeight = (int) (MAX_DIMENSION / ratio);
            } else {
                newHeight = MAX_DIMENSION;
                newWidth = (int) (MAX_DIMENSION * ratio);
            }
            workingBitmap = Bitmap.createScaledBitmap(workingBitmap, newWidth, newHeight, true);
        }

        int quality = 90;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

        // Vòng lặp hạ chất lượng JPEG nếu dung lượng vẫn > 1MB
        while (baos.size() > MAX_FILE_SIZE_BYTES && quality > 20) {
            baos.reset();
            quality -= 15;
            workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }

        // Nếu vẫn lớn hơn 1MB, tiếp tục thu nhỏ kích thước hình học
        while (baos.size() > MAX_FILE_SIZE_BYTES && workingBitmap.getWidth() > 300) {
            baos.reset();
            int newW = (int) (workingBitmap.getWidth() * 0.75);
            int newH = (int) (workingBitmap.getHeight() * 0.75);
            workingBitmap = Bitmap.createScaledBitmap(workingBitmap, newW, newH, true);
            workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }

        return baos.toByteArray();
    }

    /**
     * Nén ảnh và mã hóa sang chuỗi Base64 có tiền tố data:image/jpeg;base64,...
     * Phù hợp để gửi trực tiếp lên API kho_phieu.php (trường hinhanh_base64).
     */
    public static String nenVaChuyenBase64(Bitmap bitmap) {
        byte[] bytes = nenAnhToiDa1MB(bitmap);
        if (bytes == null || bytes.length == 0) return "";
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * Đọc Bitmap từ Uri của người dùng chọn từ Thư viện hoặc Camera.
     */
    public static Bitmap docBitmapTuUri(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            // Đọc kích thước trước để tránh OutOfMemory
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);

            int scale = 1;
            while (opts.outWidth / scale / 2 >= MAX_DIMENSION && opts.outHeight / scale / 2 >= MAX_DIMENSION) {
                scale *= 2;
            }

            // Đọc dữ liệu thật
            try (InputStream is2 = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.Options realOpts = new BitmapFactory.Options();
                realOpts.inSampleSize = scale;
                return BitmapFactory.decodeStream(is2, null, realOpts);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
