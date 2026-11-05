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
 * generations), and x-dashscope-sdk-client in the {@code client/version/module} form. The module
 * segment names the functional area issuing the request (aigc / embeddings / audio / ...) and
 * carries a trailing {@link #MODULE_MARKER} so the 3.0 surface stays separable from 2.x: one
 * artifact ships both the legacy_2x transport and this 3.0 transport and both read the same {@link
 * SdkInfo#VERSION}, so the version segment alone cannot tell them apart. With the marker 3.0 emits
 * {@code aigc3} while 2.x stays {@code aigc}, mirroring the Python SDK's V2_MODULE_MARKER.
 */
public final class UserAgentInterceptor implements Interceptor {
  public static final String HEADER_SDK_CLIENT = "x-dashscope-sdk-client";
  public static final String HEADER_SDK_SESSION_ID = "x-dashscope-sdk-session-id";

  /** Fallback module segment when a request carries no functional-area tag. */
  private static final String DEFAULT_MODULE = "dashscope";

  /** Trailing generation marker appended to every 3.0 module tag (aigc -> aigc3). */
  private static final String MODULE_MARKER = "3";

  private static final String USER_AGENT =
      "dashscope-sdk-java/"
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
    String moduleValue = (module == null || module.value.isEmpty()) ? DEFAULT_MODULE : module.value;
    String sdkClient = "java-sdk/" + SdkInfo.VERSION + "/" + moduleValue + MODULE_MARKER;
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
    return chain.proceed(tagged.build());
  }
}
