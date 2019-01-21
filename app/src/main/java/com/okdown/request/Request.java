package com.okdown.request;

import android.text.TextUtils;

import com.okdown.OkGo;
import com.okdown.request.model.HttpHeaders;
import com.okdown.request.model.HttpParams;
import com.okdown.utils.Callback;

import java.io.IOException;
import java.io.Serializable;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings("unchecked")
public abstract class Request<T, R extends Request> implements Serializable {
    private static final long serialVersionUID = -7174118653689916252L;
    protected String url;
    protected String baseUrl;
    protected transient OkHttpClient client;
    protected transient Object tag;
    protected int retryCount;
    protected HttpParams params = new HttpParams();     //添加的param
    protected HttpHeaders headers = new HttpHeaders();  //添加的header
    protected transient okhttp3.Request mRequest;
    protected transient Callback callback;
    protected transient ProgressRequestBody.UploadInterceptor uploadInterceptor;

    public Request(String url) {
        this.url = url;
        baseUrl = url;
        OkGo go = OkGo.getInstance();
        //默认添加 Accept-Language
        String acceptLanguage = HttpHeaders.getAcceptLanguage();
        if (!TextUtils.isEmpty(acceptLanguage))
            headers(HttpHeaders.HEAD_KEY_ACCEPT_LANGUAGE, acceptLanguage);
        //默认添加 User-Agent
        String userAgent = HttpHeaders.getUserAgent();
        if (!TextUtils.isEmpty(userAgent)) headers(HttpHeaders.HEAD_KEY_USER_AGENT, userAgent);
        //添加公共请求参数
        if (go.getCommonParams() != null) params(go.getCommonParams());
        if (go.getCommonHeaders() != null) headers(go.getCommonHeaders());
        //添加缓存模式
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

    public okhttp3.Request getRequest() {
        return mRequest;
    }

    protected abstract RequestBody generateRequestBody();

    public abstract okhttp3.Request generateRequest(RequestBody requestBody);

    public okhttp3.Call getRawCall() {
        //构建请求体，返回call对象
        RequestBody requestBody = generateRequestBody();
        if (requestBody != null) {
            ProgressRequestBody<T> progressRequestBody = new ProgressRequestBody<>(requestBody, callback);
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
