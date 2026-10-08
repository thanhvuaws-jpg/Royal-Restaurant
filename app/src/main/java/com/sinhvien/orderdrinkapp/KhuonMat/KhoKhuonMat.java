package com.sinhvien.orderdrinkapp.KhuonMat;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * KhoKhuonMat — nơi cất dữ liệu đăng nhập khuôn mặt TRÊN MÁY (QĐ-103).
 *
 * Mỗi tài khoản đã bật gồm: mẫu khuôn mặt (vector 512 chiều trung bình của các
 * khung lúc đăng ký) và khóa thiết bị máy chủ cấp. Toàn bộ được mã hóa AES-GCM
 * bằng một khóa nằm trong Android Keystore — khóa đó không bao giờ rời phần cứng
 * bảo mật của máy, nên chép tệp dữ liệu sang máy khác cũng không đọc được.
 *
 * Không lưu ảnh khuôn mặt. Không gửi mẫu khuôn mặt đi đâu.
 */
public final class KhoKhuonMat {

    private static final String KHO = "AndroidKeyStore";
    private static final String BI_DANH = "royal_khuon_mat_v1";
    private static final String TEP = "khuon_mat";
    private static final String KHOA_DU_LIEU = "du_lieu";

    /** Một tài khoản đã bật đăng nhập bằng khuôn mặt trên máy này. */
    public static final class TaiKhoan {
        public int maNV;
        public String tenDN;
        public String hoTen;
        public int maQuyen;
        public int maKhoa;
        public String khoa;
        public float[] mau;
        public long ngayDangKy;
    }

    private KhoKhuonMat() { }

    public static synchronized List<TaiKhoan> docTatCa(Context ctx) {
        List<TaiKhoan> ds = new ArrayList<>();
        String maHoa = ctx.getSharedPreferences(TEP, Context.MODE_PRIVATE).getString(KHOA_DU_LIEU, null);
        if (maHoa == null) return ds;
        try {
            JSONArray a = new JSONArray(giaiMa(maHoa));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                TaiKhoan t = new TaiKhoan();
                t.maNV = o.getInt("manv");
                t.tenDN = o.optString("tendn");
                t.hoTen = o.optString("hoten");
                t.maQuyen = o.optInt("maquyen");
                t.maKhoa = o.getInt("makhoa");
                t.khoa = o.getString("khoa");
                JSONArray m = o.getJSONArray("mau");
                t.mau = new float[m.length()];
                for (int j = 0; j < m.length(); j++) t.mau[j] = (float) m.getDouble(j);
                t.ngayDangKy = o.optLong("ngay");
                ds.add(t);
            }
        } catch (Exception e) {
            // Khóa Keystore bị mất (xóa khóa màn hình, khôi phục máy...) thì dữ liệu
            // cũ không giải mã được nữa: bỏ đi, người dùng đăng nhập mật khẩu rồi bật lại.
            xoaHet(ctx);
            ds.clear();
        }
        return ds;
    }

    public static boolean coTaiKhoan(Context ctx) {
        return !docTatCa(ctx).isEmpty();
    }

    public static TaiKhoan tim(Context ctx, int maNV) {
        for (TaiKhoan t : docTatCa(ctx)) if (t.maNV == maNV) return t;
        return null;
    }

    /** Thêm hoặc thay (theo mã tài khoản). */
    public static synchronized void luu(Context ctx, TaiKhoan moi) throws Exception {
        List<TaiKhoan> ds = docTatCa(ctx);
        List<TaiKhoan> giu = new ArrayList<>();
        for (TaiKhoan t : ds) if (t.maNV != moi.maNV) giu.add(t);
        giu.add(moi);
        ghi(ctx, giu);
    }

    public static synchronized void xoa(Context ctx, int maNV) {
        List<TaiKhoan> giu = new ArrayList<>();
        for (TaiKhoan t : docTatCa(ctx)) if (t.maNV != maNV) giu.add(t);
        try {
            if (giu.isEmpty()) xoaHet(ctx); else ghi(ctx, giu);
        } catch (Exception e) {
            xoaHet(ctx);
        }
    }

    public static synchronized void xoaHet(Context ctx) {
        ctx.getSharedPreferences(TEP, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static void ghi(Context ctx, List<TaiKhoan> ds) throws Exception {
        JSONArray a = new JSONArray();
        for (TaiKhoan t : ds) {
            JSONObject o = new JSONObject();
            o.put("manv", t.maNV);
            o.put("tendn", t.tenDN);
            o.put("hoten", t.hoTen);
            o.put("maquyen", t.maQuyen);
            o.put("makhoa", t.maKhoa);
            o.put("khoa", t.khoa);
            JSONArray m = new JSONArray();
            for (float f : t.mau) m.put((double) f);
            o.put("mau", m);
            o.put("ngay", t.ngayDangKy);
            a.put(o);
        }
        ctx.getSharedPreferences(TEP, Context.MODE_PRIVATE).edit()
                .putString(KHOA_DU_LIEU, maHoa(a.toString())).apply();
    }

    /* ─────────────────────────── AES-GCM qua Android Keystore ─────────────────────────── */

    private static SecretKey khoaBiMat() throws Exception {
        KeyStore ks = KeyStore.getInstance(KHO);
        ks.load(null);
        if (ks.containsAlias(BI_DANH)) {
            return ((KeyStore.SecretKeyEntry) ks.getEntry(BI_DANH, null)).getSecretKey();
        }
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KHO);
        kg.init(new KeyGenParameterSpec.Builder(BI_DANH,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return kg.generateKey();
    }

    private static String maHoa(String ro) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, khoaBiMat());
        byte[] iv = c.getIV();
        byte[] ma = c.doFinal(ro.getBytes(StandardCharsets.UTF_8));
        byte[] goi = new byte[1 + iv.length + ma.length];
        goi[0] = (byte) iv.length;
        System.arraycopy(iv, 0, goi, 1, iv.length);
        System.arraycopy(ma, 0, goi, 1 + iv.length, ma.length);
        return Base64.encodeToString(goi, Base64.NO_WRAP);
    }

    private static String giaiMa(String s) throws Exception {
        byte[] goi = Base64.decode(s, Base64.NO_WRAP);
        int n = goi[0];
        byte[] iv = new byte[n];
        System.arraycopy(goi, 1, iv, 0, n);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, khoaBiMat(), new GCMParameterSpec(128, iv));
        byte[] ro = c.doFinal(goi, 1 + n, goi.length - 1 - n);
        return new String(ro, StandardCharsets.UTF_8);
    }
}
