package com.sinhvien.orderdrinkapp.KhuonMat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

/**
 * Chạy bộ máy nhận diện THẬT trên thiết bị (MediaPipe + ONNX Runtime, đúng hai mô
 * hình trong assets của app) với bốn ảnh LFW công khai:
 *   a1, a2 — cùng một người; b1, c1 — hai người khác.
 *
 *     gradlew connectedDebugAndroidTest
 *
 * Ghi vector ra files/khuon_mat_thu.txt để đối chiếu với bản Python của dự án gốc.
 */
@RunWith(AndroidJUnit4.class)
public class MoHinhKhuonMatThietBiTest {

    private static final String TAG = "KhuonMatThu";

    private Bitmap docAnh(String ten) throws Exception {
        Context thu = InstrumentationRegistry.getInstrumentation().getContext();
        try (InputStream in = thu.getAssets().open("khuon_mat_thu/" + ten)) {
            return BitmapFactory.decodeStream(in);
        }
    }

    /** Mỗi ảnh một bộ máy mới: chế độ VIDEO giả định các khung liên tiếp của cùng một cảnh. */
    private float[] vector(Context app, String ten, StringBuilder ghi) throws Exception {
        Bitmap anh = docAnh(ten);
        try (MoHinhKhuonMat m = new MoHinhKhuonMat(app)) {
            MoHinhKhuonMat.KhuonMat km = m.phanTich(anh, 1000);
            assertNotNull("phải tìm thấy mặt trong " + ten, km);
            long t0 = System.nanoTime();
            float[] e = m.dacTrung(anh, km.namDiem);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            assertEquals(512, e.length);
            float chuan = 0;
            for (float x : e) chuan += x * x;
            assertEquals("vector đã chuẩn hóa L2", 1f, chuan, 1e-3f);
            Log.i(TAG, ten + ": mặt " + (int) km.coMat() + " px, yaw " + km.yaw + "°, EAR " + km.ear
                    + ", trích đặc trưng " + ms + " ms");
            ghi.append(ten);
            for (float x : e) ghi.append(' ').append(x);
            ghi.append('\n');
            return e;
        }
    }

    @Test
    public void cungNguoiGanHon_khacNguoiXaHon() throws Exception {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        StringBuilder ghi = new StringBuilder();
        float[] a1 = vector(app, "a1.jpg", ghi);
        float[] a2 = vector(app, "a2.jpg", ghi);
        float[] b1 = vector(app, "b1.jpg", ghi);
        float[] c1 = vector(app, "c1.jpg", ghi);

        float cung = MoHinhKhuonMat.cosine(a1, a2);
        float khac1 = MoHinhKhuonMat.cosine(a1, b1);
        float khac2 = MoHinhKhuonMat.cosine(a1, c1);
        Log.i(TAG, "cosine cùng người " + cung + "; khác người " + khac1 + ", " + khac2);
        try (FileWriter w = new FileWriter(new File(app.getExternalFilesDir(null), "khuon_mat_thu.txt"))) {
            w.write(ghi.toString());
        }
        assertTrue("cùng người phải giống hơn khác người", cung > khac1 + 0.2f && cung > khac2 + 0.2f);
        assertTrue("khác người dưới ngưỡng đăng nhập 0,65", khac1 < DangNhapKhuonMatActivity.NGUONG_KHOP
                && khac2 < DangNhapKhuonMatActivity.NGUONG_KHOP);
    }
}
