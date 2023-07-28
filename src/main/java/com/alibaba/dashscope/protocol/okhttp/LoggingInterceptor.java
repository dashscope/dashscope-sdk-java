// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol.okhttp;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
class LoggingInterceptor implements Interceptor {
  @Override
  public Response intercept(Interceptor.Chain chain) throws IOException {
    Request request = chain.request();

    long t1 = System.nanoTime();
    log.info(
        String.format(
            "Sending request %s on %s%n%s", request.url(), chain.connection(), request.headers()));

    Response response = chain.proceed(request);

    long t2 = System.nanoTime();
    log.info(
        String.format(
            "Received response for %s in %.1fms%n%s",
            response.request().url(), (t2 - t1) / 1e6d, response.headers()));

    return response;
  }
}
