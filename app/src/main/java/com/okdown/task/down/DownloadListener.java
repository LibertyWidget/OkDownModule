package com.okdown.task.down;

import com.okdown.utils.ProgressListener;

public abstract class DownloadListener implements ProgressListener {

    public final Object tag;

    public DownloadListener(Object tag) {
        this.tag = tag;
    }
}
