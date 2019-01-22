package com.okdown.task.down;

import android.content.ContentValues;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.okdown.OkDownload;
import com.okdown.OkGo;
import com.okdown.db.DownloadManager;
import com.okdown.request.Request;
import com.okdown.request.m3u8.M3U8;
import com.okdown.request.m3u8.M3U8Ts;
import com.okdown.request.m3u8.MUtils;
import com.okdown.request.model.DownMediaType;
import com.okdown.request.model.FileStatus;
import com.okdown.request.model.HttpHeaders;
import com.okdown.request.model.HttpUtils;
import com.okdown.request.model.Progress;
import com.okdown.task.PriorityRunnable;
import com.okdown.utils.IOUtils;
import com.okdown.utils.OkLog;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadTask implements Runnable {

    private static final int BUFFER_SIZE = 1024 * 8;

    public Progress progress;
    public Map<Object, DownloadListener> listeners;
    private ThreadPoolExecutor executor;
    private PriorityRunnable priorityRunnable;

    public DownloadTask(String name, Request<File, ? extends Request> request) {
        progress = new Progress();
        progress.name = name;
        progress.folder = OkDownload.$().getFolder();
        progress.url = request.getBaseUrl();
        progress.status = FileStatus.NONE.ordinal();
        progress.totalSize = -1;
        progress.request = request;
        executor = OkDownload.$().getThreadPool().getExecutor();
        listeners = new HashMap<>();
    }

    public DownloadTask(Progress progress) {
        HttpUtils.checkNotNull(progress, "progress == null");
        this.progress = progress;
        executor = OkDownload.$().getThreadPool().getExecutor();
        listeners = new HashMap<>();
    }

    public DownloadTask folder(String folder) {
        if (folder != null && !TextUtils.isEmpty(folder.trim())) {
            progress.folder = folder;
        } else {
            OkLog.e("folder is null, ignored!");
        }
        return this;
    }

    public DownloadTask fileName(String fileName) {
        if (fileName != null && !TextUtils.isEmpty(fileName.trim())) {
            progress.fileName = fileName;
        } else {
            OkLog.e("fileName is null, ignored!");
        }
        return this;
    }

    public DownloadTask priority(int priority) {
        progress.priority = priority;
        return this;
    }

    public DownloadTask extra1(Serializable extra1) {
        progress.extra1 = extra1;
        return this;
    }

    public DownloadTask extra2(Serializable extra2) {
        progress.extra2 = extra2;
        return this;
    }

    public DownloadTask extra3(Serializable extra3) {
        progress.extra3 = extra3;
        return this;
    }

    public DownloadTask save() {
        if (!TextUtils.isEmpty(progress.folder) && !TextUtils.isEmpty(progress.fileName)) {
            progress.filePath = new File(progress.folder, progress.fileName).getAbsolutePath();
        }
        DownloadManager.$().replace(progress);
        return this;
    }

    public DownloadTask register(DownloadListener listener) {
        if (listener != null) {
            listeners.put(listener.tag, listener);
        }
        return this;
    }

    public void unRegister(DownloadListener listener) {
        HttpUtils.checkNotNull(listener, "listener == null");
        listeners.remove(listener.tag);
    }

    public void unRegister(String tag) {
        HttpUtils.checkNotNull(tag, "tag == null");
        listeners.remove(tag);
    }

    public void start() {
        if (OkDownload.$().getTask(progress.url) == null || DownloadManager.$().get(progress.url) == null) {
            throw new IllegalStateException("you must call DownloadTask#save() before DownloadTask#start()！");
        }
        if (progress.status == FileStatus.NONE.ordinal() || progress.status == FileStatus.PAUSE.ordinal() || progress.status == FileStatus.ERROR.ordinal()) {
            postOnStart(progress);
            postWaiting(progress);
            priorityRunnable = new PriorityRunnable(progress.priority, this);
            executor.execute(priorityRunnable);
        } else if (progress.status == FileStatus.FINISH.ordinal()) {
            if (progress.filePath == null) {
                postOnError(progress, new OkLog("the file of the task with tag:" + progress.url + " may be invalid or damaged, please call the method restart() to downloadMp4 again！"));
            } else {
                File file = new File(progress.filePath);
                if (file.exists() && file.length() == progress.totalSize) {
                    postOnFinish(progress);
                } else {
                    postOnError(progress, new OkLog("the file " + progress.filePath + " may be invalid or damaged, please call the method restart() to downloadMp4 again！"));
                }
            }
        } else {
            OkLog.e("the task with tag " + progress.url + " is already in the downloadMp4 queue, current task status is " + progress.status);
        }
    }

    public void restart() {
        pause();
        IOUtils.delFileOrFolder(new File(progress.filePath));
        progress.status = FileStatus.NONE.ordinal();
        progress.currentSize = 0;
        progress.fraction = 0;
        progress.speed = 0;
        DownloadManager.$().replace(progress);
        start();
    }

    public void pause() {
        executor.remove(priorityRunnable);
        if (progress.status == FileStatus.WAITING.ordinal()) {
            postPause(progress);
        } else if (progress.status == FileStatus.LOADING.ordinal()) {
            progress.speed = 0;
            progress.status = FileStatus.PAUSE.ordinal();
        } else {
            OkLog.e("only the task with status WAITING(1) or LOADING(2) can pause, current status is " + progress.status);
        }
    }

    public void remove() {
        remove(false);
    }

    public DownloadTask remove(boolean isDeleteFile) {
        pause();
        if (isDeleteFile) IOUtils.delFileOrFolder(new File(progress.filePath));
        DownloadManager.$().delete(progress.url);
        DownloadTask task = OkDownload.$().removeTask(progress.url);
        postOnRemove(progress);
        return task;
    }

    @Override
    public void run() {

        //从数据库恢复的情况
        if (null != progress.type)
            if (TextUtils.equals(DownMediaType.M3U8.name(), progress.type)) {
                if (!TextUtils.isEmpty(progress.m3u8UrlList)) {
                    if (0 != progress.currentSize) {//原来都没有下载成功过
                        File file = new File(progress.folder);
                        File[] files = file.listFiles();
                        if (files.length < progress.currentSize) {
                            IOUtils.delFileOrFolder(file);
                            progress.currentSize = 0;
                        }
                    }
                    M3U8 m3U8 = MUtils.toJson(progress.m3u8UrlList);
                    m3U8.setBasePath(progress.m3u8Url);
                    try {
                        downloadM3u8(m3U8, progress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                } else if (!TextUtils.isEmpty(progress.m3u8Original)) {
                    //集合里没有但是原始数据中有
                    M3U8 parse = MUtils.parse(progress.m3u8Original);
                    progress.totalSize = 0;
                    IOUtils.delFileOrFolder(new File(progress.filePath));
                    Log.e("tag", "开始现在恢复的数据");
                    return;
                }

            }

        //check breakpoint
        long startPosition = progress.currentSize;
        if (startPosition < 0) {
            postOnError(progress, OkLog.BREAKPOINT_EXPIRED());
            return;
        }
        if (startPosition > 0) {
            if (!TextUtils.isEmpty(progress.filePath)) {
                File file = new File(progress.filePath);
                if (!file.exists()) {
                    postOnError(progress, OkLog.BREAKPOINT_NOT_EXIST());
                    return;
                }
            }
        }

        //request network from startPosition
        Response response;
        try {
            Request<?, ? extends Request> request = progress.request;
            request.headers(HttpHeaders.HEAD_KEY_RANGE, "bytes=" + startPosition + "-");
            response = request.execute();
        } catch (IOException e) {
            postOnError(progress, e);
            return;
        }
        //check network data
        int code = response.code();
        if (code == 404 || code >= 500) {
            postOnError(progress, OkLog.NET_ERROR());
            return;
        }
        ResponseBody body = response.body();
        if (body == null) {
            postOnError(progress, new OkLog("response body is null"));
            return;
        }
        if (progress.totalSize == -1) {
            progress.totalSize = body.contentLength();
        }
        //file type
        MediaType mediaType = body.contentType();
        if (null != mediaType) {
            String subtype = mediaType.subtype();
            if (!TextUtils.isEmpty(subtype)) {
                subtype = subtype.toLowerCase();
                /*
                    http://1251883823.vod2.myqcloud.com/6b94ca32vodsh1251883823/a0fd47ff5285890784524177969/2gyLYJt7ZQ8A.mp4
                 */
                if (subtype.contains("video/mp4")) {
                    progress.type = DownMediaType.MP4.name();
                } else
                    //https://v-6-cn.com/20190101/8414_be31c04d/index.m3u8?sign=a59c25d7579e1f7e96edca7af6165cbd
                    //https://135zyv6.xw0371.com/2018/11/15/tqsrSI3Bbm0abCmU/playlist.m3u8
                    //http://h1.aaccy.com/ckplayer/youku/lsit/XNDAxODM2MjkzMg==.m3u8?ts=1548126978&key=30ede8a4ba665a2c9d080a1775d094f5
                    if (subtype.contains("vnd.apple.mpegurl") || subtype.contains("x-mpegurl")) {
                        progress.type = DownMediaType.M3U8.name();
                    }
            }
        }

        //create filename
        String fileName = progress.fileName;
        if (TextUtils.isEmpty(fileName)) {
            fileName = HttpUtils.getNetFileName(response, progress.url);
            progress.fileName = fileName;
        }
        if (!IOUtils.createFolder(new File(progress.folder))) {
            postOnError(progress, OkLog.NOT_AVAILABLE());
            return;
        }

        //create and check file
        File file;
        if (TextUtils.isEmpty(progress.filePath)) {
            file = new File(String.format("%s%s/%s", progress.folder, progress.name, fileName));
            progress.filePath = file.getAbsolutePath();
        } else {
            file = new File(progress.filePath, progress.name);
        }

        if (startPosition > 0 && !file.exists()) {
            postOnError(progress, OkLog.BREAKPOINT_EXPIRED());
            return;
        }
        if (startPosition > progress.totalSize) {
            postOnError(progress, OkLog.BREAKPOINT_EXPIRED());
            return;
        }
        if (startPosition == 0 && file.exists()) {
            IOUtils.delFileOrFolder(file);
        }
        if (startPosition == progress.totalSize && startPosition > 0) {
            if (file.exists() && startPosition == file.length()) {
                postOnFinish(progress);
                return;
            } else {
                postOnError(progress, OkLog.BREAKPOINT_EXPIRED());
                return;
            }
        }

        //start downloading
        RandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.seek(startPosition);
            progress.currentSize = startPosition;
        } catch (Exception e) {
            postOnError(progress, e);
            return;
        }

        if (TextUtils.equals(DownMediaType.MP4.name(), progress.type)) {
            try {
                DownloadManager.$().replace(progress);
                downloadMp4(body.byteStream(), randomAccessFile, progress);
            } catch (IOException e) {
                postOnError(progress, e);
                return;
            }
        } else if (TextUtils.equals(DownMediaType.M3U8.name(), progress.type)) {
            try {
                M3U8 m3U8 = MUtils.parseIndex(progress.url);
                if (null != m3U8) {
                    progress.m3u8Url = m3U8.getBasePath();
                    progress.m3u8UrlList = MUtils.toList(progress.m3u8Url, m3U8.getTsList());
                    try {
                        DownloadManager.$().replace(progress);
                        downloadM3u8(m3U8, progress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //check finish status
        if (progress.status == FileStatus.PAUSE.ordinal()) {
            postPause(progress);
        } else if (progress.status == FileStatus.LOADING.ordinal()) {
            if (file.length() == progress.totalSize) {
                postOnFinish(progress);
            } else {
                postOnError(progress, OkLog.BREAKPOINT_EXPIRED());
            }
        } else {
            postOnError(progress, OkLog.UNKNOWN());
        }
    }

    private synchronized void downloadM3u8(M3U8 m3U8, Progress progress) throws IOException {
        this.progress.status = FileStatus.LOADING.ordinal();
        byte[] buffer = new byte[BUFFER_SIZE];
        List<M3U8Ts> tsList = m3U8.getTsList();
        if (progress.currentSize <= 0) {
            progress.currentSize = 0;
        }
        for (; progress.currentSize < tsList.size(); progress.currentSize++) {
            M3U8Ts ts = tsList.get((int) progress.currentSize);

            URL url = new URL(ts.getFile());
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            BufferedInputStream in = new BufferedInputStream(urlConn.getInputStream(), BUFFER_SIZE);
            String fileName = OkDownload.$().getFolder() + progress.name + File.separator + "list" + File.separator + ts.getFileName();
            File file1 = new File(fileName);
            if (!file1.exists()) {
                int len;
                OutputStream os = new FileOutputStream(fileName);
                while ((len = in.read(buffer, 0, BUFFER_SIZE)) != -1 && progress.status == FileStatus.LOADING.ordinal()) {
                    os.write(buffer, 0, len);
                }
                os.close();
                in.close();
                Progress.changeProgress(this.progress, len, this.progress.totalSize, new Progress.Action() {
                    @Override
                    public void call(Progress progress) {
                        postLoading(progress);
                    }
                });
            }
        }
    }

    private void downloadMp4(InputStream input, RandomAccessFile out, Progress progress) throws
            IOException {
        if (input == null || out == null) return;

        progress.status = FileStatus.LOADING.ordinal();
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
        int len;
        try {
            while ((len = in.read(buffer, 0, BUFFER_SIZE)) != -1 && progress.status == FileStatus.LOADING.ordinal()) {
                out.write(buffer, 0, len);

                Progress.changeProgress(progress, len, progress.totalSize, new Progress.Action() {
                    @Override
                    public void call(Progress progress) {
                        postLoading(progress);
                    }
                });
            }
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(input);
        }
    }


    private void postOnStart(final Progress progress) {
        progress.speed = 0;
        progress.status = FileStatus.NONE.ordinal();
        updateDatabase(progress);
        HttpUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : listeners.values()) {
                    listener.onStart(progress);
                }
            }
        });
    }

    private void postWaiting(final Progress progress) {
        progress.speed = 0;
        progress.status = FileStatus.WAITING.ordinal();
        updateDatabase(progress);
        HttpUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : listeners.values()) {
                    listener.onProgress(progress);
                }
            }
        });
    }

    private void postPause(final Progress progress) {
        progress.speed = 0;
        progress.status = FileStatus.PAUSE.ordinal();
        updateDatabase(progress);
        HttpUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : listeners.values()) {
                    listener.onProgress(progress);
                }
            }
        });
    }

    private void postLoading(final Progress progress) {
        updateDatabase(progress);
        HttpUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : listeners.values()) {
                    listener.onProgress(progress);
                }
            }
        });
    }

    private void postOnError(final Progress progress, final Throwable throwable) {
        progress.speed = 0;
        progress.status = FileStatus.ERROR.ordinal();
        progress.exception = throwable;
        updateDatabase(progress);
        HttpUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : listeners.values()) {
                    listener.onProgress(progress);
                    listener.onError(progress);
                }
            }
        });
    }

    private void postOnFinish(final Progress progress) {
        progress.speed = 0;
        progress.fraction = 1.0f;
        progress.status = FileStatus.FINISH.ordinal();
        updateDatabase(progress);
        HttpUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : listeners.values()) {
                    listener.onProgress(progress);
                    listener.onFinish(progress);
                }
            }
        });
    }

    private void postOnRemove(final Progress progress) {
        updateDatabase(progress);
        HttpUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : listeners.values()) {
                    listener.onRemove(progress);
                }
                listeners.clear();
            }
        });
    }

    private void updateDatabase(Progress progress) {
        ContentValues contentValues = Progress.buildUpdateContentValues(progress);
        DownloadManager.$().update(contentValues, progress.url);
    }
}
