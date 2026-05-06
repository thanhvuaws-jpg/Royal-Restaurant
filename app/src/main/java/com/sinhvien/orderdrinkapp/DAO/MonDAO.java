package com.sinhvien.orderdrinkapp.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.sinhvien.orderdrinkapp.DTO.MonDTO;
import com.sinhvien.orderdrinkapp.Database.CreateDatabase;

import java.util.ArrayList;
import java.util.List;

public class MonDAO extends BaseDAO {

    public MonDAO(Context context) {
        super(context);
    }

    public boolean ThemMon(MonDTO monDTO){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_MON_MALOAI,monDTO.getMaLoai());
        contentValues.put(CreateDatabase.TBL_MON_TENMON,monDTO.getTenMon());
        contentValues.put(CreateDatabase.TBL_MON_GIATIEN,monDTO.getGiaTien());
        contentValues.put(CreateDatabase.TBL_MON_HINHANH,monDTO.getHinhAnh());
        contentValues.put(CreateDatabase.TBL_MON_TINHTRANG,"true");

        long ktra = database.insert(CreateDatabase.TBL_MON,null,contentValues);

        return ktra != 0;
    }

    public boolean XoaMon(int mamon){
        long ktra = database.delete(CreateDatabase.TBL_MON,CreateDatabase.TBL_MON_MAMON+ " = " +mamon
                ,null);
        return ktra != 0;
    }

    public boolean SuaMon(MonDTO monDTO,int mamon){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_MON_MALOAI,monDTO.getMaLoai());
        contentValues.put(CreateDatabase.TBL_MON_TENMON,monDTO.getTenMon());
        contentValues.put(CreateDatabase.TBL_MON_GIATIEN,monDTO.getGiaTien());
        contentValues.put(CreateDatabase.TBL_MON_HINHANH,monDTO.getHinhAnh());
        contentValues.put(CreateDatabase.TBL_MON_TINHTRANG,monDTO.getTinhTrang());

        long ktra = database.update(CreateDatabase.TBL_MON,contentValues,
                CreateDatabase.TBL_MON_MAMON+" = "+mamon,null);
        return ktra != 0;
    }

    public List<MonDTO> LayDSMonTheoLoai(int maloai){
        List<MonDTO> monDTOList = new ArrayList<>();
        String query = "SELECT * FROM " +CreateDatabase.TBL_MON+ " WHERE " +CreateDatabase.TBL_MON_MALOAI+ " = '" +maloai+ "' ";
        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                MonDTO monDTO = new MonDTO();
                monDTO.setHinhAnh(cursor.getBlob(cursor.getColumnIndex(CreateDatabase.TBL_MON_HINHANH)));
                monDTO.setTenMon(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_MON_TENMON)));
                monDTO.setMaLoai(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_MON_MALOAI)));
                monDTO.setMaMon(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_MON_MAMON)));
                monDTO.setGiaTien(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_MON_GIATIEN)));
                monDTO.setTinhTrang(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_MON_TINHTRANG)));
                monDTOList.add(monDTO);

                cursor.moveToNext();
            }
        }
        cursor.close();
        return monDTOList;
    }

    public MonDTO LayMonTheoMa(int mamon){
        MonDTO monDTO = new MonDTO();
        String query = "SELECT * FROM "+CreateDatabase.TBL_MON+" WHERE "+CreateDatabase.TBL_MON_MAMON+" = "+mamon;
        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                monDTO.setHinhAnh(cursor.getBlob(cursor.getColumnIndex(CreateDatabase.TBL_MON_HINHANH)));
                monDTO.setTenMon(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_MON_TENMON)));
                monDTO.setMaLoai(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_MON_MALOAI)));
                monDTO.setMaMon(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_MON_MAMON)));
                monDTO.setGiaTien(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_MON_GIATIEN)));
                monDTO.setTinhTrang(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_MON_TINHTRANG)));

                cursor.moveToNext();
            }
        }
        cursor.close();
        return monDTO;
    }

    public boolean CapNhatTinhTrang(int mamon, String tinhtrang) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_MON_TINHTRANG, tinhtrang);
        long ktra = database.update(CreateDatabase.TBL_MON, contentValues,
                CreateDatabase.TBL_MON_MAMON + " = " + mamon, null);
        return ktra != 0;
    }

}
