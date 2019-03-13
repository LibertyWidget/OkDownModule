package com.okdown.request;

import android.text.TextUtils;

import com.okdown.OkGo;
import com.okdown.request.model.HttpHeaders;
import com.okdown.request.model.HttpParams;

import java.io.IOException;
import java.io.Serializable;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

@SuppressWarnings("unchecked")
public abstract class Request<T, R extends Request> implements Serializable {
    private static final long serialVersionUID = -7174118653689916252L;
    protected String url;
    protected String baseUrl;
    protected transient OkHttpClient client;
    protected transient Object tag;
    protected int retryCount;
    protected HttpParams params = new HttpParams();
    protected HttpHeaders headers = new HttpHeaders();
    protected transient com.squareup.okhttp.Request mRequest;
    protected transient ProgressRequestBody.UploadInterceptor uploadInterceptor;

    public Request(String url) {
        this.url = url;
        baseUrl = url;
        OkGo go = OkGo.getInstance();
        String acceptLanguage = HttpHeaders.getAcceptLanguage();
        if (!TextUtils.isEmpty(acceptLanguage))
            headers(HttpHeaders.HEAD_KEY_ACCEPT_LANGUAGE, acceptLanguage);
        String userAgent = HttpHeaders.getUserAgent();
        if (!TextUtils.isEmpty(userAgent)) headers(HttpHeaders.HEAD_KEY_USER_AGENT, userAgent);
        if (go.getCommonParams() != null) params(go.getCommonParams());
        if (go.getCommonHeaders() != null) headers(go.getCommonHeaders());
        retryCount = go.getRetryCount();
    }


    public R headers(HttpHeaders headers) {
        this.headers.put(headers);
        return (R) this;
    }

    public R headers(String key, String value) {
        headers.put(key, value);
        return (R) this;
    }


    public R params(HttpParams params) {
        this.params.put(params);
        return (R) this;
    }

    public String getUrl() {
        return url;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public com.squareup.okhttp.Request getRequest() {
        return mRequest;
    }

    protected abstract RequestBody generateRequestBody();

    public abstract com.squareup.okhttp.Request generateRequest(RequestBody requestBody);

    public Call getRawCall() {
        RequestBody requestBody = generateRequestBody();
        if (requestBody != null) {
            ProgressRequestBody<T> progressRequestBody = new ProgressRequestBody<>(requestBody);
            progressRequestBody.setInterceptor(uploadInterceptor);
            mRequest = generateRequest(progressRequestBody);
        } else {
            mRequest = generateRequest(null);
        }
        if (client == null) client = OkGo.getInstance().getOkHttpClient();
        return client.newCall(mRequest);
    }

    public Response execute() throws IOException {
        return getRawCall().execute();
    }
}
