
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
                    public void onComplete(Progress progress) {
                        Log.e("tag", "onComplete " + progress.toString());
                    }

                    @Override
                    public void onRemove(Progress progress) {
                        Log.e("tag", "onRemove " + progress.toString());
                    }
                }).start();
 */
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
        //校验数据的有效性，防止下载过程中退出，第二次进入的时候，由于状态没有更新导致的状态错误
        List<Progress> taskList = DownloadManager.$().getDownloading();
        for (Progress info : taskList) {
            if (info.status == FileStatus.WAITING.ordinal() || info.status == FileStatus.LOADING.ordinal() || info.status == FileStatus.PAUSE.ordinal()) {
                info.status = FileStatus.NONE.ordinal();
            }
            //恢复到内存数据
            DownloadTask task = new DownloadTask(info);
            taskMap.put(info.url, task);
        }
        DownloadManager.$().replace(taskList);
    }

    public DownloadTask request(String name, String url) {
        //创建一个性文件夹
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
        //先停止未开始的任务
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
        //再停止进行中的任务
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
