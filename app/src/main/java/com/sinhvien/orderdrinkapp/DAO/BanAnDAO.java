package com.sinhvien.orderdrinkapp.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.sinhvien.orderdrinkapp.DTO.BanAnDTO;
import com.sinhvien.orderdrinkapp.Database.CreateDatabase;

import java.util.ArrayList;
import java.util.List;

public class BanAnDAO extends BaseDAO {

    public BanAnDAO(Context context) {
        super(context);
    }

    //Hàm thêm bàn ăn mới
    public boolean ThemBanAn(String tenban){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_BAN_TENBAN,tenban);
        contentValues.put(CreateDatabase.TBL_BAN_TINHTRANG,"false");

        long ktra = database.insert(CreateDatabase.TBL_BAN,null,contentValues);
        return ktra != 0;
    }

    //Hàm xóa bàn ăn theo mã
    public boolean XoaBanTheoMa(int maban){
        long ktra =database.delete(CreateDatabase.TBL_BAN,CreateDatabase.TBL_BAN_MABAN+" = "+maban,null);
        return ktra != 0;
    }

    //Sửa tên bàn
    public boolean CapNhatTenBan(int maban, String tenban){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_BAN_TENBAN,tenban);

        long ktra = database.update(CreateDatabase.TBL_BAN,contentValues,CreateDatabase.TBL_BAN_MABAN+ " = '"+maban+"' ",null);
        return ktra != 0;
    }

    //Hàm lấy ds các bàn ăn đổ vào gridview
    public List<BanAnDTO> LayTatCaBanAn(){
        List<BanAnDTO> banAnDTOList = new ArrayList<>();
        String query = "SELECT * FROM " +CreateDatabase.TBL_BAN;
        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                BanAnDTO banAnDTO = new BanAnDTO();
                banAnDTO.setMaBan(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_BAN_MABAN)));
                banAnDTO.setTenBan(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_BAN_TENBAN)));

                banAnDTOList.add(banAnDTO);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return banAnDTOList;
    }

    public String LayTinhTrangBanTheoMa(int maban){
        String tinhtrang="";
        String query = "SELECT * FROM "+CreateDatabase.TBL_BAN + " WHERE " +CreateDatabase.TBL_BAN_MABAN+ " = '" +maban+ "' ";
        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                tinhtrang = cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_BAN_TINHTRANG));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return tinhtrang;
    }

    public boolean CapNhatTinhTrangBan(int maban, String tinhtrang){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_BAN_TINHTRANG,tinhtrang);

        long ktra = database.update(CreateDatabase.TBL_BAN,contentValues,CreateDatabase.TBL_BAN_MABAN+ " = '"+maban+"' ",null);
        return ktra != 0;
    }

    public String LayTenBanTheoMa(int maban){
        String tenban="";
        String query = "SELECT * FROM "+CreateDatabase.TBL_BAN + " WHERE " +CreateDatabase.TBL_BAN_MABAN+ " = '" +maban+ "' ";
        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                tenban = cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_BAN_TENBAN));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return tenban;
    }
}
