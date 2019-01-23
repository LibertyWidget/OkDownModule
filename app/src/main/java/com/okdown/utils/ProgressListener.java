package com.okdown.utils;

import com.okdown.request.model.Progress;

public interface ProgressListener {

    void onStart(Progress progress);

    void onProgress(Progress progress);

    void onError(Progress progress);

    void onComplete(Progress progress);

    void onRemove(Progress progress);
}
