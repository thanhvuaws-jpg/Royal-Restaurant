package com.sinhvien.orderdrinkapp.KhuonMat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.List;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

/**
 * MoHinhKhuonMat — bộ máy nhận diện khuôn mặt chạy ngay trên điện thoại (QĐ-103).
 *
 * Chuyển từ thư mục `engine/` của dự án xử lý ảnh (SLAVATHIGIACMT), giữ nguyên
 * các con số nhóm đã đo:
 *
 *   khung hình ─► MediaPipe Face Landmarker (478 điểm mốc)
 *              ─► 5 điểm: tâm hai tròng mắt, chóp mũi, hai khóe miệng
 *              ─► căn chỉnh similarity về mẫu ArcFace 112×112   (engine/align.py)
 *              ─► MobileFaceNet nhóm tự huấn luyện, ONNX         (engine/embed/onnx_arcface.py)
 *              ─► vector 512 chiều, chuẩn hóa L2
 *
 * Dự án gốc lấy 5 điểm bằng YuNet; ở đây lấy từ MediaPipe vì cùng bộ điểm đó còn
 * dùng để đo chớp mắt (engine/liveness/blink.py), khỏi chạy hai mô hình. Đã đo
 * trên 504 ảnh LFW trước khi làm: EER 2,61 % (MediaPipe) so với 2,38 % (YuNet),
 * cosine giữa hai cách căn chỉnh trên cùng một ảnh trung bình 0,982.
 *
 * Không an toàn khi gọi từ nhiều luồng: mỗi màn hình dùng một luồng phân tích.
 */
public final class MoHinhKhuonMat implements AutoCloseable {

    static final String TEP_DIEM_MOC = "khuon_mat/face_landmarker.task";
    static final String TEP_DAC_TRUNG = "khuon_mat/mobilefacenet_distill_casia.onnx";

    /** Mẫu 5 điểm ArcFace cho ảnh 112×112 — engine/types.py REF_LANDMARKS_112. */
    private static final float[][] MAU_112 = {
            {38.2946f, 51.6963f}, {73.5318f, 51.5014f}, {56.0252f, 71.7366f},
            {41.5493f, 92.3655f}, {70.7299f, 92.2041f}};
    private static final int CO_CHIP = 112;

    /** Chỉ số điểm mốc mắt trong lưới 478 điểm — engine/liveness/blink.py. */
    private static final int[] MAT_TRAI = {362, 385, 387, 263, 373, 380};
    private static final int[] MAT_PHAI = {33, 160, 158, 133, 153, 144};

    /** Mặt nhỏ hơn mức này thì không đo nổi chớp mắt (blink.py CO_MAT_TOI_THIEU). */
    static final int CO_MAT_TOI_THIEU = 90;
    /** Quay ngang tối đa — căn chỉnh 2D không dựng lại được má bị khuất (engine/settings.py max_pose_angle). */
    static final float YAW_TOI_DA = 30f;
    /** Ngưỡng sáng trên vùng mặt — engine/settings.py QualitySettings. */
    static final float SANG_TOI_THIEU = 45f, SANG_TOI_DA = 215f;
    /** Hệ số quy đổi lệch mũi sang độ — engine/align.py YAW_RATIO_TO_DEG. */
    private static final float YAW_HE_SO = 130f;

    private final FaceLandmarker diemMoc;
    private final OrtEnvironment moiTruong;
    private final OrtSession phien;
    private final String tenDauVao;
    private long tGanNhat = -1;

    /** Một khuôn mặt tìm được trong khung hình. Tọa độ tính bằng điểm ảnh của khung. */
    public static final class KhuonMat {
        public PointF[] namDiem;     // mắt trái-ảnh, mắt phải-ảnh, mũi, miệng trái-ảnh, miệng phải-ảnh
        public RectF hop;
        public float ear;            // trung bình hai mắt
        public float yaw;            // độ
        public float doSang;         // 0–255
        public String loi;           // null = đạt chất lượng để trích đặc trưng

        public boolean datChatLuong() { return loi == null; }
        public float coMat() { return Math.min(hop.width(), hop.height()); }
    }

    public MoHinhKhuonMat(Context ctx) throws Exception {
        BaseOptions coBan = BaseOptions.builder().setModelAssetPath(TEP_DIEM_MOC).build();
        FaceLandmarker.FaceLandmarkerOptions tuyChon = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(coBan)
                .setRunningMode(RunningMode.VIDEO)
                .setNumFaces(2)                     // để phát hiện "có hai người trong khung"
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build();
        diemMoc = FaceLandmarker.createFromOptions(ctx, tuyChon);

        moiTruong = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions so = new OrtSession.SessionOptions();
        so.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        so.setIntraOpNumThreads(2);
        phien = moiTruong.createSession(docAsset(ctx, TEP_DAC_TRUNG), so);
        tenDauVao = phien.getInputNames().iterator().next();
    }

    private static byte[] docAsset(Context ctx, String ten) throws IOException {
        try (InputStream in = ctx.getAssets().open(ten); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    /**
     * Tìm khuôn mặt trong một khung hình của luồng video.
     *
     * @return null nếu không thấy mặt nào; nếu thấy từ hai mặt trở lên thì trả mặt
     *         lớn nhất nhưng gắn lỗi "chỉ một người" — đăng nhập không được dựa vào
     *         khung hình có người thứ hai.
     */
    public KhuonMat phanTich(Bitmap anh, long tMs) {
        // Chế độ VIDEO đòi mốc thời gian tăng dần nghiêm ngặt.
        if (tMs <= tGanNhat) tMs = tGanNhat + 1;
        tGanNhat = tMs;

        MPImage mp = new BitmapImageBuilder(anh).build();
        FaceLandmarkerResult kq = diemMoc.detectForVideo(mp, tMs);
        List<List<NormalizedLandmark>> cacMat = kq.faceLandmarks();
        if (cacMat.isEmpty()) return null;

        int w = anh.getWidth(), h = anh.getHeight();
        float[][] best = null;
        float bestCo = -1;
        for (List<NormalizedLandmark> lm : cacMat) {
            float[][] p = new float[lm.size()][2];
            float minX = 1e9f, minY = 1e9f, maxX = -1e9f, maxY = -1e9f;
            for (int i = 0; i < lm.size(); i++) {
                p[i][0] = lm.get(i).x() * w;
                p[i][1] = lm.get(i).y() * h;
                minX = Math.min(minX, p[i][0]); maxX = Math.max(maxX, p[i][0]);
                minY = Math.min(minY, p[i][1]); maxY = Math.max(maxY, p[i][1]);
            }
            float co = Math.min(maxX - minX, maxY - minY);
            if (co > bestCo) { bestCo = co; best = p; }
        }

        KhuonMat km = new KhuonMat();
        float[][] p = best;
        float minX = 1e9f, minY = 1e9f, maxX = -1e9f, maxY = -1e9f;
        for (float[] d : p) {
            minX = Math.min(minX, d[0]); maxX = Math.max(maxX, d[0]);
            minY = Math.min(minY, d[1]); maxY = Math.max(maxY, d[1]);
        }
        km.hop = new RectF(minX, minY, maxX, maxY);

        // 5 điểm theo thứ tự mẫu ArcFace: xếp theo trục x của ẢNH (giống YuNet).
        PointF mat1 = trungBinh(p, 468, 473), mat2 = trungBinh(p, 473, 478);   // tâm hai tròng mắt
        PointF mieng1 = new PointF(p[61][0], p[61][1]), mieng2 = new PointF(p[291][0], p[291][1]);
        if (mat1.x > mat2.x) { PointF t = mat1; mat1 = mat2; mat2 = t; }
        if (mieng1.x > mieng2.x) { PointF t = mieng1; mieng1 = mieng2; mieng2 = t; }
        km.namDiem = new PointF[]{mat1, mat2, new PointF(p[1][0], p[1][1]), mieng1, mieng2};

        km.ear = (ear(p, MAT_TRAI) + ear(p, MAT_PHAI)) / 2f;
        km.yaw = uocLuongYaw(km.namDiem);
        km.doSang = doSang(anh, km.hop);

        if (cacMat.size() > 1) km.loi = "Chỉ một người trong khung hình";
        else if (km.coMat() < CO_MAT_TOI_THIEU) km.loi = "Đưa mặt lại gần hơn";
        else if (km.yaw > YAW_TOI_DA) km.loi = "Nhìn thẳng vào camera";
        else if (km.doSang < SANG_TOI_THIEU) km.loi = "Chỗ này tối quá, ra chỗ sáng hơn";
        else if (km.doSang > SANG_TOI_DA) km.loi = "Ánh sáng chói quá";
        return km;
    }

    private static PointF trungBinh(float[][] p, int tu, int den) {
        float x = 0, y = 0;
        for (int i = tu; i < den; i++) { x += p[i][0]; y += p[i][1]; }
        int n = den - tu;
        return new PointF(x / n, y / n);
    }

    /** EAR = (‖p2−p6‖ + ‖p3−p5‖) / (2·‖p1−p4‖) — blink.py eye_aspect_ratio. */
    private static float ear(float[][] p, int[] idx) {
        float ngang = kc(p[idx[0]], p[idx[3]]) + 1e-6f;
        return (kc(p[idx[1]], p[idx[5]]) + kc(p[idx[2]], p[idx[4]])) / (2f * ngang);
    }

    private static float kc(float[] a, float[] b) {
        return (float) Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    /** Yaw (độ) từ lệch mũi và miệng so với trục hai mắt — engine/align.py estimate_pose. */
    private static float uocLuongYaw(PointF[] d) {
        float ex = d[1].x - d[0].x, ey = d[1].y - d[0].y;
        float kcMat = (float) Math.hypot(ex, ey) + 1e-6f;
        float ax = ex / kcMat, ay = ey / kcMat;
        float gx = (d[0].x + d[1].x) / 2f, gy = (d[0].y + d[1].y) / 2f;
        float mx = (d[3].x + d[4].x) / 2f, my = (d[3].y + d[4].y) / 2f;
        float lechMui = ((d[2].x - gx) * ax + (d[2].y - gy) * ay) / kcMat;
        float lechMieng = ((mx - gx) * ax + (my - gy) * ay) / kcMat;
        return Math.abs(lechMui * 0.65f + lechMieng * 0.35f) * YAW_HE_SO;
    }

    /** Độ sáng trung bình (0–255) trên vùng mặt, lấy mẫu thưa cho nhanh. */
    private static float doSang(Bitmap anh, RectF hop) {
        int x0 = Math.max(0, (int) hop.left), y0 = Math.max(0, (int) hop.top);
        int x1 = Math.min(anh.getWidth() - 1, (int) hop.right), y1 = Math.min(anh.getHeight() - 1, (int) hop.bottom);
        if (x1 <= x0 || y1 <= y0) return 0;
        int buoc = Math.max(1, Math.min(x1 - x0, y1 - y0) / 24);
        double tong = 0;
        int n = 0;
        for (int y = y0; y <= y1; y += buoc) {
            for (int x = x0; x <= x1; x += buoc) {
                int c = anh.getPixel(x, y);
                tong += 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c);
                n++;
            }
        }
        return n == 0 ? 0 : (float) (tong / n);
    }

    /**
     * Vector đặc trưng 512 chiều (đã chuẩn hóa L2) của khuôn mặt.
     * Tiền xử lý đúng như engine/embed/onnx_arcface.py: RGB, 112×112, (x − 127,5) / 127,5.
     */
    public float[] dacTrung(Bitmap anh, PointF[] namDiem) throws Exception {
        Bitmap chip = canChinh(anh, namDiem);
        int[] px = new int[CO_CHIP * CO_CHIP];
        chip.getPixels(px, 0, CO_CHIP, 0, 0, CO_CHIP, CO_CHIP);
        chip.recycle();
        int mat = CO_CHIP * CO_CHIP;
        FloatBuffer fb = FloatBuffer.allocate(3 * mat);
        float[] v = fb.array();
        for (int i = 0; i < mat; i++) {
            int c = px[i];
            v[i] = (((c >> 16) & 0xFF) - 127.5f) / 127.5f;           // R
            v[mat + i] = (((c >> 8) & 0xFF) - 127.5f) / 127.5f;      // G
            v[2 * mat + i] = ((c & 0xFF) - 127.5f) / 127.5f;         // B
        }
        try (OnnxTensor vao = OnnxTensor.createTensor(moiTruong, fb, new long[]{1, 3, CO_CHIP, CO_CHIP});
             OrtSession.Result ra = phien.run(Collections.singletonMap(tenDauVao, vao))) {
            float[] e = ((float[][]) ra.get(0).getValue())[0];
            return chuanHoa(e);
        }
    }

    /**
     * Căn chỉnh: phép biến đổi similarity (xoay + tỉ lệ đều + tịnh tiến) bình
     * phương tối thiểu đưa 5 điểm về mẫu ArcFace, rồi vẽ lại 112×112 có nội suy
     * song tuyến, nền đen — tương đương cv2.estimateAffinePartial2D + warpAffine
     * ở engine/align.py.
     */
    static Bitmap canChinh(Bitmap anh, PointF[] d) {
        float sx = 0, sy = 0, dx = 0, dy = 0;
        for (int i = 0; i < 5; i++) { sx += d[i].x; sy += d[i].y; dx += MAU_112[i][0]; dy += MAU_112[i][1]; }
        sx /= 5; sy /= 5; dx /= 5; dy /= 5;
        double tu = 0, tb = 0, mau = 0;
        for (int i = 0; i < 5; i++) {
            double x = d[i].x - sx, y = d[i].y - sy;
            double u = MAU_112[i][0] - dx, w = MAU_112[i][1] - dy;
            tu += x * u + y * w;
            tb += x * w - y * u;
            mau += x * x + y * y;
        }
        float a = (float) (tu / mau), b = (float) (tb / mau);
        float tx = dx - (a * sx - b * sy), ty = dy - (b * sx + a * sy);
        Matrix m = new Matrix();
        m.setValues(new float[]{a, -b, tx, b, a, ty, 0, 0, 1});

        Bitmap chip = Bitmap.createBitmap(CO_CHIP, CO_CHIP, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(chip);
        c.drawColor(Color.BLACK);
        c.drawBitmap(anh, m, new Paint(Paint.FILTER_BITMAP_FLAG));
        return chip;
    }

    static float[] chuanHoa(float[] v) {
        double s = 0;
        for (float x : v) s += x * x;
        float n = (float) Math.sqrt(s) + 1e-9f;
        float[] r = new float[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i] / n;
        return r;
    }

    /** Cosine của hai vector đã chuẩn hóa. */
    public static float cosine(float[] a, float[] b) {
        float s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    @Override
    public void close() {
        try { diemMoc.close(); } catch (Exception ignored) { }
        try { phien.close(); } catch (Exception ignored) { }
    }
}
