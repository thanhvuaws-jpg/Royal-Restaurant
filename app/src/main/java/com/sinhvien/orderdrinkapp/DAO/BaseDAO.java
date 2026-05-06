package com.sinhvien.orderdrinkapp.DAO;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.sinhvien.orderdrinkapp.Database.CreateDatabase;

public abstract class BaseDAO {
    protected SQLiteDatabase database;
    protected CreateDatabase dbHelper;

    public BaseDAO(Context context) {
        dbHelper = new CreateDatabase(context);
        database = dbHelper.open();
    }

    // Đảm bảo database luôn mở khi sử dụng
    protected void checkDatabase() {
        if (database == null || !database.isOpen()) {
            database = dbHelper.open();
        }
    }
}
