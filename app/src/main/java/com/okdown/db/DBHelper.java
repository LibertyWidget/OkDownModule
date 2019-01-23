package com.okdown.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.okdown.OkGo;
import com.okdown.request.model.Progress;
import com.okdown.utils.OkLog;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_CACHE_NAME = "okgo.db";
    private static final int DB_CACHE_VERSION = 1;
    static final String TABLE_DOWNLOAD = "download";

    static final Lock lock = new ReentrantLock();

    private TableEntity downloadTableEntity = new TableEntity(TABLE_DOWNLOAD);

    public DBHelper() {
        this(OkGo.getInstance().getContext());
    }

    DBHelper(Context context) {
        super(context, DB_CACHE_NAME, null, DB_CACHE_VERSION);

        downloadTableEntity.addColumn(new ColumnEntity(Progress.URL, "VARCHAR", true, true))//
                .addColumn(new ColumnEntity(Progress.TYPE, "VARCHAR"))//
                .addColumn(new ColumnEntity(Progress.M3U8_URL, "VARCHAR"))//
                .addColumn(new ColumnEntity(Progress.M3U8_URL_LIST, "VARCHAR"))//
                .addColumn(new ColumnEntity(Progress.NAME, "VARCHAR"))//
                .addColumn(new ColumnEntity(Progress.FOLDER, "VARCHAR"))//
                .addColumn(new ColumnEntity(Progress.FILE_PATH, "VARCHAR"))//
                .addColumn(new ColumnEntity(Progress.FILE_NAME, "VARCHAR"))//
                .addColumn(new ColumnEntity(Progress.FRACTION, "VARCHAR"))//
                .addColumn(new ColumnEntity(Progress.TOTAL_SIZE, "INTEGER"))//
                .addColumn(new ColumnEntity(Progress.CURRENT_SIZE, "INTEGER"))//
                .addColumn(new ColumnEntity(Progress.STATUS, "INTEGER"))//
                .addColumn(new ColumnEntity(Progress.PRIORITY, "INTEGER"))//
                .addColumn(new ColumnEntity(Progress.DATE, "INTEGER"))//
                .addColumn(new ColumnEntity(Progress.REQUEST, "BLOB"))//
                .addColumn(new ColumnEntity(Progress.EXTRA1, "BLOB"));//
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(downloadTableEntity.buildTableString());
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (isNeedUpgradeTable(db, downloadTableEntity))
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOAD);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public static boolean isNeedUpgradeTable(SQLiteDatabase db, TableEntity table) {
        if (!isTableExists(db, table.tableName)) return true;

        Cursor cursor = db.rawQuery("select * from " + table.tableName, null);
        if (cursor == null) return false;
        try {
            int columnCount = table.getColumnCount();
            if (columnCount == cursor.getColumnCount()) {
                for (int i = 0; i < columnCount; i++) {
                    if (table.getColumnIndex(cursor.getColumnName(i)) == -1) {
                        return true;
                    }
                }
            } else {
                return true;
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    public static boolean isTableExists(SQLiteDatabase db, String tableName) {
        if (tableName == null || db == null || !db.isOpen()) return false;

        Cursor cursor = null;
        int count = 0;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name = ?", new String[]{"table", tableName});
            if (!cursor.moveToFirst()) {
                return false;
            }
            count = cursor.getInt(0);
        } catch (Exception e) {
            OkLog.printStackTrace(e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return count > 0;
    }
}
