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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LocalDatabaseHelper - Lớp quản lý cơ sở dữ liệu SQLite cục bộ của ứng dụng.
 * Kế thừa SQLiteOpenHelper để thực hiện tạo bảng, nâng cấp phiên bản dữ liệu và đồng bộ hóa ngoại tuyến.
 * Sử dụng ExecutorService để thực hiện các thao tác ghi dữ liệu bất đồng bộ dưới nền, tránh chặn luồng chính (Main Thread).
 */
public class LocalDatabaseHelper extends SQLiteOpenHelper {

    // Tên tệp cơ sở dữ liệu lưu dưới dạng SQLite cục bộ
    private static final String DATABASE_NAME = "ql_nhahang_local.db";
    // Phiên bản cơ sở dữ liệu (tăng dần khi thay đổi cấu trúc bảng/schema)
    private static final int DATABASE_VERSION = 1;

    // Singleton instance duy nhất trong vòng đời ứng dụng để tránh rò rỉ bộ nhớ
    private static LocalDatabaseHelper instance;
    // Executor luồng đơn (Single Thread Executor) dùng chuyên biệt cho việc chạy ngầm các tác vụ SQLite
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    /**
     * Lấy Executor Service chạy ngầm phục vụ ghi/đọc SQLite.
     */
    public static ExecutorService getExecutor() {
        return dbExecutor;
    }

    /**
     * Lấy instance duy nhất (Singleton Pattern) của lớp LocalDatabaseHelper.
     * Sử dụng từ khóa synchronized để đảm bảo an toàn đa luồng (Thread-safe).
     */
    public static synchronized LocalDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    // Constructor ở chế độ private để chặn khởi tạo trực tiếp từ ngoài lớp
    private LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng BAN (Quản lý danh sách bàn ăn)
        db.execSQL("CREATE TABLE BAN (" +
                "MABAN INTEGER PRIMARY KEY, " +
                "TENBAN TEXT NOT NULL, " +
                "TINHTRANG TEXT DEFAULT 'false'" +
                ");");

        // Tạo bảng LOAIMON (Quản lý các danh mục món ăn/thức uống)
        db.execSQL("CREATE TABLE LOAIMON (" +
                "MALOAI INTEGER PRIMARY KEY, " +
                "TENLOAI TEXT NOT NULL, " +
                "HINHANH TEXT" +
                ");");

        // Tạo bảng MON (Quản lý thông tin chi tiết từng món ăn)
        db.execSQL("CREATE TABLE MON (" +
                "MAMON INTEGER PRIMARY KEY, " +
                "TENMON TEXT NOT NULL, " +
                "GIATIEN TEXT NOT NULL, " +
                "TINHTRANG TEXT DEFAULT 'true', " +
                "MALOAI INTEGER, " +
                "HINHANH TEXT" +
                ");");

        // Tạo bảng NHANVIEN (Lưu thông tin nhân viên hoặc khách hàng phục vụ đăng nhập)
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
        // Áp dụng cơ chế nâng cấp schema an toàn theo Chương 4.2.J (SQLite Migration)
        // Thay vì DROP các bảng gây mất dữ liệu cục bộ, ta dùng ALTER TABLE để nâng cấp dần qua từng phiên bản
        if (oldVersion < 2) {
            try {
                // Ví dụ nâng cấp lên v2: Thêm cột mô tả món ăn (MOTA) vào bảng MON mà không làm mất dữ liệu cũ
                db.execSQL("ALTER TABLE MON ADD COLUMN MOTA TEXT");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (oldVersion < 3) {
            // Ví dụ nâng cấp lên v3: Thêm bảng mới nếu có yêu cầu lưu trữ mới
            // db.execSQL("CREATE TABLE IF NOT EXISTS ...");
        }
    }

    //region BAN Sync & Query
    /**
     * Đồng bộ hóa dữ liệu danh sách Bàn ăn từ Cloud Server xuống SQLite cục bộ.
     * Sử dụng SQLite Transaction để tối ưu tốc độ ghi và đảm bảo tính toàn vẹn dữ liệu.
     *
     * @param tables Danh sách bàn ăn nhận từ API Response.
     */
    public void syncTables(List<com.sinhvien.orderdrinkapp.Api.TableResponse> tables) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (com.sinhvien.orderdrinkapp.Api.TableResponse table : tables) {
                ContentValues cv = new ContentValues();
                cv.put("MABAN", table.getMaBan());
                cv.put("TENBAN", table.getTenBan());
                cv.put("TINHTRANG", table.getTinhTrang());
                // Chèn mới hoặc ghi đè nếu trùng MABAN (CONFLICT_REPLACE)
                db.insertWithOnConflict("BAN", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful(); // Đánh dấu giao dịch thành công để lưu thay đổi
        } finally {
            db.endTransaction(); // Hoàn tất giao dịch
        }
    }

    /**
     * Lấy toàn bộ danh sách bàn ăn được lưu trữ ở SQLite cục bộ.
     *
     * @return Danh sách đối tượng BanAnDTO.
     */
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
    /**
     * Đồng bộ hóa danh mục loại món ăn từ Cloud Server xuống SQLite cục bộ.
     * Xóa sạch danh mục cũ trước khi đồng bộ dữ liệu mới.
     *
     * @param categories Danh sách loại món nhận từ API Response.
     */
    public void syncCategories(List<com.sinhvien.orderdrinkapp.Api.LoaiMonResponse> categories) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("LOAIMON", null, null); // Xóa dữ liệu cũ để tránh dư thừa danh mục đã xóa trên Cloud
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

    /**
     * Lấy toàn bộ danh sách loại món ăn lưu trong SQLite cục bộ.
     *
     * @return Danh sách đối tượng LoaiMonDTO.
     */
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
    /**
     * Đồng bộ danh sách món ăn cụ thể thuộc một danh mục hoặc toàn bộ.
     *
     * @param maLoai Mã loại món cần đồng bộ.
     * @param dishes Danh sách món ăn nhận từ API Response.
     * @param clearAllForCategory Có xóa sạch món ăn cũ thuộc loại món này trước khi đồng bộ hay không.
     */
    public void syncDishes(int maLoai, List<com.sinhvien.orderdrinkapp.Api.MonResponse> dishes, boolean clearAllForCategory) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            if (clearAllForCategory) {
                // Xóa các món thuộc danh mục này để đồng bộ lại từ đầu
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

    /**
     * Lấy danh sách món ăn thuộc một danh mục cụ thể từ SQLite cục bộ, có hỗ trợ tìm kiếm theo tên món.
     *
     * @param maLoai Mã loại món cần tìm.
     * @param searchQuery Từ khóa tìm kiếm tên món ăn (nếu có).
     * @return Danh sách đối tượng MonDTO.
     */
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

    /**
     * Lấy toàn bộ món ăn lưu trong SQLite cục bộ, xếp thứ tự theo bảng chữ cái ABC.
     *
     * @return Danh sách tất cả đối tượng MonDTO.
     */
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
    /**
     * Đồng bộ danh sách tài khoản Nhân viên từ Cloud Server xuống SQLite cục bộ.
     *
     * @param staffList Danh sách nhân viên lấy từ API Response.
     */
    public void syncStaff(List<com.sinhvien.orderdrinkapp.Api.StaffResponse> staffList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (com.sinhvien.orderdrinkapp.Api.StaffResponse staff : staffList) {
                ContentValues cv = new ContentValues();
                cv.put("MANV", staff.getMaNV());
                cv.put("HOTENNV", staff.getHoTenNV());
                cv.put("TENDN", staff.getTenDN());
                cv.put("MATKHAU", "");
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

    /**
     * Lấy toàn bộ danh sách nhân viên từ SQLite cục bộ.
     *
     * @return Danh sách đối tượng NhanVienDTO.
     */
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
