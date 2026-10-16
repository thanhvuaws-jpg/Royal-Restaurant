package com.sinhvien.orderdrinkapp.SanChoi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.sinhvien.orderdrinkapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * VongQuayView — bánh xe 12 ô của Sân chơi Royal, làm lại đúng bản demo web
 * (_nangcap/minigame/choi_thu/index.html) bằng Canvas.
 *
 * HÌNH HỌC (khớp ảnh mg_vong_quay.webp)
 *   - Ô i chiếm góc [30i, 30i + 30) tính từ đỉnh theo chiều kim đồng hồ, vách
 *     vàng ở bội số 30°.
 *   - Nhãn ô đặt ở 0,71R; 24 bóng đèn trên vành ở 0,92R, cách nhau 15°.
 *   - Kim chỉ là một ImageView riêng phía trên (SanChoiActivity), không xoay.
 *
 * KẾT QUẢ DO MÁY CHỦ CHỌN — view này chỉ quay tới ô được giao (quayToi()).
 */
public class VongQuayView extends View {

    /** Gọi mỗi lần một vách vàng quét qua kim (để kim giật và kêu "tích"). */
    public interface NgheQuay {
        void onVachQuaKim(float tienDo);
        void onDungLai(int o);
    }

    private static final int SO_O = 12;
    private static final float GOC_O = 360f / SO_O;

    private final Bitmap banh;
    private final Paint pAnh = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint pChuTo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pChuNho = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pQuang = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTrung = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oVe = new RectF();
    private final Path duongTrung = new Path();
    private final Random rnd = new Random();

    private final List<String[]> nhan = new ArrayList<>();
    private float goc = 0f;              // góc hiện tại của bánh xe (độ, chiều kim đồng hồ)
    private boolean dangQuay = false;
    private int oTrung = -1;
    private long luongTrungBatDau = 0;   // lúc bắt đầu nhấp nháy ô trúng
    private ValueAnimator hoatCanh;
    private NgheQuay nghe;

    public VongQuayView(Context context) { this(context, null); }

    public VongQuayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        banh = BitmapFactory.decodeResource(getResources(), R.drawable.mg_vong_quay);
        pChuTo.setColor(Color.parseColor("#FFE6A3"));
        pChuTo.setTextAlign(Paint.Align.CENTER);
        pChuTo.setTypeface(Typeface.DEFAULT_BOLD);
        pChuNho.setColor(Color.parseColor("#F7ECD6"));
        pChuNho.setTextAlign(Paint.Align.CENTER);
        pChuNho.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        pDen.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        pTrung.setColor(Color.parseColor("#FFF2BF"));
        pTrung.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        // SCREEN cần vẽ phần mềm trên một số máy cũ; lớp phần cứng vẫn đúng từ API 24.
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void setNgheQuay(NgheQuay nghe) { this.nghe = nghe; }

    public void setPhongChu(Typeface tf) {
        pChuTo.setTypeface(tf);
        pChuNho.setTypeface(tf);
        invalidate();
    }

    /** Nhãn 12 ô từ máy chủ: mỗi phần tử {chữ lớn, chữ nhỏ}. */
    public void setNhan(List<String[]> ds) {
        nhan.clear();
        if (ds != null) nhan.addAll(ds);
        invalidate();
    }

    public boolean isDangQuay() { return dangQuay; }

    /**
     * Quay tới ô `o` (máy chủ đã chọn). Lệch ngẫu nhiên ±9° để không dừng
     * đúng giữa ô; 7–8 vòng; giảm tốc kiểu bánh xe thật 1 − (1 − t)^4.
     */
    public void quayToi(int o) {
        if (dangQuay) return;
        dangQuay = true;
        oTrung = -1;
        float lech = (rnd.nextFloat() - .5f) * 18f;
        float dich = (((-(o * GOC_O + GOC_O / 2f) + lech) % 360f) + 360f) % 360f;
        int vong = 7 + rnd.nextInt(2);
        final float tu = goc;
        final float den = goc - (goc % 360f) + vong * 360f + dich;
        long thoiGian = 5200 + rnd.nextInt(600);
        final int[] vachTruoc = {(int) Math.floor(tu / GOC_O)};

        if (hoatCanh != null) hoatCanh.cancel();
        hoatCanh = ValueAnimator.ofFloat(0f, 1f);
        hoatCanh.setDuration(thoiGian);
        hoatCanh.setInterpolator(t -> 1f - (float) Math.pow(1f - t, 4));
        hoatCanh.addUpdateListener(a -> {
            float k = a.getAnimatedFraction();
            goc = tu + (den - tu) * (float) a.getAnimatedValue();
            int vach = (int) Math.floor(goc / GOC_O);
            if (vach != vachTruoc[0]) {
                vachTruoc[0] = vach;
                if (nghe != null) nghe.onVachQuaKim(k);
            }
            invalidate();
        });
        hoatCanh.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                dangQuay = false;
                oTrung = o;
                luongTrungBatDau = SystemClock.uptimeMillis();
                invalidate();
                if (nghe != null) nghe.onDungLai(o);
            }
        });
        hoatCanh.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (hoatCanh != null) hoatCanh.cancel();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(w, w);   // luôn vuông
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float d = Math.min(getWidth(), getHeight());
        if (d <= 0) return;
        float cx = getWidth() / 2f, cy = getHeight() / 2f, R = d / 2f;
        long bayGio = SystemClock.uptimeMillis();

        /* Quầng sáng phía sau, "thở" theo nhịp 3 giây */
        float tho = (float) (0.5 + 0.5 * Math.sin(bayGio / 3000.0 * 2 * Math.PI));
        float rQuang = R * (1.18f - 0.03f * tho);
        pQuang.setShader(new RadialGradient(cx, cy, rQuang,
                new int[]{Color.argb((int) (0x55 * (1 - .45f * tho)), 0xFF, 0xB5, 0x4A),
                          Color.argb((int) (0x22 * (1 - .45f * tho)), 0xFF, 0x8A, 0x2A), Color.TRANSPARENT},
                new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, rQuang, pQuang);

        canvas.save();
        canvas.rotate(goc, cx, cy);

        /* Bánh xe */
        oVe.set(cx - R, cy - R, cx + R, cy + R);
        canvas.drawBitmap(banh, null, oVe, pAnh);

        /* Ô trúng nhấp nháy 4 lần (0 → 0,6 → 0), trong vành 0,22R–0,84R */
        if (oTrung >= 0) {
            float t = (bayGio - luongTrungBatDau) / 500f;
            if (t < 4f) {
                float pha = t % 2f;
                float a = pha < 1f ? pha : 2f - pha;
                pTrung.setAlpha((int) (255 * .6f * a));
                duongTrung.reset();
                float r1 = .22f * R, r2 = .84f * R;
                float bd = oTrung * GOC_O - 90f;
                duongTrung.arcTo(new RectF(cx - r2, cy - r2, cx + r2, cy + r2), bd, GOC_O, true);
                duongTrung.arcTo(new RectF(cx - r1, cy - r1, cx + r1, cy + r1), bd + GOC_O, -GOC_O);
                duongTrung.close();
                canvas.drawPath(duongTrung, pTrung);
            }
        }

        /* Nhãn ô */
        if (nhan.size() == SO_O) {
            float rNhan = .355f * d;
            pChuNho.setTextSize(d * .034f);
            pChuNho.setShadowLayer(d * .012f, 0, d * .004f, Color.argb(220, 0, 0, 0));
            for (int i = 0; i < SO_O; i++) {
                String[] n = nhan.get(i);
                canvas.save();
                canvas.rotate(i * GOC_O + GOC_O / 2f, cx, cy);
                pChuTo.setTextSize(d * (n[0].length() > 2 ? .056f : .068f));
                pChuTo.setShadowLayer(d * .012f, 0, d * .006f, Color.BLACK);
                float yTo = cy - rNhan + pChuTo.getTextSize() * .8f;
                canvas.drawText(n[0], cx, yTo, pChuTo);
                canvas.drawText(n[1], cx, yTo + pChuNho.getTextSize() * 1.15f, pChuNho);
                canvas.restore();
            }
        }

        /* 24 bóng đèn: chẵn/lẻ sáng so le, nhanh hơn khi đang quay */
        float chuKy = dangQuay ? 180f : 1200f;
        boolean nuaDau = ((bayGio / (long) (chuKy / 2)) % 2) == 0;
        float rDen = d * .025f;
        for (int i = 0; i < 24; i++) {
            double g = Math.toRadians(i * 15 - 90);
            float x = cx + (float) Math.cos(g) * .92f * R;
            float y = cy + (float) Math.sin(g) * .92f * R;
            boolean sang = (i % 2 == 0) == nuaDau;
            pDen.setShader(new RadialGradient(x, y, rDen,
                    new int[]{Color.parseColor("#FFFBE8"), Color.parseColor("#FFE08A"), Color.TRANSPARENT},
                    new float[]{0f, .35f, 1f}, Shader.TileMode.CLAMP));
            pDen.setAlpha(sang ? 255 : 38);
            canvas.drawCircle(x, y, rDen, pDen);
        }

        canvas.restore();

        // Đèn và quầng sáng luôn chuyển động: vẽ lại ở khung kế tiếp.
        postInvalidateOnAnimation();
    }
}
