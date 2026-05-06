package com.sinhvien.orderdrinkapp.DAO;

import android.content.Context;
import android.database.Cursor;
import com.sinhvien.orderdrinkapp.DTO.ThanhToanDTO;
import com.sinhvien.orderdrinkapp.Database.CreateDatabase;
import java.util.ArrayList;
import java.util.List;

public class ThanhToanDAO extends BaseDAO {

    public ThanhToanDAO(Context context) {
        super(context);
    }

    public List<ThanhToanDTO> LayDSMonTheoMaDon(int madondat){
        List<ThanhToanDTO> thanhToanDTOS = new ArrayList<>();
        String query = "SELECT * FROM " + CreateDatabase.TBL_CHITIETDONDAT + " ctdd, " + 
                CreateDatabase.TBL_MON + " mon WHERE " +
                "ctdd." + CreateDatabase.TBL_CHITIETDONDAT_MAMON + " = mon." + CreateDatabase.TBL_MON_MAMON + 
                " AND " + CreateDatabase.TBL_CHITIETDONDAT_MADONDAT + " = " + madondat;

        Cursor cursor = database.rawQuery(query, null);
        if (cursor != null) {
            while (cursor.moveToNext()){
                ThanhToanDTO thanhToanDTO = new ThanhToanDTO();
                
                int indexSoLuong = cursor.getColumnIndex(CreateDatabase.TBL_CHITIETDONDAT_SOLUONG);
                int indexGiaTien = cursor.getColumnIndex(CreateDatabase.TBL_MON_GIATIEN);
                int indexTenMon = cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_MON_TENMON)) != null ? 
                        cursor.getColumnIndex(CreateDatabase.TBL_MON_TENMON) : -1;
                
                // Sử dụng getColumnIndex an toàn hơn
                if (indexSoLuong != -1) thanhToanDTO.setSoLuong(cursor.getInt(indexSoLuong));
                if (indexGiaTien != -1) thanhToanDTO.setGiaTien(cursor.getInt(indexGiaTien));
                
                int nameIndex = cursor.getColumnIndex(CreateDatabase.TBL_MON_TENMON);
                if (nameIndex != -1) thanhToanDTO.setTenMon(cursor.getString(nameIndex));
                
                int imgIndex = cursor.getColumnIndex(CreateDatabase.TBL_MON_HINHANH);
                if (imgIndex != -1) thanhToanDTO.setHinhAnh(cursor.getBlob(imgIndex));
                
                thanhToanDTOS.add(thanhToanDTO);
            }
            cursor.close();
        }
        return thanhToanDTOS;
    }
}
