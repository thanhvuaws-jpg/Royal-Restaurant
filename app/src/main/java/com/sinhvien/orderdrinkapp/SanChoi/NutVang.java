package com.sinhvien.orderdrinkapp.SanChoi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * NutVang — nút "QUAY" / "MỞ RƯƠNG" dát vàng nổi khối như bản demo:
 * bóng nâu đậm phía dưới (độ dày 6dp), mặt vàng chuyển màu, viền sáng trên,
 * mép tối dưới, và một vệt sáng lướt qua mỗi 2,6 giây.
 *
 * Bấm thì mặt nút lún xuống 4dp (chữ lún theo). Tắt thì xám đi và ngừng vệt sáng.
 */
public class NutVang extends FrameLayout {

    private final float dp;
    private final float bo, day, lun;
    private final Paint pBong = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pMat = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pMep = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pVienSang = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSang = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path mat = new Path();
    private ValueAnimator vetSang;
    private float viTriSang = -1f;

    public NutVang(Context context) { this(context, null); }

    public NutVang(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        bo = 18 * dp; day = 6 * dp; lun = 4 * dp;
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        pBong.setColor(Color.parseColor("#5C3409"));
        pMep.setColor(Color.parseColor("#8F5A14"));
        pVienSang.setColor(Color.parseColor("#88FFFFFF"));
        pVienSang.setStyle(Paint.Style.STROKE);
        pVienSang.setStrokeWidth(2 * dp);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        pMat.setShader(new LinearGradient(0, 0, 0, h - day,
                new int[]{Color.parseColor("#FFF0BF"), Color.parseColor("#F2C45C"), Color.parseColor("#C98A26")},
                new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ColorMatrixColorFilter loc = null;
        if (!enabled) {
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(.4f);
            ColorMatrix toi = new ColorMatrix();
            toi.setScale(.7f, .7f, .7f, 1f);
            cm.postConcat(toi);
            loc = new ColorMatrixColorFilter(cm);
        }
        pBong.setColorFilter(loc); pMat.setColorFilter(loc); pMep.setColorFilter(loc);
        setAlpha(enabled ? 1f : .85f);
        invalidate();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        vetSang = ValueAnimator.ofFloat(0f, 1f);
        vetSang.setDuration(2600);
        vetSang.setRepeatCount(ValueAnimator.INFINITE);
        vetSang.setInterpolator(new LinearInterpolator());
        vetSang.addUpdateListener(a -> {
            // 60% đầu chu kỳ vệt sáng chạy từ trái sang phải, 40% sau nghỉ.
            float t = (float) a.getAnimatedValue();
            viTriSang = t < .6f ? t / .6f : -1f;
            invalidate();
        });
        vetSang.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (vetSang != null) vetSang.cancel();
        super.onDetachedFromWindow();
    }

    private float doLun() { return isPressed() && isEnabled() ? lun : 0f; }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float l = doLun();
        // Bóng dưới (khối nút)
        r.set(0, day, w, h);
        canvas.drawRoundRect(r, bo, bo, pBong);
        // Mặt nút
        r.set(0, l, w, h - day + l);
        mat.reset();
        mat.addRoundRect(r, bo, bo, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(mat);
        canvas.drawRect(r, pMat);
        canvas.drawRect(0, r.bottom - 4 * dp, w, r.bottom, pMep);          // mép tối dưới
        canvas.drawLine(bo / 2, r.top + dp, w - bo / 2, r.top + dp, pVienSang); // viền sáng trên
        if (isEnabled() && viTriSang >= 0f) {
            float x = -w * .35f + viTriSang * w * 1.7f;
            pSang.setShader(new LinearGradient(x - w * .15f, 0, x + w * .15f, h * .4f,
                    new int[]{0x00FFFFFF, 0x70FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(r, pSang);
        }
        canvas.restore();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(0, doLun());
        super.dispatchDraw(canvas);
        canvas.restore();
    }
}
