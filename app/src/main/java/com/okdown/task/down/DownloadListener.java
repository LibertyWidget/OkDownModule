package com.okdown.task.down;

import com.okdown.request.model.Progress;
import com.okdown.utils.ProgressListener;

public abstract class DownloadListener implements ProgressListener {

    public final Object tag;

    public DownloadListener(Object tag) {
        this.tag = tag;
    }

    public void onStart(Progress progress) {
    }

    public void onProgress(Progress progress) {
    }

    public void onError(Progress progress) {
    }

    public void onComplete(Progress progress) {
    }

    public void onRemove(Progress progress) {
    }

}
