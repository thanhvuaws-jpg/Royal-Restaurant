package com.sinhvien.orderdrinkapp.SanChoi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * 16 tia sáng vàng tỏa từ tâm, mờ dần ra ngoài (12% → 66% bán kính) — vẽ sau
 * rương khi rương bật nắp. Xoay chậm bằng setRotation() từ Activity.
 */
public class TiaSangView extends View {

    private static final int SO_TIA = 16;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tia = new Path();
    private final RectF o = new RectF();

    public TiaSangView(Context context) { this(context, null); }

    public TiaSangView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        float cx = w / 2f, cy = h / 2f, R = Math.min(w, h) / 2f;
        p.setShader(new RadialGradient(cx, cy, R,
                new int[]{0x55FFD77A, 0x55FFD77A, 0x00FFD77A}, new float[]{0f, .12f, .66f},
                Shader.TileMode.CLAMP));
        tia.reset();
        o.set(cx - R, cy - R, cx + R, cy + R);
        float buoc = 360f / SO_TIA;
        for (int i = 0; i < SO_TIA; i++) {
            tia.moveTo(cx, cy);
            tia.arcTo(o, i * buoc + 7f, 6f);
            tia.close();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(tia, p);
    }
}
