
package com.okdown;

import android.os.Environment;

import com.okdown.db.DownloadManager;
import com.okdown.request.Request;
import com.okdown.request.model.FileStatus;
import com.okdown.request.model.Progress;
import com.okdown.task.XExecutor;
import com.okdown.task.down.DownloadTask;
import com.okdown.task.down.DownloadThreadPool;
import com.okdown.utils.IOUtils;
import com.okdown.utils.OkLog;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OkDownload {

    private String folder;                                      //下载的默认文件夹
    private final DownloadThreadPool threadPool = new DownloadThreadPool();//下载的线程池
    private final ConcurrentHashMap<String, DownloadTask> taskMap = new ConcurrentHashMap<>();//所有任务

    public static OkDownload $() {
        return OkDownloadHolder.instance;
    }


    private static class OkDownloadHolder {
        private static final OkDownload instance = new OkDownload();
    }

    public void init() {
        folder = Environment.getExternalStorageDirectory() + "/aa" + File.separator + "download" + File.separator;
        IOUtils.createFolder(new File(folder));
        List<Progress> taskList = DownloadManager.$().getDownloading();
        for (Progress info : taskList) {
            if (info.status == FileStatus.WAITING.ordinal() || info.status == FileStatus.LOADING.ordinal() || info.status == FileStatus.PAUSE.ordinal()) {
                info.status = FileStatus.NONE.ordinal();
            }
            DownloadTask task = new DownloadTask(info);
            taskMap.put(info.url, task);
        }
        DownloadManager.$().replace(taskList);
    }

    public DownloadTask request(String name, String url) {
        IOUtils.createFolder(new File(OkDownload.$().getFolder() + name));

        Request<File, ? extends Request> request = OkGo.get(url);
        Map<String, DownloadTask> taskMap = OkDownload.$().getTaskMap();
        DownloadTask task = taskMap.get(url);
        if (task == null) {
            task = new DownloadTask(name, request);
            taskMap.put(url, task);
        }
        return task;
    }

    public void start(String url) {
        for (Map.Entry<String, DownloadTask> entry : taskMap.entrySet()) {
            if (entry.getKey().equals(url)) {
                DownloadTask task = entry.getValue();
                if (task == null) {
                    OkLog.e("can't find task with tag = " + entry.getKey());
                    continue;
                }
                task.start();
                break;
            }
        }
    }


    public void startAll() {
        for (Map.Entry<String, DownloadTask> entry : taskMap.entrySet()) {
            DownloadTask task = entry.getValue();
            if (task == null) {
                OkLog.e("can't find task with tag = " + entry.getKey());
                continue;
            }
            task.start();
        }
    }

    public void pause(String url) {
        for (Map.Entry<String, DownloadTask> entry : taskMap.entrySet()) {
            DownloadTask task = entry.getValue();
            if (task == null) {
                OkLog.e("can't find task with tag = " + entry.getKey());
                continue;
            }
            if (entry.getKey().equals(url)) {
                if (task.progress.status != FileStatus.LOADING.ordinal()) {
                    task.pause();
                }
                break;
            }
        }
        for (Map.Entry<String, DownloadTask> entry : taskMap.entrySet()) {
            DownloadTask task = entry.getValue();
            if (task == null) {
                OkLog.e("can't find task with tag = " + entry.getKey());
                continue;
            }
            if (entry.getKey().equals(url)) {
                if (task.progress.status == FileStatus.LOADING.ordinal()) {
                    task.pause();
                }
                break;
            }
        }
    }

    public void pauseAll() {
        for (Map.Entry<String, DownloadTask> entry : taskMap.entrySet()) {
            DownloadTask task = entry.getValue();
            if (task == null) {
                OkLog.e("can't find task with tag = " + entry.getKey());
                continue;
            }
            if (task.progress.status != FileStatus.LOADING.ordinal()) {
                task.pause();
            }
        }
        for (Map.Entry<String, DownloadTask> entry : taskMap.entrySet()) {
            DownloadTask task = entry.getValue();
            if (task == null) {
                OkLog.e("can't find task with tag = " + entry.getKey());
                continue;
            }
            if (task.progress.status == FileStatus.LOADING.ordinal()) {
                task.pause();
            }
        }
    }

    public void removeAll() {
        removeAll(false);
    }

    public void removeAll(boolean isDeleteFile) {
        Map<String, DownloadTask> map = new HashMap<>(taskMap);
        for (Map.Entry<String, DownloadTask> entry : map.entrySet()) {
            DownloadTask task = entry.getValue();
            if (task == null) {
                OkLog.e("can't find task with tag = " + entry.getKey());
                continue;
            }
            if (task.progress.status != FileStatus.LOADING.ordinal()) {
                task.remove(isDeleteFile);
            }
        }
        for (Map.Entry<String, DownloadTask> entry : map.entrySet()) {
            DownloadTask task = entry.getValue();
            if (task == null) {
                OkLog.e("can't find task with tag = " + entry.getKey());
                continue;
            }
            if (task.progress.status == FileStatus.LOADING.ordinal()) {
                task.remove(isDeleteFile);
            }
        }
    }

    public String getFolder() {
        return folder;
    }

    public OkDownload setFolder(String folder) {
        this.folder = folder;
        return this;
    }

    public DownloadThreadPool getThreadPool() {
        return threadPool;
    }

    public Map<String, DownloadTask> getTaskMap() {
        return taskMap;
    }

    public DownloadTask getTask(String tag) {
        return taskMap.get(tag);
    }

    public boolean hasTask(String tag) {
        return taskMap.containsKey(tag);
    }

    public DownloadTask removeTask(String tag) {
        return taskMap.remove(tag);
    }

    public void addOnAllTaskEndListener(XExecutor.OnAllTaskEndListener listener) {
        threadPool.getExecutor().addOnAllTaskEndListener(listener);
    }

    public void removeOnAllTaskEndListener(XExecutor.OnAllTaskEndListener listener) {
        threadPool.getExecutor().removeOnAllTaskEndListener(listener);
    }
}
