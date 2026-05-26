package com.sinhvien.orderdrinkapp.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sinhvien.orderdrinkapp.DTO.BanAnDTO;
import com.sinhvien.orderdrinkapp.DTO.LoaiMonDTO;
import com.sinhvien.orderdrinkapp.DTO.MonDTO;
import com.sinhvien.orderdrinkapp.DTO.NhanVienDTO;

import java.util.ArrayList;
import java.util.List;

public class LocalDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ql_nhahang_local.db";
    private static final int DATABASE_VERSION = 1;

    private static LocalDatabaseHelper instance;

    public static synchronized LocalDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table BAN
        db.execSQL("CREATE TABLE BAN (" +
                "MABAN INTEGER PRIMARY KEY, " +
                "TENBAN TEXT NOT NULL, " +
                "TINHTRANG TEXT DEFAULT 'false'" +
                ");");

        // Table LOAIMON
        db.execSQL("CREATE TABLE LOAIMON (" +
                "MALOAI INTEGER PRIMARY KEY, " +
                "TENLOAI TEXT NOT NULL, " +
                "HINHANH TEXT" +
                ");");

        // Table MON
        db.execSQL("CREATE TABLE MON (" +
                "MAMON INTEGER PRIMARY KEY, " +
                "TENMON TEXT NOT NULL, " +
                "GIATIEN TEXT NOT NULL, " +
                "TINHTRANG TEXT DEFAULT 'true', " +
                "MALOAI INTEGER, " +
                "HINHANH TEXT" +
                ");");

        // Table NHANVIEN
        db.execSQL("CREATE TABLE NHANVIEN (" +
                "MANV INTEGER PRIMARY KEY, " +
                "HOTENNV TEXT NOT NULL, " +
                "TENDN TEXT UNIQUE NOT NULL, " +
                "MATKHAU TEXT NOT NULL, " +
                "EMAIL TEXT, " +
                "SDT TEXT, " +
                "GIOITINH TEXT, " +
                "NGAYSINH TEXT, " +
                "MAQUYEN INTEGER" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS BAN");
        db.execSQL("DROP TABLE IF EXISTS LOAIMON");
        db.execSQL("DROP TABLE IF EXISTS MON");
        db.execSQL("DROP TABLE IF EXISTS NHANVIEN");
        onCreate(db);
    }

    //region BAN Sync & Query
    public void syncTables(List<com.sinhvien.orderdrinkapp.Api.TableResponse> tables) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (com.sinhvien.orderdrinkapp.Api.TableResponse table : tables) {
                ContentValues cv = new ContentValues();
                cv.put("MABAN", table.getMaBan());
                cv.put("TENBAN", table.getTenBan());
                cv.put("TINHTRANG", table.getTinhTrang());
                db.insertWithOnConflict("BAN", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<BanAnDTO> getTables() {
        List<BanAnDTO> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM BAN ORDER BY MABAN ASC", null);
        if (cursor.moveToFirst()) {
            do {
                BanAnDTO dto = new BanAnDTO();
                dto.setMaBan(cursor.getInt(cursor.getColumnIndexOrThrow("MABAN")));
                dto.setTenBan(cursor.getString(cursor.getColumnIndexOrThrow("TENBAN")));
                dto.setTinhTrang(cursor.getString(cursor.getColumnIndexOrThrow("TINHTRANG")));
                list.add(dto);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
    //endregion

    //region LOAIMON Sync & Query
    public void syncCategories(List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> categories) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (com.sinhvien.orderdrinkapp.Api.LoaiMonResponse cat : categories) {
                ContentValues cv = new ContentValues();
                cv.put("MALOAI", cat.getMaLoai());
                cv.put("TENLOAI", cat.getTenLoai());
                cv.put("HINHANH", cat.getHinhAnh());
                db.insertWithOnConflict("LOAIMON", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<LoaiMonDTO> getCategories() {
        List<LoaiMonDTO> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM LOAIMON ORDER BY MALOAI ASC", null);
        if (cursor.moveToFirst()) {
            do {
                LoaiMonDTO dto = new LoaiMonDTO();
                dto.setMaLoai(cursor.getInt(cursor.getColumnIndexOrThrow("MALOAI")));
                dto.setTenLoai(cursor.getString(cursor.getColumnIndexOrThrow("TENLOAI")));
                dto.setHinhAnhPath(cursor.getString(cursor.getColumnIndexOrThrow("HINHANH")));
                list.add(dto);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
    //endregion

    //region MON Sync & Query
    public void syncDishes(int maLoai, List<com.sinhvien.orderdrinkapp.Api.MonResponse> dishes, boolean clearAllForCategory) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            if (clearAllForCategory) {
                db.delete("MON", "MALOAI = ?", new String[]{String.valueOf(maLoai)});
            }
            for (com.sinhvien.orderdrinkapp.Api.MonResponse dish : dishes) {
                ContentValues cv = new ContentValues();
                cv.put("MAMON", dish.getMaMon());
                cv.put("TENMON", dish.getTenMon());
                cv.put("GIATIEN", dish.getGiaTien());
                cv.put("TINHTRANG", dish.getTinhTrang());
                cv.put("MALOAI", dish.getMaLoai());
                cv.put("HINHANH", dish.getHinhAnh());
                db.insertWithOnConflict("MON", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<MonDTO> getDishes(int maLoai, String searchQuery) {
        List<MonDTO> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            cursor = db.rawQuery("SELECT * FROM MON WHERE MALOAI = ? AND TENMON LIKE ? ORDER BY MAMON ASC",
                    new String[]{String.valueOf(maLoai), "%" + searchQuery + "%"});
        } else {
            cursor = db.rawQuery("SELECT * FROM MON WHERE MALOAI = ? ORDER BY MAMON ASC",
                    new String[]{String.valueOf(maLoai)});
        }

        if (cursor.moveToFirst()) {
            do {
                MonDTO dto = new MonDTO();
                dto.setMaMon(cursor.getInt(cursor.getColumnIndexOrThrow("MAMON")));
                dto.setTenMon(cursor.getString(cursor.getColumnIndexOrThrow("TENMON")));
                dto.setGiaTien(cursor.getString(cursor.getColumnIndexOrThrow("GIATIEN")));
                dto.setTinhTrang(cursor.getString(cursor.getColumnIndexOrThrow("TINHTRANG")));
                dto.setMaLoai(cursor.getInt(cursor.getColumnIndexOrThrow("MALOAI")));
                dto.setHinhAnhUrl(cursor.getString(cursor.getColumnIndexOrThrow("HINHANH")));
                list.add(dto);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
    public List<MonDTO> getAllDishes() {
        List<MonDTO> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM MON ORDER BY TENMON ASC", null);
        if (cursor.moveToFirst()) {
            do {
                MonDTO dto = new MonDTO();
                dto.setMaMon(cursor.getInt(cursor.getColumnIndexOrThrow("MAMON")));
                dto.setTenMon(cursor.getString(cursor.getColumnIndexOrThrow("TENMON")));
                dto.setGiaTien(cursor.getString(cursor.getColumnIndexOrThrow("GIATIEN")));
                dto.setTinhTrang(cursor.getString(cursor.getColumnIndexOrThrow("TINHTRANG")));
                dto.setMaLoai(cursor.getInt(cursor.getColumnIndexOrThrow("MALOAI")));
                dto.setHinhAnhUrl(cursor.getString(cursor.getColumnIndexOrThrow("HINHANH")));
                list.add(dto);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
    //endregion

    //region NHANVIEN Sync & Query
    public void syncStaff(List<com.sinhvien.orderdrinkapp.Api.StaffResponse> staffList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (com.sinhvien.orderdrinkapp.Api.StaffResponse staff : staffList) {
                ContentValues cv = new ContentValues();
                cv.put("MANV", staff.getMaNV());
                cv.put("HOTENNV", staff.getHoTenNV());
                cv.put("TENDN", staff.getTenDN());
                cv.put("MATKHAU", staff.getMatKhau());
                cv.put("EMAIL", staff.getEmail());
                cv.put("SDT", staff.getSdt());
                cv.put("GIOITINH", staff.getGioiTinh());
                cv.put("NGAYSINH", staff.getNgaySinh());
                cv.put("MAQUYEN", staff.getMaQuyen());
                db.insertWithOnConflict("NHANVIEN", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<NhanVienDTO> getStaff() {
        List<NhanVienDTO> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM NHANVIEN ORDER BY MANV ASC", null);
        if (cursor.moveToFirst()) {
            do {
                NhanVienDTO dto = new NhanVienDTO();
                dto.setMANV(cursor.getInt(cursor.getColumnIndexOrThrow("MANV")));
                dto.setHOTENNV(cursor.getString(cursor.getColumnIndexOrThrow("HOTENNV")));
                dto.setTENDN(cursor.getString(cursor.getColumnIndexOrThrow("TENDN")));
                dto.setMATKHAU(cursor.getString(cursor.getColumnIndexOrThrow("MATKHAU")));
                dto.setEMAIL(cursor.getString(cursor.getColumnIndexOrThrow("EMAIL")));
                dto.setSDT(cursor.getString(cursor.getColumnIndexOrThrow("SDT")));
                dto.setGIOITINH(cursor.getString(cursor.getColumnIndexOrThrow("GIOITINH")));
                dto.setNGAYSINH(cursor.getString(cursor.getColumnIndexOrThrow("NGAYSINH")));
                dto.setMAQUYEN(cursor.getInt(cursor.getColumnIndexOrThrow("MAQUYEN")));
                list.add(dto);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
    //endregion
}
