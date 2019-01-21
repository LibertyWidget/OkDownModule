package com.okdown.utils;

import com.okdown.request.model.Progress;

public interface Callback {
    void uploadProgress(Progress progress);
}
