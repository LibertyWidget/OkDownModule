package com.okdown.task;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class XExecutor extends ThreadPoolExecutor {

    private Handler innerHandler = new Handler(Looper.getMainLooper());

    public XExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected void afterExecute(final Runnable r, Throwable t) {
        super.afterExecute(r, t);
        //当前正在运行的数量为1 表示当前正在停止的任务，同时队列中没有任务，表示所有任务下载完毕
        if (getActiveCount() == 1 && getQueue().size() == 0) {
            if (allTaskEndListenerList != null && allTaskEndListenerList.size() > 0) {
                for (final OnAllTaskEndListener listener : allTaskEndListenerList) {
                    innerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onAllTaskEnd();
                        }
                    });
                }
            }
        }
    }

    private List<OnAllTaskEndListener> allTaskEndListenerList;

    public void addOnAllTaskEndListener(OnAllTaskEndListener allTaskEndListener) {
        if (allTaskEndListenerList == null) allTaskEndListenerList = new ArrayList<>();
        allTaskEndListenerList.add(allTaskEndListener);
    }

    public void removeOnAllTaskEndListener(OnAllTaskEndListener allTaskEndListener) {
        allTaskEndListenerList.remove(allTaskEndListener);
    }

    public interface OnAllTaskEndListener {
        void onAllTaskEnd();
    }
}
