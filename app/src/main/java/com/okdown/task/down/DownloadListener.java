package com.okdown.task.down;

import com.okdown.request.model.Progress;

public abstract class DownloadListener {

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
