package com.sinhvien.orderdrinkapp.SanChoi;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * AmThanhGame — âm thanh Sân chơi, TỔNG HỢP bằng mã (không kèm tệp âm thanh),
 * cùng công thức với bản demo web (WebAudio): sóng vuông/tam giác/sin với
 * đường bao tắt dần theo hàm mũ.
 *
 * Lần đầu mở, sinh 5 tệp WAV nhỏ (~10–60 KB) vào bộ nhớ đệm của app rồi nạp
 * vào SoundPool — SoundPool phát chồng nhiều tiếng "tích" liên tiếp được và độ
 * trễ thấp hơn MediaPlayer.
 */
final class AmThanhGame {

    private static final int TAN_SO_MAU = 22050;

    private final SoundPool pool;
    private final int tich, ting, thang, thich, lach;
    private final Random rnd = new Random();
    private boolean bat;

    AmThanhGame(Context ctx, boolean bat) {
        this.bat = bat;
        pool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .build();
        File thuMuc = new File(ctx.getCacheDir(), "am_san_choi");
        //noinspection ResultOfMethodCallIgnored
        thuMuc.mkdirs();
        tich  = nap(thuMuc, "tich",  new Not[]{new Not(1650, 0, .045, Song.VUONG, .05)});
        ting  = nap(thuMuc, "ting",  new Not[]{new Not(2093, 0, .25, Song.SIN, .08), new Not(3136, .03, .2, Song.SIN, .04)});
        thang = nap(thuMuc, "thang", new Not[]{new Not(523, 0, .5, Song.TAM_GIAC, .12), new Not(659, .09, .5, Song.TAM_GIAC, .12),
                new Not(784, .18, .5, Song.TAM_GIAC, .12), new Not(1047, .27, .5, Song.TAM_GIAC, .12),
                new Not(1319, .36, .5, Song.TAM_GIAC, .12)});
        thich = nap(thuMuc, "thich", new Not[]{new Not(90, 0, .35, Song.SIN, .35), new Not(180, 0, .2, Song.TAM_GIAC, .1)});
        lach  = nap(thuMuc, "lach",  new Not[]{new Not(280, 0, .06, Song.VUONG, .04)});
    }

    void setBat(boolean bat) { this.bat = bat; }
    boolean isBat() { return bat; }

    /** Vách vàng quét qua kim. Đổi cao độ nhẹ cho đỡ đều. */
    void tich()  { phat(tich, .9f + rnd.nextFloat() * .2f, .8f); }
    void ting()  { phat(ting, 1f, .9f); }
    void thang() { phat(thang, 1f, 1f); }
    void thich() { phat(thich, 1f, 1f); }
    void lach()  { phat(lach, .85f + rnd.nextFloat() * .3f, .7f); }

    private void phat(int id, float caoDo, float to) {
        if (bat && id != 0) pool.play(id, to, to, 1, 0, caoDo);
    }

    void giaiPhong() { pool.release(); }

    /* ═══════════════════════════ Tổng hợp ═══════════════════════════ */

    private enum Song { SIN, VUONG, TAM_GIAC }

    private static final class Not {
        final double tan, luc, dai, to; final Song song;
        Not(double tan, double luc, double dai, Song song, double to) {
            this.tan = tan; this.luc = luc; this.dai = dai; this.song = song; this.to = to;
        }
    }

    private int nap(File thuMuc, String ten, Not[] not) {
        File tep = new File(thuMuc, ten + ".wav");
        try {
            if (!tep.exists() || tep.length() < 64) ghiWav(tep, tongHop(not));
            return pool.load(tep.getPath(), 1);
        } catch (IOException e) {
            return 0;   // không có âm thanh thì trò chơi vẫn chạy
        }
    }

    private static short[] tongHop(Not[] not) {
        double tong = 0;
        for (Not n : not) tong = Math.max(tong, n.luc + n.dai + .02);
        int so = (int) (tong * TAN_SO_MAU);
        double[] mau = new double[so];
        for (Not n : not) {
            int dau = (int) (n.luc * TAN_SO_MAU), dai = (int) (n.dai * TAN_SO_MAU);
            for (int i = 0; i < dai && dau + i < so; i++) {
                double t = i / (double) TAN_SO_MAU;
                // Đường bao: lên trong 5 ms, tắt dần theo mũ tới 0,0001 ở cuối nốt
                double bao = t < .005 ? t / .005 : Math.pow(.0001 / 1, (t - .005) / Math.max(.001, n.dai - .005));
                double pha = (n.tan * t) % 1.0;
                double s;
                switch (n.song) {
                    case VUONG:    s = pha < .5 ? 1 : -1; break;
                    case TAM_GIAC: s = 4 * Math.abs(pha - .5) - 1; break;
                    default:       s = Math.sin(2 * Math.PI * pha);
                }
                mau[dau + i] += s * bao * n.to * 4;   // ×4: bù độ nhỏ của gain WebAudio
            }
        }
        short[] ra = new short[so];
        for (int i = 0; i < so; i++) {
            ra[i] = (short) Math.max(-32767, Math.min(32767, mau[i] * 32767));
        }
        return ra;
    }

    private static void ghiWav(File tep, short[] mau) throws IOException {
        int dl = mau.length * 2;
        ByteBuffer b = ByteBuffer.allocate(44 + dl).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()).putInt(36 + dl).put("WAVE".getBytes());
        b.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1)
         .putInt(TAN_SO_MAU).putInt(TAN_SO_MAU * 2).putShort((short) 2).putShort((short) 16);
        b.put("data".getBytes()).putInt(dl);
        for (short s : mau) b.putShort(s);
        try (FileOutputStream o = new FileOutputStream(tep)) {
            o.write(b.array());
        }
    }
}
