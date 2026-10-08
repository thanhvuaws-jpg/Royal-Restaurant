package com.sinhvien.orderdrinkapp.KhuonMat;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;

/**
 * KiemTraChopMat — chống giả mạo bằng chớp mắt, chuyển từ engine/liveness/blink.py
 * của dự án xử lý ảnh (lớp _FaceState), giữ nguyên các ngưỡng nhóm đã đo.
 *
 * Vì sao chớp mắt: nhóm đã đo bằng tấn công thật (điện thoại hiển thị ảnh chân
 * dung, 60 khung mỗi loại) — phân tích kết cấu ảnh cho ACER 50–70 %, chớp mắt cho
 * ACER 0 %. Một tấm ảnh không thể chớp mắt; đó là chuyện vật lý, không phải chất
 * lượng in. Chớp mắt cần CHUỖI khung hình nên chỉ làm được ở nơi có camera chạy
 * liên tục — chính là điện thoại, không phải máy chủ.
 *
 * Cách đo: EAR của hai mắt qua từng khung. Ngưỡng tính theo TỈ LỆ của đường nền
 * (EAR lúc mắt mở, phân vị 80) của chính người đó, vì EAR không bất biến theo cỡ
 * mặt trên camera độ phân giải thấp. Chỉ tính một cái chớp khi đi đủ
 * mở → nhắm → mở: ảnh chụp người đang nhắm mắt cũng cho EAR thấp mãi mãi.
 *
 * Khác bản gốc ở một chỗ: bản gốc theo dõi nhiều mặt (máy kiosk); ở đây chỉ một
 * mặt, và mất mặt quá SLOT_TIMEOUT thì xóa sạch bằng chứng — ngăn kẻ gian chớp
 * mắt thật rồi giơ ảnh người khác vào đúng chỗ đó.
 */
public final class KiemTraChopMat {

    static final float TI_LE_NHAM = 0.72f;        // tụt dưới 72 % đường nền -> đang nhắm
    static final float TI_LE_MO = 0.85f;          // bật lại trên 85 % đường nền -> đã mở
    static final float EAR_SAN_TOI_THIEU = 0.06f; // dưới mức này là đo hỏng, không phải nhắm
    static final long SLOT_TIMEOUT_MS = 600;
    static final int SO_KHUNG_XET_DUNG_IM = 45;   // ~1,5 giây ở 30 khung/giây
    static final float NGUONG_MAT_DUNG_IM = 0.020f;
    static final float DAO_DONG_RO_RANG = 0.060f;
    static final float CHO_TOI_THIEU_S = 3f, CHO_CHOP_MAT_S = 12f;
    static final float BLINK_VALID_S = 15f;

    private final ArrayDeque<Float> ear = new ArrayDeque<>();
    private boolean dangNham = false;
    private int soLanChop = 0;
    private long lanChopCuoi = 0, thayLanDau = 0, thayLanCuoi = 0;

    /** Gọi cho mỗi khung CÓ mặt. */
    public void capNhat(float e, long tMs) {
        if (thayLanCuoi != 0 && tMs - thayLanCuoi > SLOT_TIMEOUT_MS) datLai();
        if (thayLanDau == 0) thayLanDau = tMs;
        thayLanCuoi = tMs;

        ear.addLast(e);
        while (ear.size() > 90) ear.removeFirst();
        float nen = duongNen();
        if (nen < EAR_SAN_TOI_THIEU) return;
        if (!dangNham && e < nen * TI_LE_NHAM) {
            dangNham = true;
        } else if (dangNham && e > nen * TI_LE_MO) {
            dangNham = false;
            soLanChop++;
            lanChopCuoi = tMs;
        }
    }

    /** Gọi cho mỗi khung KHÔNG có mặt: quá SLOT_TIMEOUT thì quên hết. */
    public void khongThayMat(long tMs) {
        if (thayLanCuoi != 0 && tMs - thayLanCuoi > SLOT_TIMEOUT_MS) datLai();
    }

    public void datLai() {
        ear.clear();
        dangNham = false;
        soLanChop = 0;
        lanChopCuoi = thayLanDau = thayLanCuoi = 0;
    }

    /** EAR lúc mắt MỞ của người này — phân vị 80 (đỉnh dễ bị một khung nhiễu kéo lên). */
    float duongNen() {
        if (ear.size() < 12) return 0f;
        float[] a = new float[ear.size()];
        int i = 0;
        for (Float f : ear) a[i++] = f;
        Arrays.sort(a);
        float vt = 0.8f * (a.length - 1);
        int lo = (int) Math.floor(vt), hi = (int) Math.ceil(vt);
        return a[lo] + (a[hi] - a[lo]) * (vt - lo);
    }

    /** Hệ số biến thiên (độ lệch chuẩn / trung bình) của EAR trong ~1,5 giây gần nhất; -1 nếu chưa đủ mẫu. */
    float heSoBienThien() {
        if (ear.size() < SO_KHUNG_XET_DUNG_IM) return -1f;
        float[] a = new float[SO_KHUNG_XET_DUNG_IM];
        Iterator<Float> it = ear.descendingIterator();
        for (int i = 0; i < a.length; i++) a[i] = it.next();
        double tb = 0;
        for (float x : a) tb += x;
        tb /= a.length;
        if (tb < EAR_SAN_TOI_THIEU) return -1f;
        double ps = 0;
        for (float x : a) ps += (x - tb) * (x - tb);
        return (float) (Math.sqrt(ps / a.length) / tb);
    }

    /** Mắt gần như không đổi -> nhiều khả năng là ảnh tĩnh. */
    public boolean matDungIm() {
        float cv = heSoBienThien();
        return cv >= 0 && cv < NGUONG_MAT_DUNG_IM;
    }

    /** Thời gian chờ chớp mắt, co giãn theo mức dao động của mắt (3–12 giây). */
    float thoiGianChoS() {
        float cv = heSoBienThien();
        if (cv < 0) return CHO_CHOP_MAT_S;
        float tiLe = Math.min(Math.max(cv / DAO_DONG_RO_RANG, 0f), 1f);
        return CHO_TOI_THIEU_S + (CHO_CHOP_MAT_S - CHO_TOI_THIEU_S) * tiLe;
    }

    /**
     * 1 = vừa chớp mắt (trong 15 giây); -1 = đang kiểm tra, chưa kết luận;
     * 0 = đã chờ đủ mà không thấy chớp -> coi là giả. Giảm dần khi bằng chứng cũ.
     */
    public float diem(long tMs) {
        if (thayLanDau == 0) return -1f;
        if (soLanChop == 0) {
            return (tMs - thayLanDau) / 1000f < thoiGianChoS() ? -1f : 0f;
        }
        float tuoi = (tMs - lanChopCuoi) / 1000f;
        if (tuoi <= BLINK_VALID_S) return 1f;
        return Math.max(0f, 1f - (tuoi - BLINK_VALID_S) / BLINK_VALID_S);
    }

    public int soLanChop() { return soLanChop; }
}
