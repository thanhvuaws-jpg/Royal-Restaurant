package com.sinhvien.orderdrinkapp.SanChoi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.sinhvien.orderdrinkapp.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * HieuUngView — lớp phủ toàn màn cho Sân chơi: pháo giấy, tia lửa và đồng xu
 * bay về ví. Không nhận chạm (để bấm xuyên qua).
 *
 * Mô phỏng theo từng khung hình (vận tốc, trọng lực, lực cản) giống bản demo
 * web; đơn vị trong công thức là dp rồi nhân mật độ màn hình.
 *
 * ĐỒNG XU: tung lên theo trọng lực, rồi bay theo đường cong Bezier bậc hai về
 * vị trí ví; đồng xu tự "lật" bằng cách co chiều ngang theo |cos|. Mỗi đồng
 * tới ví gọi KhiXuToi để ví nhích số và kêu "ting".
 */
public class HieuUngView extends View {

    public interface KhiXuToi { void onXuToi(); }

    private static final int[] MAU_GIAY = {
            Color.parseColor("#FFE6A3"), Color.parseColor("#E3B34F"), Color.parseColor("#C41E1E"),
            Color.parseColor("#F7ECD6"), Color.parseColor("#FF8A3D"), Color.parseColor("#8F1414")};

    private final float dp;
    private final Random rnd = new Random();
    private final Paint pGiay = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLua = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pXu = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF oXu = new RectF();
    private final Bitmap anhXu;

    private final List<Hat> hat = new ArrayList<>();
    private final List<Xu> xu = new ArrayList<>();

    private static class Hat {
        boolean giay;
        float x, y, vx, vy, xoay, vxoay, rong, cao, r;
        int mau, song, tho;
    }

    private static class Xu {
        float x, y, vx, vy, lat, tocLat, tuX, tuY, denX, denY;
        long batDau, tungMs, bayMs;
        boolean daToi, coDiemTung;
        KhiXuToi khiToi;
    }

    public HieuUngView(Context context) { this(context, null); }

    public HieuUngView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        anhXu = BitmapFactory.decodeResource(getResources(), R.drawable.mg_dong_xu);
        setClickable(false);
        setFocusable(false);
    }

    /** Pháo giấy bắn từ (x, y) theo tọa độ của view này. */
    public void phaoGiay(float x, float y, int so, float luc) {
        for (int i = 0; i < so; i++) {
            double g = -Math.PI / 2 + (rnd.nextDouble() - .5) * 2.2;
            float v = (6 + rnd.nextFloat() * 9) * luc;
            Hat h = new Hat();
            h.giay = true;
            h.x = x; h.y = y;
            h.vx = (float) Math.cos(g) * v; h.vy = (float) Math.sin(g) * v;
            h.xoay = rnd.nextFloat() * 6; h.vxoay = (rnd.nextFloat() - .5f) * .4f;
            h.rong = 6 + rnd.nextFloat() * 6; h.cao = 3 + rnd.nextFloat() * 4;
            h.mau = MAU_GIAY[i % MAU_GIAY.length];
            h.tho = 150 + rnd.nextInt(60);
            hat.add(h);
        }
        postInvalidateOnAnimation();
    }

    /** Tia lửa nhỏ xì ra quanh (x, y) — dùng lúc rương đang rung. */
    public void tiaLua(float x, float y, int so) {
        for (int i = 0; i < so; i++) {
            double g = -Math.PI / 2 + (rnd.nextDouble() - .5) * 1.6;
            float v = 2 + rnd.nextFloat() * 4;
            Hat h = new Hat();
            h.giay = false;
            h.x = x + (rnd.nextFloat() - .5f) * 120 * dp; h.y = y;
            h.vx = (float) Math.cos(g) * v; h.vy = (float) Math.sin(g) * v;
            h.tho = 30 + rnd.nextInt(25);
            h.r = 1.5f + rnd.nextFloat() * 2;
            hat.add(h);
        }
        postInvalidateOnAnimation();
    }

    /**
     * `so` đồng xu tung lên từ (tuX, tuY) rồi bay về (denX, denY).
     * `tung` > 1 tung mạnh hơn (mở rương), < 1 nhẹ hơn (nhận thưởng vòng quay).
     */
    public void xuBay(float tuX, float tuY, float denX, float denY, int so, float tung, KhiXuToi khiToi) {
        long bayGio = SystemClock.uptimeMillis();
        for (int i = 0; i < so; i++) {
            double g = -Math.PI / 2 + (rnd.nextDouble() - .5) * 2.4 * tung;
            float v = (5 + rnd.nextFloat() * 6) * tung;
            Xu c = new Xu();
            c.x = tuX; c.y = tuY;
            c.vx = (float) Math.cos(g) * v; c.vy = (float) Math.sin(g) * v;
            c.lat = rnd.nextFloat() * 6; c.tocLat = .25f + rnd.nextFloat() * .2f;
            c.denX = denX; c.denY = denY;
            c.batDau = bayGio + i * 45L;
            c.tungMs = 520 + rnd.nextInt(200);
            c.bayMs = 520;
            c.khiToi = khiToi;
            xu.add(c);
        }
        postInvalidateOnAnimation();
    }

    public boolean dangChay() { return !hat.isEmpty() || !xu.isEmpty(); }

    @Override
    protected void onDraw(Canvas canvas) {
        // Công thức gốc của demo tính theo px CSS; trên điện thoại 1 px CSS = 1 dp.
        float k = dp;

        /* Hạt: pháo giấy và tia lửa */
        Iterator<Hat> ih = hat.iterator();
        while (ih.hasNext()) {
            Hat p = ih.next();
            p.song++;
            if (p.song >= p.tho || p.y > getHeight() + 40 * dp) { ih.remove(); continue; }
            if (p.giay) {
                p.vx *= .985f;
                p.vy = p.vy * .985f + .22f;
                p.x += (p.vx + (float) Math.sin(p.song * .1) * .6f) * k;
                p.y += p.vy * k;
                p.xoay += p.vxoay;
                canvas.save();
                canvas.translate(p.x, p.y);
                canvas.rotate((float) Math.toDegrees(p.xoay));
                canvas.scale(1f, (float) Math.cos(p.song * .15));
                pGiay.setColor(p.mau);
                pGiay.setAlpha((int) (255 * Math.min(1f, (p.tho - p.song) / 30f)));
                canvas.drawRect(-p.rong * k / 2, -p.cao * k / 2, p.rong * k / 2, p.cao * k / 2, pGiay);
                canvas.restore();
            } else {
                p.vy += .05f;
                p.x += p.vx * k;
                p.y += p.vy * k;
                float a = 1f - (float) p.song / p.tho;
                float r = p.r * k * 2.2f;
                pLua.setShader(new RadialGradient(p.x, p.y, r,
                        new int[]{Color.argb((int) (255 * a), 0xFF, 0xE6, 0xA3),
                                  Color.argb((int) (120 * a), 0xFF, 0xB4, 0x3B), Color.TRANSPARENT},
                        new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawCircle(p.x, p.y, r, pLua);
            }
        }

        /* Đồng xu */
        long bayGio = SystemClock.uptimeMillis();
        float coXu = 34 * k;
        Iterator<Xu> ix = xu.iterator();
        while (ix.hasNext()) {
            Xu c = ix.next();
            long dt = bayGio - c.batDau;
            if (dt < 0) continue;
            float x, y, co = 1f;
            if (dt < c.tungMs) {
                c.vy += .32f; c.vx *= .98f;
                c.x += c.vx * k; c.y += c.vy * k;
                x = c.x; y = c.y;
            } else {
                if (!c.coDiemTung) { c.coDiemTung = true; c.tuX = c.x; c.tuY = c.y; }
                float t = Math.min(1f, (dt - c.tungMs) / (float) c.bayMs);
                float e = t * t * (3 - 2 * t);
                float dkX = (c.tuX + c.denX) / 2, dkY = Math.min(c.tuY, c.denY) - 120 * k;
                x = (1 - e) * (1 - e) * c.tuX + 2 * (1 - e) * e * dkX + e * e * c.denX;
                y = (1 - e) * (1 - e) * c.tuY + 2 * (1 - e) * e * dkY + e * e * c.denY;
                co = 1 - e * .35f;
                if (t >= 1f) {
                    ix.remove();
                    if (c.khiToi != null) c.khiToi.onXuToi();
                    continue;
                }
            }
            c.lat += c.tocLat;
            float sx = Math.max(.18f, Math.abs((float) Math.cos(c.lat))) * co;
            float half = coXu / 2;
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(sx, co);
            oXu.set(-half, -half, half, half);
            canvas.drawBitmap(anhXu, null, oXu, pXu);
            canvas.restore();
        }

        if (dangChay()) postInvalidateOnAnimation();
    }
}
