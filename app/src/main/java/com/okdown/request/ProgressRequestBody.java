package com.okdown.request;

import com.okdown.request.model.Progress;
import com.okdown.utils.OkLog;

import java.io.IOException;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

public class ProgressRequestBody<T> extends RequestBody {

    private RequestBody requestBody;
    private UploadInterceptor interceptor;

    ProgressRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() {
        try {
            return requestBody.contentLength();
        } catch (IOException e) {
            OkLog.printStackTrace(e);
            return -1;
        }
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);
        requestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    private final class CountingSink extends ForwardingSink {

        private Progress progress;

        CountingSink(Sink delegate) {
            super(delegate);
            progress = new Progress();
            progress.totalSize = contentLength();
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);

            Progress.changeProgress(progress, byteCount, new Progress.Action() {
                @Override
                public void call(Progress progress) {
                    if (interceptor != null) {
                        interceptor.uploadProgress(progress);
                    }
                }
            });
        }
    }


    public void setInterceptor(UploadInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public interface UploadInterceptor {
        void uploadProgress(Progress progress);
    }
}
