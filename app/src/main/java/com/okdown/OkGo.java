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

import okhttp3.OkHttpClient;

public class OkGo {
    public static final long DEFAULT_MILLISECONDS = 60000;      //默认的超时时间
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

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.readTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.writeTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.connectTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);

        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        builder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
        builder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
        okHttpClient = builder.build();
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
        initOkGo(app);
        OkDownload.$().init();
        return this;
    }

    private void initOkGo(Application app) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        //超时时间设置，默认60秒
        builder.readTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);      //全局的读取超时时间
        builder.writeTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);     //全局的写入超时时间
        builder.connectTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);   //全局的连接超时时间

        //https相关设置，以下几种方案根据需要自己设置
        //方法一：信任所有证书,不安全有风险
        HttpsUtils.SSLParams sslParams1 = HttpsUtils.getSslSocketFactory();
        //方法三：使用预埋证书，校验服务端证书（自签名证书）
        //HttpsUtils.SSLParams sslParams3 = HttpsUtils.getSslSocketFactory(getAssets().open("srca.cer"));
        //方法四：使用bks证书和密码管理客户端证书（双向认证），使用预埋证书，校验服务端证书（自签名证书）
        //HttpsUtils.SSLParams sslParams4 = HttpsUtils.getSslSocketFactory(getAssets().open("xxx.bks"), "123456", getAssets().open("yyy.cer"));
        builder.sslSocketFactory(sslParams1.sSLSocketFactory, sslParams1.trustManager);

        // 其他统一的配置
        // 详细说明看GitHub文档：https://github.com/jeasonlzy/
        setOkHttpClient(builder.build());             //建议设置OkHttpClient，不设置会使用默认的
        setRetryCount(3);                               //全局统一超时重连次数，默认为三次，那么最差的情况会请求4次(一次原始请求，三次重连请求)，不需要可以设置为0
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
