package com.sinhvien.orderdrinkapp.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.sinhvien.orderdrinkapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tiện ích xử lý hóa đơn: chụp layout thành ảnh và chia sẻ.
 */
public class ReceiptHelper {

    /**
     * Chụp một View thành Bitmap (dùng để xem trước hoặc lưu/chia sẻ).
     */
    public static Bitmap captureView(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) {
            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            width = view.getMeasuredWidth();
            height = view.getMeasuredHeight();
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        if (view.getBackground() != null) {
            view.getBackground().draw(canvas);
        } else {
            canvas.drawColor(android.graphics.Color.WHITE);
        }
        view.draw(canvas);
        return bitmap;
    }

    /**
     * Lưu Bitmap vào thư viện ảnh của thiết bị.
     * Tương thích cả Android 9 trở xuống và Android 10 trở lên.
     * @return Uri của ảnh đã lưu, null nếu thất bại.
     */
    public static Uri saveBitmapToGallery(Context context, Bitmap bitmap) {
        String fileName = "HoaDon_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date())
                + ".png";

        OutputStream outputStream;
        Uri imageUri;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+): dùng MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/RoyalReceipts");

                imageUri = context.getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri == null) return null;

                outputStream = context.getContentResolver().openOutputStream(imageUri);
            } else {
                // Android 9 trở xuống: lưu trực tiếp vào file
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "RoyalReceipts");
                if (!dir.exists()) dir.mkdirs();

                File file = new File(dir, fileName);
                outputStream = new FileOutputStream(file);
                imageUri = Uri.fromFile(file);

                // Quét vào thư viện
                context.sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri));
            }

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();

            Toast.makeText(context,
                    context.getString(R.string.receipt_save) + " thành công!",
                    Toast.LENGTH_SHORT).show();
            return imageUri;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Lưu thất bại: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * Chia sẻ Bitmap qua các ứng dụng (Zalo, Gmail, v.v.)
     */
    public static void shareBitmap(Context context, Bitmap bitmap) {
        try {
            Uri uri = saveBitmapToGallery(context, bitmap);
            if (uri == null) return;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                    context.getString(R.string.receipt_title));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent,
                    context.getString(R.string.receipt_share)));
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }
}
