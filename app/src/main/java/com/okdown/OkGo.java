package com.okdown;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.okdown.request.GetRequest;
import com.okdown.request.model.HttpHeaders;
import com.okdown.request.model.HttpParams;
import com.okdown.request.model.HttpUtils;
import com.okdown.request.model.HttpsUtils;

import java.util.concurrent.TimeUnit;

import com.squareup.okhttp.OkHttpClient;

public class OkGo {
    private long DEFAULT_MILLISECONDS = 60000;      //默认的超时时间
    public static long REFRESH_TIME = 300;                      //回调刷新时间（单位ms）

    private Application context;            //全局上下文
    private Handler mDelivery;              //用于在主线程执行的调度器
    private OkHttpClient okHttpClient;      //ok请求的客户端
    private HttpParams mCommonParams;       //全局公共请求参数
    private HttpHeaders mCommonHeaders;     //全局公共请求头
    private int mRetryCount;                //全局超时重试次数

    private OkGo() {
        mDelivery = new Handler(Looper.getMainLooper());
        mRetryCount = 3;
        okHttpClient = new OkHttpClient();
        okHttpClient.setReadTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        okHttpClient.setWriteTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        okHttpClient.setConnectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        okHttpClient.setSslSocketFactory(sslParams.sSLSocketFactory);
        okHttpClient.setHostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
    }

    public static OkGo getInstance() {
        return OkGoHolder.holder;
    }

    private static class OkGoHolder {
        private static OkGo holder = new OkGo();
    }

    public static <T> GetRequest<T> get(String url) {
        return new GetRequest<>(url);
    }

    public OkGo init(Application app) {
        context = app;
        initOkGo();
        OkDownload.$().init();
        return this;
    }

    private void initOkGo() {
        okHttpClient = new OkHttpClient();
        okHttpClient.setReadTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        okHttpClient.setWriteTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        okHttpClient.setConnectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        okHttpClient.setSslSocketFactory(sslParams.sSLSocketFactory);
        okHttpClient.setHostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
        setRetryCount(3);   //全局统一超时重连次数，默认为三次，那么最差的情况会请求4次(一次原始请求，三次重连请求)，不需要可以设置为0
    }

    public Context getContext() {
        HttpUtils.checkNotNull(context, "please call OkGo.$().init() first in application!");
        return context;
    }

    public Handler getDelivery() {
        return mDelivery;
    }

    public OkHttpClient getOkHttpClient() {
        HttpUtils.checkNotNull(okHttpClient, "please call OkGo.$().setOkHttpClient() first in application!");
        return okHttpClient;
    }

    public OkGo setOkHttpClient(OkHttpClient okHttpClient) {
        HttpUtils.checkNotNull(okHttpClient, "okHttpClient == null");
        this.okHttpClient = okHttpClient;
        return this;
    }

    public OkGo setRetryCount(int retryCount) {
        if (retryCount < 0) throw new IllegalArgumentException("retryCount must > 0");
        mRetryCount = retryCount;
        return this;
    }

    public int getRetryCount() {
        return mRetryCount;
    }

    public HttpParams getCommonParams() {
        return mCommonParams;
    }

    public OkGo addCommonParams(HttpParams commonParams) {
        if (mCommonParams == null) mCommonParams = new HttpParams();
        mCommonParams.put(commonParams);
        return this;
    }

    public HttpHeaders getCommonHeaders() {
        return mCommonHeaders;
    }

    public OkGo addCommonHeaders(HttpHeaders commonHeaders) {
        if (mCommonHeaders == null) mCommonHeaders = new HttpHeaders();
        mCommonHeaders.put(commonHeaders);
        return this;
    }
}
