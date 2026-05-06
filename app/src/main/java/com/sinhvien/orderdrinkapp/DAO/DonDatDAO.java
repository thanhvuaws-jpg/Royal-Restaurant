package com.sinhvien.orderdrinkapp.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Pair;

import com.sinhvien.orderdrinkapp.DTO.ChiTietDonDatDTO;
import com.sinhvien.orderdrinkapp.DTO.DonDatDTO;
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.DTO.ThanhToanDTO;
import com.sinhvien.orderdrinkapp.Database.CreateDatabase;

import java.util.ArrayList;
import java.util.List;

public class DonDatDAO extends BaseDAO {

    public DonDatDAO(Context context) {
        super(context);
    }

    public long ThemDonDat(DonDatDTO donDatDTO){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_DONDAT_MABAN,donDatDTO.getMaBan());
        contentValues.put(CreateDatabase.TBL_DONDAT_MANV,donDatDTO.getMaNV());
        contentValues.put(CreateDatabase.TBL_DONDAT_NGAYDAT,donDatDTO.getNgayDat());
        contentValues.put(CreateDatabase.TBL_DONDAT_TINHTRANG,donDatDTO.getTinhTrang());
        contentValues.put(CreateDatabase.TBL_DONDAT_TONGTIEN,donDatDTO.getTongTien());

        long madondat = database.insert(CreateDatabase.TBL_DONDAT,null,contentValues);

        return madondat;
    }

    /** Lấy danh sách đơn đã thanh toán (tinhtrang = 'true') cho màn thống kê */
    public List<DonDatDTO> LayDSDonDatDaThanhToan() {
        List<DonDatDTO> donDatDTOS = new ArrayList<>();
        String query = "SELECT * FROM " + CreateDatabase.TBL_DONDAT
                + " WHERE " + CreateDatabase.TBL_DONDAT_TINHTRANG + " = 'true'"
                + " ORDER BY " + CreateDatabase.TBL_DONDAT_MADONDAT + " DESC";
        Cursor cursor = database.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                DonDatDTO donDatDTO = new DonDatDTO();
                donDatDTO.setMaDonDat(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_MADONDAT)));
                donDatDTO.setMaBan(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_MABAN)));
                donDatDTO.setTongTien(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_TONGTIEN)));
                donDatDTO.setTinhTrang(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_TINHTRANG)));
                donDatDTO.setNgayDat(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_NGAYDAT)));
                donDatDTO.setMaNV(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_MANV)));
                donDatDTOS.add(donDatDTO);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return donDatDTOS;
    }

    public List<DonDatDTO> LayDSDonDat(){
        List<DonDatDTO> donDatDTOS = new ArrayList<>();
        String query = "SELECT * FROM "+CreateDatabase.TBL_DONDAT;
        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                DonDatDTO donDatDTO = new DonDatDTO();
                donDatDTO.setMaDonDat(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_MADONDAT)));
                donDatDTO.setMaBan(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_MABAN)));
                donDatDTO.setTongTien(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_TONGTIEN)));
                donDatDTO.setTinhTrang(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_TINHTRANG)));
                donDatDTO.setNgayDat(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_NGAYDAT)));
                donDatDTO.setMaNV(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_MANV)));
                donDatDTOS.add(donDatDTO);

                cursor.moveToNext();
            }
        }
        cursor.close();
        return donDatDTOS;
    }

    public List<DonDatDTO> LayDSDonDatNgay(String ngaythang){
        List<DonDatDTO> donDatDTOS = new ArrayList<>();
        String query = "SELECT * FROM "+CreateDatabase.TBL_DONDAT+" WHERE "+CreateDatabase.TBL_DONDAT_NGAYDAT+" like '"+ngaythang+"'";
        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                DonDatDTO donDatDTO = new DonDatDTO();
                donDatDTO.setMaDonDat(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_MADONDAT)));
                donDatDTO.setMaBan(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_MABAN)));
                donDatDTO.setTongTien(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_TONGTIEN)));
                donDatDTO.setTinhTrang(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_TINHTRANG)));
                donDatDTO.setNgayDat(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_NGAYDAT)));
                donDatDTO.setMaNV(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_MANV)));
                donDatDTOS.add(donDatDTO);

                cursor.moveToNext();
            }
        }
        cursor.close();
        return donDatDTOS;
    }

    public List<ThanhToanDTO> LayDSMonTheoMaDon(int madondat){
        List<ThanhToanDTO> thanhToanDTOList = new ArrayList<>();
        String query = "SELECT * FROM "+CreateDatabase.TBL_CHITIETDONDAT+" ct,"+CreateDatabase.TBL_MON+" mon WHERE ct."
                +CreateDatabase.TBL_CHITIETDONDAT_MAMON+" = mon."+CreateDatabase.TBL_MON_MAMON+" AND "
                +CreateDatabase.TBL_CHITIETDONDAT_MADONDAT+" = "+madondat;

        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                ThanhToanDTO thanhToanDTO = new ThanhToanDTO();
                thanhToanDTO.setSoLuong(cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_CHITIETDONDAT_SOLUONG)));
                
                // FIX: Lấy giá tiền dạng String, xóa dấu chấm rồi mới parse sang int để tránh bị mất hàng nghìn
                String giaTienStr = cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_MON_GIATIEN));
                try {
                    int giaTien = Integer.parseInt(giaTienStr.replace(".", ""));
                    thanhToanDTO.setGiaTien(giaTien);
                } catch (Exception e) {
                    thanhToanDTO.setGiaTien(0);
                }
                
                thanhToanDTO.setTenMon(cursor.getString(cursor.getColumnIndex(CreateDatabase.TBL_MON_TENMON)));
                thanhToanDTO.setHinhAnh(cursor.getBlob(cursor.getColumnIndex(CreateDatabase.TBL_MON_HINHANH)));
                thanhToanDTOList.add(thanhToanDTO);

                cursor.moveToNext();
            }
        }
        cursor.close();
        return thanhToanDTOList;
    }

    public boolean CapNhatTinhTrangDon(int madondat, String tinhtrang){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_DONDAT_TINHTRANG,tinhtrang);
        long ktra = database.update(CreateDatabase.TBL_DONDAT,contentValues,CreateDatabase.TBL_DONDAT_MADONDAT+
                " = "+madondat,null);
        return ktra != 0;
    }

    public long LayMaDonTheoMaBan(int maban, String tinhtrang){
        String query = "SELECT * FROM " +CreateDatabase.TBL_DONDAT+ " WHERE " +CreateDatabase.TBL_DONDAT_MABAN+ " = '" +maban+ "' AND "
                +CreateDatabase.TBL_DONDAT_TINHTRANG+ " = '" +tinhtrang+ "' ";
        long magoimon = 0;
        Cursor cursor = database.rawQuery(query,null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()){
                magoimon = cursor.getInt(cursor.getColumnIndex(CreateDatabase.TBL_DONDAT_MADONDAT));

                cursor.moveToNext();
            }
        }
        cursor.close();
        return magoimon;
    }

    public boolean UpdateTongTienDonDat(int madondat,String tongtien){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_DONDAT_TONGTIEN,tongtien);
        long ktra  = database.update(CreateDatabase.TBL_DONDAT,contentValues,
                CreateDatabase.TBL_DONDAT_MADONDAT+" = "+madondat,null);
        return ktra != 0;
    }

    public boolean UpdateTThaiDonTheoMaBan(int maban,String tinhtrang){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CreateDatabase.TBL_DONDAT_TINHTRANG,tinhtrang);
        long ktra = database.update(CreateDatabase.TBL_DONDAT,contentValues,CreateDatabase.TBL_DONDAT_MABAN+
                " = '"+maban+"'",null);
        return ktra != 0;
    }

}
