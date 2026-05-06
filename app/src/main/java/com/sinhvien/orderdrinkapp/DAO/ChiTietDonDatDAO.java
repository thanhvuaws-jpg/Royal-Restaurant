package com.sinhvien.orderdrinkapp.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.sinhvien.orderdrinkapp.DTO.ChiTietDonDatDTO;
import com.sinhvien.orderdrinkapp.Database.CreateDatabase;

public class ChiTietDonDatDAO extends BaseDAO {

    public ChiTietDonDatDAO(Context context) {
        super(context);
    }

    public boolean KiemTraMonTonTai(int madondat, int mamon){
        String query = "SELECT * FROM " +CreateDatabase.TBL_CHITIETDONDAT+ " WHERE " +CreateDatabase.TBL_CHITIETDONDAT_MAMON+
                " = " +mamon+ " AND " +CreateDatabase.TBL_CHITIETDONDAT_MADONDAT+ " = "+madondat;
        Cursor cursor = database.rawQuery(query,null);
        boolean tonTai = cursor.getCount() != 0;
        cursor.close();
        return tonTai;
    }

    public int LaySLMonTheoMaDon(int madondat, int mamon){
        int soluong = 0;
        String query = "SELECT * FROM " +CreateDatabase.TBL_CHITIETDONDAT+ " WHERE " +CreateDatabase.TBL_CHITIETDONDAT_MAMON+
                " = " +mamon+ " AND " +CreateDatabase.TBL_CHITIETDONDAT_MADONDAT+ " = "+madondat;
        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                soluong = cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_CHITIETDONDAT_SOLUONG));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return soluong;
    }

    public boolean CapNhatSL(ChiTietDonDatDTO chiTietDonDatDTO){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_CHITIETDONDAT_SOLUONG, chiTietDonDatDTO.getSoLuong());
        if (chiTietDonDatDTO.getGhiChu() != null) {
            contentValues.put(CreateDatabase.TBL_CHITIETDONDAT_GHICHU, chiTietDonDatDTO.getGhiChu());
        }
        long ktra = database.update(CreateDatabase.TBL_CHITIETDONDAT, contentValues,
                CreateDatabase.TBL_CHITIETDONDAT_MADONDAT + " = "
                + chiTietDonDatDTO.getMaDonDat() + " AND " + CreateDatabase.TBL_CHITIETDONDAT_MAMON + " = "
                + chiTietDonDatDTO.getMaMon(), null);
        return ktra != 0;
    }

    public String LayGhiChuMonTheoMaDon(int madondat, int mamon) {
        String ghichu = "";
        String query = "SELECT " + CreateDatabase.TBL_CHITIETDONDAT_GHICHU
                + " FROM " + CreateDatabase.TBL_CHITIETDONDAT
                + " WHERE " + CreateDatabase.TBL_CHITIETDONDAT_MAMON + " = " + mamon
                + " AND " + CreateDatabase.TBL_CHITIETDONDAT_MADONDAT + " = " + madondat;
        Cursor cursor = database.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            ghichu = cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_CHITIETDONDAT_GHICHU));
            if (ghichu == null) ghichu = "";
        }
        cursor.close();
        return ghichu;
    }

    public boolean ThemChiTietDonDat(ChiTietDonDatDTO chiTietDonDatDTO){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_CHITIETDONDAT_SOLUONG, chiTietDonDatDTO.getSoLuong());
        contentValues.put(CreateDatabase.TBL_CHITIETDONDAT_MADONDAT, chiTietDonDatDTO.getMaDonDat());
        contentValues.put(CreateDatabase.TBL_CHITIETDONDAT_MAMON, chiTietDonDatDTO.getMaMon());
        contentValues.put(CreateDatabase.TBL_CHITIETDONDAT_GHICHU,
                chiTietDonDatDTO.getGhiChu() != null ? chiTietDonDatDTO.getGhiChu() : "");
        long ktra = database.insert(CreateDatabase.TBL_CHITIETDONDAT, null, contentValues);
        return ktra != 0;
    }

}
