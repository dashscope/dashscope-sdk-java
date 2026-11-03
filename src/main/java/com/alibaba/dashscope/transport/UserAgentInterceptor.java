// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport;

import com.alibaba.dashscope.shared.SdkInfo;
import com.alibaba.dashscope.shared.SdkSession;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Attaches SDK telemetry headers (design §6): User-Agent (version), x-dashscope-sdk-session-id
 * (shared with 2.x via the neutral layer, so one process reports one session id across
 * generations), x-dashscope-sdk-client with the per-resource module tag, and
 * x-dashscope-sdk-generation marking requests issued by the 3.0 transport. The generation header is
 * 3.0-only by design: 2.x requests (legacy_2x transport, AgentStudio's 2.x protocol) carry no such
 * header, so server logs can separate the two code paths during migration.
 */
public final class UserAgentInterceptor implements Interceptor {
  public static final String HEADER_SDK_CLIENT = "x-dashscope-sdk-client";
  public static final String HEADER_SDK_SESSION_ID = "x-dashscope-sdk-session-id";
  public static final String HEADER_SDK_GENERATION = "x-dashscope-sdk-generation";

  private static final String USER_AGENT =
      "dashscope/"
          + SdkInfo.VERSION
          + "; java/"
          + System.getProperty("java.version")
          + "; platform/"
          + System.getProperty("os.name")
          + "; processor/"
          + System.getProperty("os.arch");

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    RequestTags.Module module = request.tag(RequestTags.Module.class);
    String sdkClient = "java-sdk/" + SdkInfo.VERSION;
    if (module != null && !module.value.isEmpty()) {
      sdkClient += "/" + module.value;
    }
    String userAgent = USER_AGENT;
    RequestTags.UserAgentSuffix suffix = request.tag(RequestTags.UserAgentSuffix.class);
    if (suffix != null && !suffix.value.isEmpty()) {
      userAgent += "; " + suffix.value;
    }
    Request.Builder tagged = request.newBuilder();
    // RequestOptions custom headers are documented as overriding SDK-set headers: only fill in
    // the telemetry headers the caller did not provide.
    if (request.header("User-Agent") == null) {
      tagged.header("User-Agent", userAgent);
    }
    if (request.header(HEADER_SDK_CLIENT) == null) {
      tagged.header(HEADER_SDK_CLIENT, sdkClient);
    }
    if (request.header(HEADER_SDK_SESSION_ID) == null) {
      tagged.header(HEADER_SDK_SESSION_ID, SdkSession.ID);
    }
    if (request.header(HEADER_SDK_GENERATION) == null) {
      tagged.header(HEADER_SDK_GENERATION, "3");
    }
    return chain.proceed(tagged.build());
  }
}
