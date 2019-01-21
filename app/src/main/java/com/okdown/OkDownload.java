
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
              String url = "http://downapp.baidu.com/baidusearch/AndroidPhone/11.3.0.13/1/757p/20190108123353/baidusearch_AndroidPhone_11-3-0-13_757p.apk?responseContentDisposition=attachment%3Bfilename%3D%22baidusearch_AndroidPhone_757p.apk%22&responseContentType=application%2Fvnd.android.package-archive&request_id=1547787195_2004241216&type=static";
                OkDownload.request(url).save().register(new DownloadListener(url) {
                    @Override
                    public void onStart(Progress progress) {
                        Log.e("tag", "onStart " + progress.toString());
                    }

                    @Override
                    public void onProgress(Progress progress) {
                        Log.e("tag", "onProgress " + progress.toString());
                    }

                    @Override
                    public void onError(Progress progress) {
                        Log.e("tag", "onError " + progress.toString());
                    }

                    @Override
                    public void onFinish(Progress progress) {
                        Log.e("tag", "onFinish " + progress.toString());
                    }

                    @Override
                    public void onRemove(Progress progress) {
                        Log.e("tag", "onRemove " + progress.toString());
                    }
                }).start();
 */
public class OkDownload {

    private String folder;                                      //下载的默认文件夹
    private DownloadThreadPool threadPool;                      //下载的线程池
    private ConcurrentHashMap<String, DownloadTask> taskMap;    //所有任务

    public static OkDownload $() {
        return OkDownloadHolder.instance;
    }

    private static class OkDownloadHolder {
        private static final OkDownload instance = new OkDownload();
    }

    private OkDownload() {
        folder = Environment.getExternalStorageDirectory() + File.separator + "download" + File.separator;
        IOUtils.createFolder(new File(folder));
        threadPool = new DownloadThreadPool();
        taskMap = new ConcurrentHashMap<>();

        //校验数据的有效性，防止下载过程中退出，第二次进入的时候，由于状态没有更新导致的状态错误
        List<Progress> taskList = DownloadManager.$().getDownloading();
        for (Progress info : taskList) {
            if (info.status == FileStatus.WAITING.ordinal() || info.status == FileStatus.LOADING.ordinal() || info.status == FileStatus.PAUSE.ordinal()) {
                info.status = FileStatus.NONE.ordinal();
            }
        }
        DownloadManager.$().replace(taskList);
    }

    public static DownloadTask request(String url) {
        Request<File, ? extends Request> request = OkGo.get(url);
        Map<String, DownloadTask> taskMap = OkDownload.$().getTaskMap();
        DownloadTask task = taskMap.get(url);
        if (task == null) {
            task = new DownloadTask(request);
            taskMap.put(url, task);
        }
        return task;
    }

    /**
     * 从数据库中恢复任务
     */
    public static DownloadTask restore(Progress progress) {
        Map<String, DownloadTask> taskMap = OkDownload.$().getTaskMap();
        DownloadTask task = taskMap.get(progress.url);
        if (task == null) {
            task = new DownloadTask(progress);
            taskMap.put(progress.url, task);
        }
        return task;
    }

    /**
     * 从数据库中恢复任务
     */
    public static List<DownloadTask> restore(List<Progress> progressList) {
        Map<String, DownloadTask> taskMap = OkDownload.$().getTaskMap();
        List<DownloadTask> tasks = new ArrayList<>();
        for (Progress progress : progressList) {
            DownloadTask task = taskMap.get(progress.url);
            if (task == null) {
                task = new DownloadTask(progress);
                taskMap.put(progress.url, task);
            }
            tasks.add(task);
        }
        return tasks;
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

    public void pauseAll() {
        //先停止未开始的任务
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
        //再停止进行中的任务
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
        //先删除未开始的任务
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
        //再删除进行中的任务
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
