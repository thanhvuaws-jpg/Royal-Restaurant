package com.sinhvien.orderdrinkapp.KhuonMat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

/**
 * Kiểm logic chớp mắt (chuyển từ engine/liveness/blink.py) bằng chuỗi EAR giả lập,
 * 30 khung/giây. Chạy trên máy tính:  gradlew testDebugUnitTest
 */
public class KiemTraChopMatTest {

    private static final long KHUNG_MS = 33;
    private final Random rd = new Random(42);

    /** Mắt mở: EAR quanh 0,30 kèm rung nhẹ như mắt thật (hệ số biến thiên ~4 %). */
    private float matMo() { return 0.30f + (float) rd.nextGaussian() * 0.012f; }

    private long chay(KiemTraChopMat k, long t, int soKhung, float ear, boolean rung) {
        for (int i = 0; i < soKhung; i++) {
            k.capNhat(rung ? matMo() : ear, t);
            t += KHUNG_MS;
        }
        return t;
    }

    @Test
    public void nguoiThatChopMat_duocCongNhan() {
        KiemTraChopMat k = new KiemTraChopMat();
        long t = chay(k, 1000, 40, 0, true);              // ~1,3 s mắt mở
        assertEquals("chưa chớp thì đang kiểm tra", -1f, k.diem(t), 0f);
        for (float e : new float[]{0.22f, 0.14f, 0.10f, 0.12f, 0.20f}) { k.capNhat(e, t); t += KHUNG_MS; }
        t = chay(k, t, 10, 0, true);                       // mở lại
        assertEquals(1, k.soLanChop());
        assertEquals(1f, k.diem(t), 0f);
    }

    @Test
    public void anhChupMatNham_khongPhaiChopMat() {
        // Ảnh chụp người đang nhắm mắt: EAR thấp mãi, không bao giờ đi đủ mở → nhắm → mở.
        KiemTraChopMat k = new KiemTraChopMat();
        long t = chay(k, 1000, 200, 0.11f, false);
        assertEquals(0, k.soLanChop());
    }

    @Test
    public void anhTinh_matDungIm_bacBoSau3Giay() {
        KiemTraChopMat k = new KiemTraChopMat();
        long t = chay(k, 1000, 60, 0.29f, false);           // EAR hằng số như ảnh tĩnh
        assertTrue("ảnh tĩnh: mắt đứng im", k.matDungIm());
        assertEquals("sau 2 s vẫn đang chờ", -1f, k.diem(1000 + 2000), 0f);
        t = chay(k, t, 60, 0.29f, false);                   // thêm 2 s nữa (tổng ~4 s)
        assertEquals("quá thời gian chờ tối thiểu 3 s mà không chớp -> giả", 0f, k.diem(t), 0f);
    }

    @Test
    public void nguoiThatNgoiYen_duocChoLauHon() {
        KiemTraChopMat k = new KiemTraChopMat();
        long t = chay(k, 1000, 120, 0, true);               // ~4 s, mắt rung nhẹ, chưa chớp
        assertFalse("mắt người thật không đứng im", k.matDungIm());
        assertEquals("người thật chưa chớp vẫn được chờ", -1f, k.diem(t), 0f);
    }

    @Test
    public void matMatQuaLau_xoaBangChung() {
        // Chớp mắt thật rồi rời khỏi khung > 0,6 s: không được giữ bằng chứng cho mặt khác giơ vào.
        KiemTraChopMat k = new KiemTraChopMat();
        long t = chay(k, 1000, 40, 0, true);
        for (float e : new float[]{0.14f, 0.10f, 0.12f}) { k.capNhat(e, t); t += KHUNG_MS; }
        t = chay(k, t, 10, 0, true);
        assertEquals(1, k.soLanChop());
        t += 1000;
        k.khongThayMat(t);
        assertEquals(0, k.soLanChop());
        k.capNhat(0.29f, t + KHUNG_MS);
        assertEquals(-1f, k.diem(t + KHUNG_MS), 0f);
    }

    @Test
    public void bangChungCu_giamDan() {
        KiemTraChopMat k = new KiemTraChopMat();
        long t = chay(k, 1000, 40, 0, true);
        for (float e : new float[]{0.14f, 0.10f, 0.12f}) { k.capNhat(e, t); t += KHUNG_MS; }
        long tChop = t;                                     // khung mở mắt đầu tiên = lúc ghi nhận cái chớp
        t = chay(k, t, 10, 0, true);
        assertEquals(1, k.soLanChop());
        assertEquals(1f, k.diem(tChop + 14_000), 0f);       // còn trong 15 s
        assertEquals(0.5f, k.diem(tChop + 22_500), 0.01f);  // quá hạn 7,5 s -> còn một nửa
        assertEquals(0f, k.diem(tChop + 31_000), 0f);
    }
}
