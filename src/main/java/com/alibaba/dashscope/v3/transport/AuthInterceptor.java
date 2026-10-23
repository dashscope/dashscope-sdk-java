// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.transport;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Attaches Authorization and X-DashScope-WorkSpace from the per-request auth tag. Headers already
 * set on the request (RequestOptions custom headers) win — they are documented as "applied last,
 * may override SDK-set headers".
 */
public final class AuthInterceptor implements Interceptor {
  public static final String HEADER_WORKSPACE = "X-DashScope-WorkSpace";

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    RequestTags.Auth auth = request.tag(RequestTags.Auth.class);
    if (auth == null) {
      return chain.proceed(request);
    }
    Request.Builder tagged = request.newBuilder();
    if (auth.apiKey != null && !auth.apiKey.isEmpty() && request.header("Authorization") == null) {
      tagged.header("Authorization", "Bearer " + auth.apiKey);
    }
    if (auth.workspace != null
        && !auth.workspace.isEmpty()
        && request.header(HEADER_WORKSPACE) == null) {
      tagged.header(HEADER_WORKSPACE, auth.workspace);
    }
    return chain.proceed(tagged.build());
  }
}
