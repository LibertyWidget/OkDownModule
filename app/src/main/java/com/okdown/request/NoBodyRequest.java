package com.okdown.request;

import com.okdown.request.model.HttpUtils;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

public abstract class NoBodyRequest<T, R extends NoBodyRequest> extends com.okdown.request.Request<T, R> {
    private static final long serialVersionUID = 1200621102761691196L;

    public NoBodyRequest(String url) {
        super(url);
    }

    @Override
    public RequestBody generateRequestBody() {
        return null;
    }

    protected Request.Builder generateRequestBuilder(RequestBody requestBody) {
        url = HttpUtils.createUrlFromParams(baseUrl, params.urlParamsMap);
        Request.Builder requestBuilder = new Request.Builder();
        return HttpUtils.appendHeaders(requestBuilder, headers);
    }
}
