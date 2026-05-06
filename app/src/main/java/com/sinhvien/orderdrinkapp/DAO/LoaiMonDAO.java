package com.sinhvien.orderdrinkapp.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.Database.CreateDatabase;
import java.util.ArrayList;
import java.util.List;

public class LoaiMonDAO extends BaseDAO {

    public LoaiMonDAO(Context context) {
        super(context);
    }

    public boolean ThemLoaiMon(LoaiMonDTO loaiMonDTO){
        checkDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_LOAIMON_TENLOAI, loaiMonDTO.getTenLoai());
        contentValues.put(CreateDatabase.TBL_LOAIMON_HINHANH, loaiMonDTO.getHinhAnh());
        long ktra = database.insert(CreateDatabase.TBL_LOAIMON, null, contentValues);
        return ktra != 0;
    }

    public boolean XoaLoaiMon(int maloai){
        checkDatabase();
        long ktra = database.delete(CreateDatabase.TBL_LOAIMON, 
                CreateDatabase.TBL_LOAIMON_MALOAI + " = " + maloai, null);
        return ktra != 0;
    }

    public boolean SuaLoaiMon(LoaiMonDTO loaiMonDTO, int maloai){
        checkDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_LOAIMON_TENLOAI, loaiMonDTO.getTenLoai());
        contentValues.put(CreateDatabase.TBL_LOAIMON_HINHANH, loaiMonDTO.getHinhAnh());
        long ktra = database.update(CreateDatabase.TBL_LOAIMON, contentValues,
                CreateDatabase.TBL_LOAIMON_MALOAI + " = " + maloai, null);
        return ktra != 0;
    }

    public List<LoaiMonDTO> LayDSLoaiMon(){
        checkDatabase();
        List<LoaiMonDTO> loaiMonDTOList = new ArrayList<>();
        String query = "SELECT * FROM " + CreateDatabase.TBL_LOAIMON;
        Cursor cursor = database.rawQuery(query, null);
        if (cursor != null) {
            while (cursor.moveToNext()){
                LoaiMonDTO loaiMonDTO = new LoaiMonDTO();
                int indexMaLoai = cursor.getColumnIndex(CreateDatabase.TBL_LOAIMON_MALOAI);
                int indexTenLoai = cursor.getColumnIndex(CreateDatabase.TBL_LOAIMON_TENLOAI);
                int indexHinhAnh = cursor.getColumnIndex(CreateDatabase.TBL_LOAIMON_HINHANH);

                if (indexMaLoai != -1) loaiMonDTO.setMaLoai(cursor.getInt(indexMaLoai));
                if (indexTenLoai != -1) loaiMonDTO.setTenLoai(cursor.getString(indexTenLoai));
                if (indexHinhAnh != -1) loaiMonDTO.setHinhAnh(cursor.getBlob(indexHinhAnh));
                
                loaiMonDTOList.add(loaiMonDTO);
            }
            cursor.close();
        }
        return loaiMonDTOList;
    }

    public LoaiMonDTO LayLoaiMonTheoMa(int maloai){
        checkDatabase();
        LoaiMonDTO loaiMonDTO = new LoaiMonDTO();
        String query = "SELECT * FROM " + CreateDatabase.TBL_LOAIMON + 
                " WHERE " + CreateDatabase.TBL_LOAIMON_MALOAI + " = " + maloai;
        Cursor cursor = database.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.moveToFirst()){
                int indexTenLoai = cursor.getColumnIndex(CreateDatabase.TBL_LOAIMON_TENLOAI);
                int indexHinhAnh = cursor.getColumnIndex(CreateDatabase.TBL_LOAIMON_HINHANH);
                
                if (indexTenLoai != -1) loaiMonDTO.setTenLoai(cursor.getString(indexTenLoai));
                if (indexHinhAnh != -1) loaiMonDTO.setHinhAnh(cursor.getBlob(indexHinhAnh));
            }
            cursor.close();
        }
        return loaiMonDTO;
    }
}
