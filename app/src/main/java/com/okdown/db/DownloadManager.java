package com.okdown.db;

import android.content.ContentValues;
import android.database.Cursor;


import com.okdown.request.model.FileStatus;
import com.okdown.request.model.Progress;

import java.util.List;

public class DownloadManager extends BaseDao<Progress> {

    private DownloadManager() {
        super(new DBHelper());
    }

    public static DownloadManager $() {
        return DownloadManagerHolder.instance;
    }

    private static class DownloadManagerHolder {
        private static final DownloadManager instance = new DownloadManager();
    }

    @Override
    public Progress parseCursorToBean(Cursor cursor) {
        return Progress.parseCursorToBean(cursor);
    }

    @Override
    public ContentValues getContentValues(Progress progress) {
        return Progress.buildContentValues(progress);
    }

    @Override
    public String getTableName() {
        return DBHelper.TABLE_DOWNLOAD;
    }

    @Override
    public void unInit() {
    }

    public Progress get(String tag) {
        return queryOne(Progress.URL + "=?", new String[]{tag});
    }

    public void delete(String taskKey) {
        delete(Progress.URL + "=?", new String[]{taskKey});
    }

    public boolean update(Progress progress) {
        return update(progress, Progress.URL + "=?", new String[]{progress.url});
    }

    public boolean update(ContentValues contentValues, String tag) {
        return update(contentValues, Progress.URL + "=?", new String[]{tag});
    }

    public List<Progress> getAll() {
        return query(null, null, null, null, null, Progress.DATE + " ASC", null);
    }

    public List<Progress> getFinished() {
        return query(null, "status=?", new String[]{FileStatus.FINISH.name()}, null, null, Progress.DATE + " ASC", null);
    }

    public List<Progress> getDownloading() {
        return query(null, "status not in(?)", new String[]{FileStatus.FINISH.name()}, null, null, Progress.DATE + " ASC", null);
    }

    public boolean clear() {
        return deleteAll();
    }
}
