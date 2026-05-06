package com.sinhvien.orderdrinkapp.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.sinhvien.orderdrinkapp.DTO.QuyenDTO;
import com.sinhvien.orderdrinkapp.Database.CreateDatabase;

import java.util.ArrayList;
import java.util.List;

public class QuyenDAO extends BaseDAO {

    public QuyenDAO(Context context) {
        super(context);
    }

    public void ThemQuyen(String tenquyen){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_QUYEN_TENQUYEN,tenquyen);
        database.insert(CreateDatabase.TBL_QUYEN,null,contentValues);
    }
    

    public String LayTenQuyenTheoMa(int maquyen){
        String tenquyen ="";
        String query = "SELECT * FROM "+CreateDatabase.TBL_QUYEN+" WHERE "+CreateDatabase.TBL_QUYEN_MAQUYEN+" = "+maquyen;
        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                tenquyen = cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_QUYEN_TENQUYEN));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return tenquyen;
    }
}
